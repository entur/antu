# Cross project grants to anzu.
#
# anzu lives in its own project and needs two things from antu: the application reads report blobs out of
# antu's bucket at runtime, and anzu's GitHub Actions service account creates a subscription on antu's status
# topic when it applies the anzu-deployment-config terraform.

# Runtime: the anzu application reads antu's validation reports.
resource "google_storage_bucket_iam_member" "anzu_report_reader" {
  count  = var.anzu_service_account == "" ? 0 : 1
  bucket = google_storage_bucket.storage_bucket.name
  role   = "roles/storage.objectViewer"
  member = "serviceAccount:${var.anzu_service_account}"
}

# Deploy time: attaching a subscription to a topic in another project needs
# pubsub.topics.attachSubscription on the topic, which roles/pubsub.subscriber carries. Without this the
# anzu-deployment-config apply that creates the subscription cannot run.
resource "google_pubsub_topic_iam_member" "anzu_deploy_attach_subscription" {
  count   = var.anzu_github_service_account == "" ? 0 : 1
  project = var.gcp_resources_project
  topic   = google_pubsub_topic.AntuNetexValidationStatusQueue.name
  role    = "roles/pubsub.subscriber"
  member  = "serviceAccount:${var.anzu_github_service_account}"
}

# Pub/Sub grants to antu on its own topics and subscriptions.
#
# Until now these worked only through the team-wide folder rule in entur/tf-gcp-iam
# (application_rules/ror-antu-<env>.yaml), which grants publisher, subscriber and objectAdmin on every team-ror
# resource in the environment, and antu has no project-level Pub/Sub role. These grants reproduce exactly what
# antu uses here, so that the rule can be removed by Team Plattform without breaking it.
#
# antu's own buckets need nothing: the project-level roles/storage.objectAdmin in ent-antu-<env> covers them.
# The stop place export it reads from marduk-<env> is public.

locals {
  antu_service_account = "serviceAccount:application@${var.gcp_resources_project}.iam.gserviceaccount.com"
}

# antu publishes the next pipeline job and the validation status. Nothing else publishes to these.
resource "google_pubsub_topic_iam_member" "antu_publisher" {
  for_each = toset([
    google_pubsub_topic.AntuJobQueue.name,
    google_pubsub_topic.AntuNetexValidationStatusQueue.name,
  ])
  project = var.gcp_resources_project
  topic   = each.value
  role    = "roles/pubsub.publisher"
  member  = local.antu_service_account
}

# antu consumes validation requests from marduk and its own pipeline jobs. No admin call is made at startup:
# entur.pubsub.subscriber.autocreate is false in every deployed environment.
resource "google_pubsub_subscription_iam_member" "antu_subscriber" {
  for_each = toset([
    google_pubsub_subscription.AntuNetexValidationQueue.name,
    google_pubsub_subscription.AntuJobQueue.name,
  ])
  project      = var.gcp_resources_project
  subscription = each.value
  role         = "roles/pubsub.subscriber"
  member       = local.antu_service_account
}

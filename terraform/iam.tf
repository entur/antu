# Cross project grants to anzu, and grants on antu's own resources (at the end of this file).
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

# Pub/Sub grants on antu's own resources, for antu and for marduk.
#
# Until now these worked only through the team-wide folder rule in entur/tf-gcp-iam
# (application_rules/ror-antu-<env>.yaml and ror-marduk-<env>.yaml), which grants publisher, subscriber and
# objectAdmin on every team-ror resource in the environment. The topics, subscriptions and exchange bucket
# below had no binding of their own. These grants reproduce exactly what the two applications use here, so
# that the rules can be removed by Team Plattform without breaking either of them.
#
# antu's own buckets need nothing: the project-level roles/storage.objectAdmin in ent-antu-<env> covers them.
# The stop place export it reads from marduk-<env> is public.

locals {
  antu_service_account   = "serviceAccount:application@${var.gcp_resources_project}.iam.gserviceaccount.com"
  marduk_service_account = "serviceAccount:application@ent-marduk-${var.env}.iam.gserviceaccount.com"
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

# marduk requests a validation by publishing to AntuNetexValidationQueue...
resource "google_pubsub_topic_iam_member" "marduk_validation_request_publisher" {
  project = var.gcp_resources_project
  topic   = google_pubsub_topic.AntuNetexValidationQueue.name
  role    = "roles/pubsub.publisher"
  member  = local.marduk_service_account
}

# ...after uploading the dataset to the exchange bucket, which antu reads it from.
resource "google_storage_bucket_iam_member" "marduk_exchange_writer" {
  bucket = google_storage_bucket.storage_bucket_exchange.name
  role   = "roles/storage.objectUser"
  member = local.marduk_service_account
}

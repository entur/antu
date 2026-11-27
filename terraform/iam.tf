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

# Cross project grants to marduk.
#
# marduk sends its validation requests to AntuNetexValidationQueue, and copies the NeTEx datasets to
# validate into the exchange bucket. These grants replace marduk's team-wide folder rule
# (tf-gcp-iam application_rules/ror-marduk-<env>.yaml), so marduk keeps this access when that rule is removed.
resource "google_pubsub_topic_iam_member" "marduk_validation_request_publisher" {
  project = var.gcp_resources_project
  topic   = google_pubsub_topic.AntuNetexValidationQueue.name
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${var.marduk_service_account}"
}

# A copy may replace an existing object, which needs storage.objects.delete, so roles/storage.objectCreator
# is not enough.
resource "google_storage_bucket_iam_member" "marduk_exchange_object_user" {
  bucket = google_storage_bucket.storage_bucket_exchange.name
  role   = "roles/storage.objectUser"
  member = "serviceAccount:${var.marduk_service_account}"
}

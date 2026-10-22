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

# Contains main description of bulk of terraform
terraform {
  required_version = ">= 0.13.2"
}

provider "google" {
  version = "~> 3.74.0"
}
provider "kubernetes" {
  load_config_file = var.load_config_file
  version = "~> 1.13.4"
}

# create service account
resource "google_service_account" "antu_service_account" {
  account_id = "${var.labels.team}-${var.labels.app}-sa"
  display_name = "${var.labels.team}-${var.labels.app} service account"
  project = var.gcp_resources_project
}

# add service account as member to marduk bucket
resource "google_storage_bucket_iam_member" "storage_marduk_bucket_iam_member" {
  bucket = var.bucket_marduk_instance_name
  role = var.service_account_bucket_role
  member = "serviceAccount:${google_service_account.antu_service_account.email}"
}

# add service account as member to antu bucket
resource "google_storage_bucket_iam_member" "storage_antu_bucket_iam_member" {
  bucket = var.bucket_antu_instance_name
  role = var.service_account_bucket_role
  member = "serviceAccount:${google_service_account.antu_service_account.email}"
}


# add service account as member to pubsub service in the resources project
resource "google_project_iam_member" "pubsub_project_iam_member_subscriber" {
  project = var.gcp_pubsub_project
  role = "roles/pubsub.subscriber"
  member = "serviceAccount:${google_service_account.antu_service_account.email}"
}

resource "google_project_iam_member" "pubsub_project_iam_member_publisher" {
  project = var.gcp_pubsub_project
  role = "roles/pubsub.publisher"
  member = "serviceAccount:${google_service_account.antu_service_account.email}"
}

# Create pubsub topics and subscriptions
resource "google_pubsub_topic" "AntuJobQueue" {
  name = "AntuJobQueue"
  project = var.gcp_pubsub_project
  labels = var.labels
}

resource "google_pubsub_subscription" "AntuJobQueue" {
  name = "AntuJobQueue"
  topic = google_pubsub_topic.AntuJobQueue.name
  project = var.gcp_pubsub_project
  labels = var.labels
  ack_deadline_seconds = 600
  retry_policy {
    minimum_backoff = "10s"
  }
}

resource "google_pubsub_topic" "AntuReportAggregationQueue" {
  name = "AntuReportAggregationQueue"
  project = var.gcp_pubsub_project
  labels = var.labels
}

resource "google_pubsub_subscription" "AntuReportAggregationQueue" {
  name = "AntuReportAggregationQueue"
  topic = google_pubsub_topic.AntuReportAggregationQueue.name
  project = var.gcp_pubsub_project
  labels = var.labels
  retry_policy {
    minimum_backoff = "10s"
  }
}




# create key for service account
resource "google_service_account_key" "antu_service_account_key" {
  service_account_id = google_service_account.antu_service_account.name
}

# Add SA key to to k8s
resource "kubernetes_secret" "antu_service_account_credentials" {
  metadata {
    name = "${var.labels.team}-${var.labels.app}-sa-key"
    namespace = var.kube_namespace
  }
  data = {
    "credentials.json" = base64decode(google_service_account_key.antu_service_account_key.private_key)
  }
}

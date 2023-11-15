# Contains main description of bulk of terraform
terraform {
  required_version = ">= 0.13.2"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 4.84.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.13.1"
    }
  }
}


# Create bucket
resource "google_storage_bucket" "storage_bucket" {
  name               = "${var.bucket_instance_prefix}-${var.bucket_instance_suffix}"
  location           = var.bucket_location
  project            = var.gcp_resources_project
  storage_class      = var.bucket_storage_class
  labels             = merge(var.labels, {offsite_enabled = "false"})
  uniform_bucket_level_access = true


  lifecycle_rule {

    condition {
      age = var.bucket_retention_period
      with_state = "ANY"
    }
    action {
      type = "Delete"
    }
  }
}

resource "google_storage_bucket" "storage_bucket_exchange" {
  name               = "ror-antu-exchange-gcp2-${var.bucket_instance_suffix}"
  location           = var.bucket_location
  project            = var.gcp_resources_project
  storage_class      = var.bucket_storage_class
  labels             = merge(var.labels, {offsite_enabled = "false"})
  uniform_bucket_level_access = true


  lifecycle_rule {

    condition {
      age = var.bucket_retention_period
      with_state = "ANY"
    }
    action {
      type = "Delete"
    }
  }
}

resource "google_pubsub_topic" "AntuNetexValidationQueue" {
  name = "AntuNetexValidationQueue"
  project = var.gcp_resources_project
  labels = var.labels
}

resource "google_pubsub_subscription" "AntuNetexValidationQueue" {
  name = "AntuNetexValidationQueue"
  topic = google_pubsub_topic.AntuNetexValidationQueue.name
  project = var.gcp_resources_project
  labels = var.labels
  retry_policy {
    minimum_backoff = "10s"
  }
}

resource "google_pubsub_topic" "AntuNetexValidationStatusQueue" {
  name = "AntuNetexValidationStatusQueue"
  project = var.gcp_resources_project
  labels = var.labels
}

# Create pubsub topics and subscriptions
resource "google_pubsub_topic" "AntuJobQueue" {
  name = "AntuJobQueue"
  project = var.gcp_resources_project
  labels = var.labels
}

resource "google_pubsub_subscription" "AntuJobQueue" {
  name = "AntuJobQueue"
  topic = google_pubsub_topic.AntuJobQueue.name
  project = var.gcp_resources_project
  labels = var.labels
  ack_deadline_seconds = 60
  message_retention_duration = "3600s"
  retry_policy {
    minimum_backoff = "10s"
  }
}

resource "google_pubsub_topic" "AntuReportAggregationQueue" {
  name = "AntuReportAggregationQueue"
  project = var.gcp_resources_project
  labels = var.labels
}

resource "google_pubsub_subscription" "AntuReportAggregationQueue" {
  name = "AntuReportAggregationQueue"
  topic = google_pubsub_topic.AntuReportAggregationQueue.name
  project = var.gcp_resources_project
  labels = var.labels
  message_retention_duration = "3600s"
  retry_policy {
    minimum_backoff = "10s"
  }
}

resource "google_pubsub_topic" "AntuCommonFilesAggregationQueue" {
  name = "AntuCommonFilesAggregationQueue"
  project = var.gcp_resources_project
  labels = var.labels
}

resource "google_pubsub_subscription" "AntuCommonFilesAggregationQueue" {
  name = "AntuCommonFilesAggregationQueue"
  topic = google_pubsub_topic.AntuCommonFilesAggregationQueue.name
  project = var.gcp_resources_project
  labels = var.labels
  message_retention_duration = "3600s"
  retry_policy {
    minimum_backoff = "10s"
  }
}



# Redis server
resource "google_redis_instance" "antu-redis" {
  name = "${var.labels.app}-${var.kube_namespace}"
  project                 = var.gcp_resources_project
  tier                    = "STANDARD_HA"
  memory_size_gb          = 5

  redis_version           = "REDIS_6_X"
  authorized_network      = data.google_compute_network.main_network_project_vpc.id
  connect_mode            = "PRIVATE_SERVICE_ACCESS"
  region                  = var.gcp_region
  location_id             = var.redis_zone
  transit_encryption_mode = "SERVER_AUTHENTICATION"
  auth_enabled = "true"
  labels                  = var.labels
  redis_configs           = {
    maxmemory-gb = "4.8",
    maxmemory-policy = "allkeys-lru"
    activedefrag = "yes"
  }
  timeouts {
    update = "25m"
  }
}

data "google_compute_network" "main_network_project_vpc" {
  name = "vpc-${var.env}-001"
  project = data.google_projects.network_projects.projects[0].project_id
}

data "google_projects" "network_projects" {
  filter = "lifecycleState:ACTIVE labels.app_short:network labels.environment:${var.env}"
}

resource "kubernetes_config_map" "antu-redis-config" {
  metadata {
    name =  "antu-redis-configmap"
    namespace = var.kube_namespace
    labels    = var.labels
  }

  data = {
    "REDIS_HOST" = google_redis_instance.antu-redis.host
    "REDIS_PORT" = google_redis_instance.antu-redis.port
    "redis-server-ca.pem" = google_redis_instance.antu-redis.server_ca_certs.0.cert
  }

}

resource "random_integer" "password_length" {
  min = 16
  max = 16
}
resource "random_password" "truststore-password" {
  length           = random_integer.password_length.result
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}


resource "kubernetes_secret" "ror-antu-secret" {
  metadata {
    name = "${var.labels.team}-${var.labels.app}-secret"
    namespace = var.kube_namespace
  }
  data = {
    "redis-server-trust-store-password" = random_password.truststore-password.result
    "redis-authentication-string" =  google_redis_instance.antu-redis.auth_string
    "partner-auth0-secret" = var.ror-partner-auth0-secret
  }
}


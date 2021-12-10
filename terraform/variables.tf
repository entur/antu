#Enviroment variables
variable "gcp_project" {
  description = "The GCP project hosting the workloads"
}

variable "gcp_region" {
  description = "The GCP region"
  default     = "europe-west1"
}

variable "gcp_storage_project" {
  description = "The GCP project hosting the Google Storage resources"
}

variable "gcp_pubsub_project" {
  description = "The GCP project hosting the PubSub resources"
}

variable "gcp_resources_project" {
  description = "The GCP project hosting the project resources"
}

variable "kube_namespace" {
  description = "The Kubernetes namespace"
}

variable "labels" {
  description = "Labels used in all resources"
  type = map(string)
  default = {
    manager = "terraform"
    team = "ror"
    slack = "talk-ror"
    app = "antu"
  }
}

variable "load_config_file" {
  description = "Do not load kube config file"
  default = false
}

variable "service_account_bucket_role" {
  description = "Role of the Service Account - more about roles https://cloud.google.com/storage/docs/access-control/iam-roles"
  default = "roles/storage.objectViewer"
}

variable "bucket_marduk_instance_name" {
  description = "Marduk bucket name"
}

variable "bucket_antu_instance_name" {
  description = "Antu bucket name"
}

variable "redis_project" {
  description = "The GCP project for redis"
}

variable "redis_zone" {
  description = "The GCP zone for redis"
  default = "europe-west1-d"
}

variable "redis_reserved_ip_range" {
  description = "IP range for Redis, follow addressing scheme"
}

variable "redis_prevent_destroy" {
  description = "Prevents destruction of this redis instance"
  type        = bool
  default     = false
}






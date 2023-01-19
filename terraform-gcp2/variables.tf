#Enviroment variables
variable "env" {
  description = "Kubernetes deployment environment"
}

variable "bucket_location" {
  description = "GCP bucket location"
  default = "europe-west1"
}

variable "bucket_instance_suffix" {
  description = "A suffix for the bucket instance, may be changed if environment is destroyed and then needed again (name collision workaround) - also bucket names must be globally unique"
}

variable "bucket_instance_prefix" {
  description = "A prefix for the bucket instance, may be changed if environment is destroyed and then needed again (name collision workaround) - also bucket names must be globally unique"
  default     = "ror-antu-gcp2"
}

variable "bucket_storage_class" {
  description = "GCP storage class"
  default     = "REGIONAL"
}

variable "bucket_retention_period" {
  description = "Retention period for GCS objects, in days"
  default     = "90"
}

variable "gcp_resources_project" {
  description = "The GCP project hosting the project resources"
}

variable "kube_namespace" {
  description = "The Kubernetes namespace"
  default = "antu"
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


variable "redis_zone" {
  description = "The GCP zone for redis"
  default = "europe-west1-d"
}


variable ror-partner-auth0-secret {
  description = "Auth0 client secret for Entur partner tenant"
}







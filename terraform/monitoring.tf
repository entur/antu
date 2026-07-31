# Antu alerting, prd only: the Slack notification channel exists only in prd.

# Holds a Slack token, so it stays console-managed. If it is renamed there,
# update display_name here.
data "google_monitoring_notification_channel" "antu_slack_alert" {
  count        = var.env == "prd" ? 1 : 0
  display_name = "Antu Slack Alert"
  project      = var.gcp_resources_project
}

# Two signals rather than one memory threshold: nightly batches normally peak at
# 41-52%, so a plain usage alert fires on ordinary nights. Evictions are the
# incident, the nightly peak is capacity planning.
resource "google_monitoring_alert_policy" "redis_evictions" {
  count        = var.env == "prd" ? 1 : 0
  project      = var.gcp_resources_project
  display_name = "Antu Redis - keys evicted"
  # severity needs google provider v5+; add CRITICAL when it is bumped
  combiner = "OR"

  conditions {
    display_name = "Redis evicted_keys increased"
    condition_threshold {
      filter          = "resource.type = \"redis_instance\" AND resource.labels.instance_id = \"${google_redis_instance.antu-redis.id}\" AND metric.type = \"redis.googleapis.com/stats/evicted_keys\" AND metric.labels.role = \"primary\""
      comparison      = "COMPARISON_GT"
      threshold_value = 0
      duration        = "0s"
      trigger {
        count = 1
      }
      aggregations {
        alignment_period   = "300s"
        per_series_aligner = "ALIGN_DELTA"
      }
    }
  }

  documentation {
    content = "Redis evicted keys under memory pressure. The instance uses allkeys-lru, so evictions can drop in-flight validation state (NeTEx id uniqueness sets) and produce false-pass reports with nothing logged. Recovery: find validations overlapping the eviction timestamps in the antu logs and re-trigger them from Marduk; treat reports from that window as untrustworthy. Prevention: raise memory_size_gb and maxmemory-gb on google_redis_instance.antu-redis. Backlog: check evicted_keys when finalising a report, so this fails loudly instead of silently."
  }

  alert_strategy {
    auto_close = "1800s"
  }

  notification_channels = [data.google_monitoring_notification_channel.antu_slack_alert[0].name]
  user_labels           = var.labels
}

resource "google_monitoring_alert_policy" "redis_memory_usage" {
  count        = var.env == "prd" ? 1 : 0
  project      = var.gcp_resources_project
  display_name = "Antu Redis - nightly peak above 85% of maxmemory"
  combiner     = "OR"

  conditions {
    display_name = "Daily max of Redis usage_ratio above 0.85"
    condition_threshold {
      filter          = "resource.type = \"redis_instance\" AND resource.labels.instance_id = \"${google_redis_instance.antu-redis.id}\" AND metric.type = \"redis.googleapis.com/stats/memory/usage_ratio\" AND metric.labels.role = \"primary\""
      comparison      = "COMPARISON_GT"
      threshold_value = 0.85
      duration        = "0s"
      trigger {
        count = 1
      }
      aggregations {
        alignment_period   = "86400s"
        per_series_aligner = "ALIGN_MAX"
      }
    }
  }

  documentation {
    content = "Capacity signal, not an incident: the nightly batch peaked above 85% of maxmemory (normal is 75-80% around 00:10, back to ~10% by 01:30). Evictions have their own alert, so nothing to do at night. Next business day: if peaks stay above 85%, raise memory_size_gb and maxmemory-gb on google_redis_instance.antu-redis."
  }

  alert_strategy {
    auto_close = "86400s"
  }

  notification_channels = [data.google_monitoring_notification_channel.antu_slack_alert[0].name]
  user_labels           = var.labels
}

resource "google_logging_metric" "system_errors" {
  count   = var.env == "prd" ? 1 : 0
  name    = "antu/system_errors"
  project = var.gcp_resources_project

  # Antu logs to ent-kub-<env>, which routes the entries here. Without
  # bucket_name the metric sees nothing.
  bucket_name = "projects/${var.gcp_resources_project}/locations/${var.gcp_region}/buckets/log-${var.gcp_resources_project}"

  description = "Counts ERROR-level 'System error' lines from the antu container. Retryable failures log at INFO and are retried, so they are excluded."

  filter = <<-EOT
    resource.labels.container_name="antu"
    jsonPayload.message:"System error"
    severity>=ERROR
  EOT

  metric_descriptor {
    metric_kind  = "DELTA"
    value_type   = "INT64"
    unit         = "1"
    display_name = "Antu system errors"
  }
}

# The Monitoring API lags the Logging API, so a policy referencing a brand new
# metric type 404s. Keyed on the metric name so a rename re-arms the wait.
resource "time_sleep" "wait_for_system_errors_metric" {
  count           = var.env == "prd" ? 1 : 0
  create_duration = "60s"
  triggers        = { metric = google_logging_metric.system_errors[0].name }
}

resource "google_monitoring_alert_policy" "system_errors" {
  count        = var.env == "prd" ? 1 : 0
  project      = var.gcp_resources_project
  display_name = "Antu - System error"
  combiner     = "OR"
  depends_on   = [time_sleep.wait_for_system_errors_metric]

  conditions {
    display_name = "System errors in the last 10 minutes"
    condition_threshold {
      filter          = "resource.type = \"logging_bucket\" AND metric.type = \"logging.googleapis.com/user/${google_logging_metric.system_errors[0].name}\""
      comparison      = "COMPARISON_GT"
      threshold_value = 0
      # The metric is sparse, so without evaluation_missing_data the incident
      # hangs until auto_close. It needs a retest window, hence 60s not 0s.
      duration                = "60s"
      evaluation_missing_data = "EVALUATION_MISSING_DATA_INACTIVE"
      trigger {
        count = 1
      }
      aggregations {
        alignment_period     = "600s"
        per_series_aligner   = "ALIGN_DELTA"
        cross_series_reducer = "REDUCE_SUM"
      }
    }
  }

  # documentation.subject needs google provider v5+; until then Slack shows
  # display_name.
  documentation {
    content = "An exception aborted a NeTEx validation run. Recovery: in Logs Explorer, scope the query to the log-ent-antu-prd bucket (the default scope shows nothing), query severity>=ERROR jsonPayload.message:\"System error\", take referential and reportId from the log line, and re-trigger that import from Marduk. The report still completes and looks valid: the dataset and cross-file paths add a SYSTEM_ERROR entry, but the single-file path replaces that file's report with a lone SYSTEM_ERROR entry and drops its findings. Treat reports from this window as untrustworthy. If 'Antu Redis - keys evicted' fired too, that is the likely cause and the whole night is suspect."
  }

  alert_strategy {
    auto_close = "28800s"
  }

  notification_channels = [data.google_monitoring_notification_channel.antu_slack_alert[0].name]
  user_labels           = var.labels
}

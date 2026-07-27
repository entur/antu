# Redis alerting. Replaces the console-created policy
# "Antu Redis - Memory usage above 50%": normal nightly validation batches
# peak at 41-52% system memory, so that threshold fires on ordinary heavy
# nights. After these policies are verified in prd, delete the old one:
#   gcloud alpha monitoring policies delete projects/ent-antu-prd/alertPolicies/1393222744772184491
#
# dev/tst intentionally get no Redis alerting: the Slack notification channel
# only exists in prd. To add it, create a channel there and drop the count guards.

# Console-managed channel (holds a Slack token, so not terraform-managed).
# Renaming it in the console breaks terraform plan in prd only; update
# display_name here in the same change.
data "google_monitoring_notification_channel" "antu_slack_alert" {
  count        = var.env == "prd" ? 1 : 0
  display_name = "Antu Slack Alert"
  project      = var.gcp_resources_project
}

resource "google_monitoring_alert_policy" "redis_evictions" {
  count        = var.env == "prd" ? 1 : 0
  project      = var.gcp_resources_project
  display_name = "Antu Redis - keys evicted"
  # severity requires google provider v5+; add CRITICAL here when the provider is bumped
  combiner     = "OR"

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
    content = "Redis evicted keys under memory pressure. The instance uses allkeys-lru, so evictions can remove in-flight validation state (NeTEx id uniqueness sets), producing false-pass validation reports with no error anywhere. Recovery: find validations that overlapped the eviction timestamps (antu logs) and re-trigger them from Marduk; treat reports from that window as untrustworthy. Prevention: raise memory_size_gb and maxmemory-gb on google_redis_instance.antu-redis in terraform/main.tf. Backlog: check evicted_keys at validation-report finalization in antu so this becomes a loud validation failure instead of silent corruption."
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
    content = "Capacity signal, not an incident: the nightly validation batch peaked above 85% of maxmemory (normal peaks are 75-80% around 00:10, back to ~10% by 01:30). No night-time action; eviction has its own alert. Next business day: check the peak trend, and if peaks stay above 85%, raise memory_size_gb and maxmemory-gb on google_redis_instance.antu-redis in terraform/main.tf."
  }

  alert_strategy {
    auto_close = "86400s"
  }

  notification_channels = [data.google_monitoring_notification_channel.antu_slack_alert[0].name]
  user_labels           = var.labels
}

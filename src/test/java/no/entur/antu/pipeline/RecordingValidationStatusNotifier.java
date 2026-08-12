package no.entur.antu.pipeline;

import java.util.ArrayList;
import java.util.List;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.job.ValidationStatus;
import no.entur.antu.job.ValidationStatusNotifier;

public class RecordingValidationStatusNotifier
  implements ValidationStatusNotifier {

  public record Notification(
    ValidationContext context,
    ValidationStatus status
  ) {}

  private final List<Notification> notifications = new ArrayList<>();

  @Override
  public void notifyStatus(ValidationContext context, ValidationStatus status) {
    notifications.add(new Notification(context, status));
  }

  public List<Notification> notifications() {
    return List.copyOf(notifications);
  }

  public List<ValidationStatus> statuses() {
    return notifications.stream().map(Notification::status).toList();
  }

  public void reset() {
    notifications.clear();
  }
}

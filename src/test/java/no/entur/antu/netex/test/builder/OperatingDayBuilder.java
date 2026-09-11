package no.entur.antu.netex.test.builder;

import java.time.LocalDate;
import org.rutebanken.netex.model.OperatingDay;

public class OperatingDayBuilder extends EntityBuilder<OperatingDay> {

  private final LocalDate calendarDate;

  public OperatingDayBuilder(int id, LocalDate calendarDate) {
    super("OperatingDay", id);
    this.calendarDate = calendarDate;
  }

  @Override
  public OperatingDay build() {
    return new OperatingDay()
      .withId(ref())
      .withCalendarDate(calendarDate.atStartOfDay());
  }
}

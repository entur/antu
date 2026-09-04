package no.entur.antu.netex.test.builder;

import org.rutebanken.netex.model.DayType;

public class DayTypeBuilder extends EntityBuilder<DayType> {

  public DayTypeBuilder(int id) {
    super("DayType", id);
  }

  @Override
  public DayType build() {
    return new DayType().withId(ref());
  }
}

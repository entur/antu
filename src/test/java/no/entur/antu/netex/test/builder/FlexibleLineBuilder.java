package no.entur.antu.netex.test.builder;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;
import org.rutebanken.netex.model.MultilingualString;

public class FlexibleLineBuilder extends GenericLineBuilder<FlexibleLine> {

  private FlexibleLineTypeEnumeration flexibleLineType;

  public FlexibleLineBuilder(int id) {
    super("FlexibleLine", id);
  }

  public FlexibleLineBuilder withFlexibleLineType(
    FlexibleLineTypeEnumeration flexibleLineType
  ) {
    this.flexibleLineType = flexibleLineType;
    return this;
  }

  @Override
  public FlexibleLine build() {
    return new FlexibleLine()
      .withId(ref())
      .withFlexibleLineType(flexibleLineType)
      .withName(new MultilingualString().withValue("FlexibleLine " + id))
      .withTransportMode(transportMode)
      .withTransportSubmode(transportSubmode)
      .withOperatorRef(operatorRef);
  }

  @Override
  public void registerInto(NetexEntitiesIndex netexEntitiesIndex) {
    netexEntitiesIndex.getFlexibleLineIndex().put(ref(), build());
  }
}

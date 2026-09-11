package no.entur.antu.netex.test.builder;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.MultilingualString;

public class LineBuilder extends GenericLineBuilder<Line> {

  public LineBuilder(int id) {
    super("Line", id);
  }

  @Override
  public Line build() {
    return new Line()
      .withId(ref())
      .withName(new MultilingualString().withValue("Line " + id))
      .withTransportMode(transportMode)
      .withTransportSubmode(transportSubmode)
      .withOperatorRef(operatorRef);
  }

  @Override
  public void registerInto(NetexEntitiesIndex netexEntitiesIndex) {
    netexEntitiesIndex.getLineIndex().put(ref(), build());
  }
}

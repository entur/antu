package no.entur.antu.netex.test.builder;

import java.util.List;
import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.ObjectFactory;
import net.opengis.gml._3.PolygonType;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.MultilingualString;

public class FlexibleAreaBuilder extends EntityBuilder<FlexibleArea> {

  private List<Double> coordinates;
  private boolean withNullPolygon = false;

  public FlexibleAreaBuilder(int id) {
    super("FlexibleArea", id);
  }

  public FlexibleAreaBuilder withCoordinates(List<Double> coordinates) {
    this.coordinates = coordinates;
    return this;
  }

  public FlexibleAreaBuilder withNullPolygon(boolean withNullPolygon) {
    this.withNullPolygon = withNullPolygon;
    return this;
  }

  @Override
  public FlexibleArea build() {
    LinearRingType linearRing = new LinearRingType();
    DirectPositionListType positionList = new DirectPositionListType()
      .withValue(coordinates);
    linearRing.withPosList(positionList);

    FlexibleArea flexibleArea = new FlexibleArea()
      .withId(ref())
      .withName(new MultilingualString().withValue("FlexibleArea " + id));

    if (withNullPolygon) {
      return flexibleArea.withPolygon(null);
    }

    return flexibleArea.withPolygon(
      new PolygonType()
        .withExterior(
          new AbstractRingPropertyType()
            .withAbstractRing(new ObjectFactory().createLinearRing(linearRing))
        )
    );
  }
}

package no.entur.antu.netex.test.builder;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createJaxbElement;

import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.Route;

public class RouteBuilder extends EntityBuilder<Route> {

  private final GenericLineBuilder<? extends Line_VersionStructure> lineRef;

  public RouteBuilder(
    int id,
    GenericLineBuilder<? extends Line_VersionStructure> lineRef
  ) {
    super("Route", id);
    this.lineRef = lineRef;
  }

  @Override
  public Route build() {
    return new Route()
      .withId(ref())
      .withLineRef(
        createJaxbElement(new LineRefStructure().withRef(lineRef.ref()))
      );
  }
}

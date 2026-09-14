package no.entur.antu.netex.test.builder;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createJaxbElement;

import java.util.List;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LineStringType;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.Projections_RelStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceLink;

public class ServiceLinkBuilder extends EntityBuilder<ServiceLink> {

  private ScheduledStopPointRefStructure fromScheduledStopPointRef;
  private ScheduledStopPointRefStructure toScheduledStopPointRef;
  private LinkSequenceProjection_VersionStructure linkSequenceProjection;

  public ServiceLinkBuilder(int id) {
    super("ServiceLink", id);
  }

  public ServiceLinkBuilder withFromScheduledStopPointRef(
    ScheduledStopPointRefStructure fromScheduledStopPointRef
  ) {
    this.fromScheduledStopPointRef = fromScheduledStopPointRef;
    return this;
  }

  public ServiceLinkBuilder withToScheduledStopPointRef(
    ScheduledStopPointRefStructure toScheduledStopPointRef
  ) {
    this.toScheduledStopPointRef = toScheduledStopPointRef;
    return this;
  }

  public ServiceLinkBuilder withLineStringList(
    List<Double> lineStringPositions
  ) {
    this.linkSequenceProjection =
      new LinkSequenceProjection_VersionStructure()
        .withId("TST:ServiceLinkProjection:" + id)
        .withLineString(
          new LineStringType()
            .withPosList(
              new DirectPositionListType().withValue(lineStringPositions)
            )
        );
    return this;
  }

  public ServiceLinkBuilder withLineStringPositions(
    List<DirectPositionType> lineStringPositions
  ) {
    this.linkSequenceProjection =
      new LinkSequenceProjection_VersionStructure()
        .withId("TST:ServiceLinkProjection:" + id)
        .withLineString(
          new LineStringType()
            .withPosOrPointProperty(lineStringPositions.toArray(Object[]::new))
        );
    return this;
  }

  @Override
  public ServiceLink build() {
    return new ServiceLink()
      .withId(ref())
      .withFromPointRef(fromScheduledStopPointRef)
      .withToPointRef(toScheduledStopPointRef)
      .withProjections(
        new Projections_RelStructure()
          .withProjectionRefOrProjection(
            createJaxbElement(linkSequenceProjection)
          )
      );
  }
}

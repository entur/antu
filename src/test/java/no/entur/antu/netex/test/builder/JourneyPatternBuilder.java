package no.entur.antu.netex.test.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LinkInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.LinksInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.RouteRefStructure;

public class JourneyPatternBuilder extends EntityBuilder<JourneyPattern> {

  private RouteBuilder routeRef;

  private final List<StopPointInJourneyPatternBuilder> stopPointsInJourneyPatterns =
    new ArrayList<>();

  private final List<LinkInJourneyPatternBuilder> serviceLinksInJourneyPatterns =
    new ArrayList<>();

  private boolean noServiceLinksInJourneyPattern = false;

  public JourneyPatternBuilder(int id) {
    super("JourneyPattern", id);
  }

  public JourneyPatternBuilder withRoute(RouteBuilder routeRef) {
    this.routeRef = routeRef;
    return this;
  }

  public JourneyPatternBuilder withNoServiceLinksInJourneyPattern() {
    this.noServiceLinksInJourneyPattern = true;
    return this;
  }

  /**
   * Adds a stop point to this journey pattern, ordered by and referring to the scheduled stop
   * point of the same number.
   *
   * @param id the id of the stop point in the journey pattern
   * @return StopPointInJourneyPatternBuilder
   */
  public StopPointInJourneyPatternBuilder addStopPoint(int id) {
    StopPointInJourneyPatternBuilder stopPointInJourneyPattern =
      new StopPointInJourneyPatternBuilder(id)
        .withOrder(id)
        .withScheduledStopPointRef(NetexRefs.scheduledStopPointRef(id));
    stopPointsInJourneyPatterns.add(stopPointInJourneyPattern);
    return stopPointInJourneyPattern;
  }

  /**
   * Adds a link to this journey pattern, without a service link ref. Use
   * {@link #addLinks(int)} to get links that refer to the service link of the same number.
   *
   * @param id the id of the link in the journey pattern
   * @return LinkInJourneyPatternBuilder
   */
  public LinkInJourneyPatternBuilder addLink(int id) {
    LinkInJourneyPatternBuilder linkInJourneyPattern =
      new LinkInJourneyPatternBuilder(id);
    serviceLinksInJourneyPatterns.add(linkInJourneyPattern);
    return linkInJourneyPattern;
  }

  /**
   * Adds numberOfStopPointInJourneyPattern stop points with ids 1..n. The first is for boarding,
   * the last is for alighting, and both carry a destination display.
   *
   * @param numberOfStopPointInJourneyPattern the number of stop points to add
   * @return the added builders, in order
   */
  public List<StopPointInJourneyPatternBuilder> addStopPoints(
    int numberOfStopPointInJourneyPattern
  ) {
    List<StopPointInJourneyPatternBuilder> addedStopPoints = IntStream
      .rangeClosed(1, numberOfStopPointInJourneyPattern)
      .mapToObj(index -> {
        StopPointInJourneyPatternBuilder stopPointInJourneyPattern =
          new StopPointInJourneyPatternBuilder(index)
            .withOrder(index)
            .withScheduledStopPointRef(NetexRefs.scheduledStopPointRef(index))
            .withForBoarding(index == 1) // first stop point
            .withForAlighting(index == numberOfStopPointInJourneyPattern); // last stop point

        // Setting destination display id for first and last stop point
        if (index == 1 || index == numberOfStopPointInJourneyPattern) {
          stopPointInJourneyPattern.withDestinationDisplayId(
            NetexRefs.destinationDisplayRef(index)
          );
        }

        return stopPointInJourneyPattern;
      })
      .toList();

    this.stopPointsInJourneyPatterns.addAll(addedStopPoints);
    return addedStopPoints;
  }

  /**
   * Adds numberOfServiceLinksInJourneyPattern service links with ids 1..n, each referring to the
   * service link of the same number.
   *
   * @param numberOfServiceLinksInJourneyPattern the number of service links to add
   * @return the added builders, in order
   */
  public List<LinkInJourneyPatternBuilder> addLinks(
    int numberOfServiceLinksInJourneyPattern
  ) {
    List<LinkInJourneyPatternBuilder> addedLinks = IntStream
      .range(0, numberOfServiceLinksInJourneyPattern)
      .mapToObj(index ->
        new LinkInJourneyPatternBuilder(index + 1)
          .withOrder(index + 1)
          .withServiceLinkRef(NetexRefs.serviceLinkRef(index + 1))
      )
      .toList();

    serviceLinksInJourneyPatterns.addAll(addedLinks);
    return addedLinks;
  }

  @Override
  public JourneyPattern build() {
    JourneyPattern journeyPattern = new JourneyPattern().withId(ref());

    if (routeRef != null) {
      journeyPattern.withRouteRef(
        new RouteRefStructure().withRef(routeRef.ref())
      );
    }

    journeyPattern.withPointsInSequence(
      new PointsInJourneyPattern_RelStructure()
        .withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(
          this.stopPointsInJourneyPatterns.isEmpty()
            ? List.of()
            : this.stopPointsInJourneyPatterns.stream()
              .map(StopPointInJourneyPatternBuilder::build)
              .map(PointInLinkSequence_VersionedChildStructure.class::cast)
              .toList()
        )
    );

    if (!noServiceLinksInJourneyPattern) {
      journeyPattern.withLinksInSequence(
        new LinksInJourneyPattern_RelStructure()
          .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
            this.serviceLinksInJourneyPatterns.isEmpty()
              ? List.of()
              : this.serviceLinksInJourneyPatterns.stream()
                .map(LinkInJourneyPatternBuilder::build)
                .map(LinkInLinkSequence_VersionedChildStructure.class::cast)
                .toList()
          )
      );
    }

    return journeyPattern;
  }
}

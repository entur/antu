package no.entur.antu.netextestdata;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NetexXmlTestFragment {

  public static final String TEST_CODESPACE = "TST";

  public CreateStopPointInJourneyPattern stopPointInJourneyPattern(
    int journeyPatternId
  ) {
    return new CreateStopPointInJourneyPattern()
      .withJourneyPatternId(journeyPatternId);
  }

  public CreatePointsInSequence pointsInSequence(int numberOfStopPoints) {
    return pointsInSequence(numberOfStopPoints, UnaryOperator.identity());
  }

  public CreatePointsInSequence pointsInSequence(
    int numberOfStopPoints,
    UnaryOperator<List<CreateStopPointInJourneyPattern>> modificationFunction
  ) {
    return pointsInSequence(numberOfStopPoints, 1, modificationFunction);
  }

  public CreatePointsInSequence pointsInSequence(
    int numberOfStopPoints,
    int journeyPatternId,
    UnaryOperator<List<CreateStopPointInJourneyPattern>> modificationFunction
  ) {
    return pointsInSequence(
      IntStream
        .rangeClosed(1, numberOfStopPoints)
        .mapToObj(i ->
          stopPointInJourneyPattern(journeyPatternId).withOrder(i).withId(i)
        )
        .collect(
          Collectors.collectingAndThen(
            Collectors.toList(),
            modificationFunction
          )
        )
    );
  }

  public CreatePointsInSequence pointsInSequence(
    List<CreateStopPointInJourneyPattern> stopPoints
  ) {
    return new CreatePointsInSequence(stopPoints);
  }

  public CreateJourneyPattern journeyPattern(
    CreatePointsInSequence pointsInSequences
  ) {
    return new CreateJourneyPattern(pointsInSequences);
  }

  public CreateJourneyPatterns journeyPatterns(
    CreateJourneyPattern journeyPattern
  ) {
    return journeyPatterns(List.of(journeyPattern));
  }

  public CreateJourneyPatterns journeyPatterns(
    List<CreateJourneyPattern> journeyPatterns
  ) {
    return new CreateJourneyPatterns(journeyPatterns);
  }

  public CreateJourneyPatterns journeyPatterns(
    int numberOfJourneyPatterns,
    Function<Integer, CreatePointsInSequence> pointsInSequence
  ) {
    return journeyPatterns(
      IntStream
        .rangeClosed(1, numberOfJourneyPatterns)
        .mapToObj(jpIndex ->
          journeyPattern(pointsInSequence.apply(jpIndex)).withId(jpIndex)
        )
        .toList()
    );
  }

  public CreateServiceFrame serviceFrame(
    CreateJourneyPatterns createJourneyPatterns
  ) {
    return new CreateServiceFrame(createJourneyPatterns);
  }

  public static class CreateServiceFrame {

    private final CreateJourneyPatterns createJourneyPatterns;

    public CreateServiceFrame(CreateJourneyPatterns createJourneyPatterns) {
      this.createJourneyPatterns = createJourneyPatterns;
    }

    public String create() {
      return """
        <ServiceFrame xmlns="http://www.netex.org.uk/netex" version="1" id="TST:ServiceFrame:1">
          ${journeyPatterns}
        </ServiceFrame>
        """.replace(
          "${journeyPatterns}",
          createJourneyPatterns.create()
        );
    }
  }

  public static class CreateJourneyPatterns {

    private final List<CreateJourneyPattern> journeyPatterns;

    public CreateJourneyPatterns(List<CreateJourneyPattern> journeyPatterns) {
      this.journeyPatterns = journeyPatterns;
    }

    public String create() {
      return """
        <journeyPatterns>${journeyPatterns}</journeyPatterns>
        """.replace(
          "${journeyPatterns}",
          journeyPatterns
            .stream()
            .map(CreateJourneyPattern::create)
            .collect(Collectors.joining("\n"))
        );
    }
  }

  public static class CreateJourneyPattern {

    private int id = 1;
    private final CreatePointsInSequence pointsInSequences;

    public CreateJourneyPattern withId(int id) {
      this.id = id;
      return this;
    }

    public CreateJourneyPattern() {
      this.pointsInSequences = new CreatePointsInSequence(List.of());
    }

    public CreateJourneyPattern(CreatePointsInSequence pointsInSequences) {
      this.pointsInSequences = pointsInSequences;
    }

    public String create() {
      return """
          <JourneyPattern version="0" id="TST:JourneyPattern:${id}">
            <Name>journey-pattern-${id}</Name>
            <RouteRef ref="TST:Route:${id}" version="0"></RouteRef>
            ${pointsInSequences}
          </JourneyPattern>
        """.replace(
          "${pointsInSequences}",
          pointsInSequences.create()
        )
        .replace("${id}", id + "");
    }
  }

  public static class CreatePointsInSequence {

    private final List<CreateStopPointInJourneyPattern> stopPoints;

    public CreatePointsInSequence(
      List<CreateStopPointInJourneyPattern> stopPoints
    ) {
      this.stopPoints = stopPoints;
    }

    public String create() {
      return (
        "<pointsInSequence>" +
        stopPoints
          .stream()
          .map(CreateStopPointInJourneyPattern::create)
          .collect(Collectors.joining("\n")) +
        "</pointsInSequence>"
      );
    }
  }

  public static class CreateStopPointInJourneyPattern {

    private int order = 1;
    private int id = 1;
    private int journeyPatternId = 1;
    private Boolean forBoarding;
    private Boolean forAlighting;

    public CreateStopPointInJourneyPattern withOrder(int order) {
      this.order = order;
      return this;
    }

    public CreateStopPointInJourneyPattern withId(int id) {
      this.id = id;
      return this;
    }

    public CreateStopPointInJourneyPattern withJourneyPatternId(
      int journeyPatternId
    ) {
      this.journeyPatternId = journeyPatternId;
      return this;
    }

    public CreateStopPointInJourneyPattern withForBoarding(
      Boolean forBoarding
    ) {
      this.forBoarding = forBoarding;
      return this;
    }

    public CreateStopPointInJourneyPattern withForAlighting(
      Boolean forAlighting
    ) {
      this.forAlighting = forAlighting;
      return this;
    }

    public String create() {
      return """
        <StopPointInJourneyPattern
          order="${order}"
          version="14"
          id="TST:StopPointInJourneyPattern:${journeyPatternId}-${id}">
            <ScheduledStopPointRef ref="TST:ScheduledStopPoint:${journeyPatternId}-${id}"></ScheduledStopPointRef>
            <DestinationDisplayRef ref="TST:DestinationDisplay:${journeyPatternId}-${id}"></DestinationDisplayRef>
            ${forBoarding}
            ${forAlighting}
        </StopPointInJourneyPattern>
        """.replace(
          "${order}",
          order + ""
        )
        .replace("${id}", id + "")
        .replace("${journeyPatternId}", journeyPatternId + "")
        .replace("${forBoarding}", getForBoarding())
        .replace("${forAlighting}", getForAlighting());
    }

    private String getForAlighting() {
      return forAlighting == null
        ? ""
        : "<ForAlighting>${forAlighting}</ForAlighting>".replace(
            "${forAlighting}",
            forAlighting + ""
          );
    }

    private String getForBoarding() {
      return forBoarding == null
        ? ""
        : "<ForBoarding>${forBoarding}</ForBoarding>".replace(
            "${forBoarding}",
            forBoarding + ""
          );
    }
  }
}

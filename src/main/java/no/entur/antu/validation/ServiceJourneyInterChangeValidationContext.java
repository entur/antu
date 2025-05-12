package no.entur.antu.validation;

import java.time.Duration;

public class ServiceJourneyInterChangeValidationContext {

  private final String serviceJourneyInterchangeId;
  private final String fromJourneyId;
  private final String toJourneyId;
  private final String fromPointId;
  private final String toPointId;
  private final Duration maximumWaitTime;

  private String fromJourneyArrivalTime;
  private String fromJourneyArrivalDayOffset;
  private String toJourneyDepartureTime;
  private String toJourneyDepartureDayOffset;

  private String fromStopPointInJourneyPatternId;
  private String toStopPointInJourneyPatternId;


  public ServiceJourneyInterChangeValidationContext(
      String serviceJourneyInterchangeId,
      String fromJourneyId,
      String toJourneyId,
      String fromPointId,
      String toPointId,
      Duration maximumWaitTime) {
    this.serviceJourneyInterchangeId = serviceJourneyInterchangeId;
    this.fromJourneyId = fromJourneyId;
    this.toJourneyId = toJourneyId;
    this.fromPointId = fromPointId;
    this.toPointId = toPointId;
    this.maximumWaitTime = maximumWaitTime;
  }

  public String getServiceJourneyInterchangeId() {
    return serviceJourneyInterchangeId;
  }

  public String getFromJourneyId() {
    return fromJourneyId;
  }

  public String getToJourneyId() {
    return toJourneyId;
  }

  public String getFromPointId() {
    return fromPointId;
  }

  public String getToPointId() {
    return toPointId;
  }

  public Duration getMaximumWaitTime() {
    return maximumWaitTime;
  }


  public String getFromJourneyArrivalTime() {
    return fromJourneyArrivalTime;
  }

  public void setFromJourneyArrivalTime(String fromJourneyArrivalTime) {
    this.fromJourneyArrivalTime = fromJourneyArrivalTime;
  }

  public String getFromJourneyArrivalDayOffset() {
    return fromJourneyArrivalDayOffset;
  }

  public void setFromJourneyArrivalDayOffset(String fromJourneyArrivalDayOffset) {
    this.fromJourneyArrivalDayOffset = fromJourneyArrivalDayOffset;
  }

  public String getToJourneyDepartureTime() {
    return toJourneyDepartureTime;
  }

  public void setToJourneyDepartureTime(String toJourneyDepartureTime) {
    this.toJourneyDepartureTime = toJourneyDepartureTime;
  }

  public String getToJourneyDepartureDayOffset() {
    return toJourneyDepartureDayOffset;
  }

  public void setToJourneyDepartureDayOffset(String toJourneyDepartureDayOffset) {
    this.toJourneyDepartureDayOffset = toJourneyDepartureDayOffset;
  }

  public String getFromStopPointInJourneyPatternId() {
    return fromStopPointInJourneyPatternId;
  }

  public void setFromStopPointInJourneyPatternId(String fromStopPointInJourneyPatternId) {
    this.fromStopPointInJourneyPatternId = fromStopPointInJourneyPatternId;
  }

  public String getToStopPointInJourneyPatternId() {
    return toStopPointInJourneyPatternId;
  }

  public void setToStopPointInJourneyPatternId(String toStopPointInJourneyPatternId) {
    this.toStopPointInJourneyPatternId = toStopPointInJourneyPatternId;
  }
}

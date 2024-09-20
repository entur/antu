package no.entur.antu.model;

/**
 * Lightway representation of a NeTEx StopPlace.
 * This contains the minimum information required to validate a StopPlace.
 */
public record SimpleStopPlace(
  String name,
  TransportModeAndSubMode transportModeAndSubMode
) {}

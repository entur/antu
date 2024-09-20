package no.entur.antu.model;

/**
 * Lightway representation of a NeTEx quay.
 * This contains the minimum information required to validate a Quay.
 */
public record NetexQuay(
  QuayCoordinates quayCoordinates,
  StopPlaceId stopPlaceId
) {}

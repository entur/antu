package no.entur.antu.netexdata.collectors.activedatecollector.calender;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import org.rutebanken.netex.model.AvailabilityCondition;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.ValidBetween;
import org.rutebanken.netex.model.ValidityConditions_RelStructure;

public class CalendarUtilities {

  static <K, V> Collector<DayTypeAssignment, ?, Multimap<K, V>> toMultimap(
    Function<DayTypeAssignment, K> keyMapper,
    Function<DayTypeAssignment, V> valueMapper
  ) {
    return Collector.of(
      ArrayListMultimap::create, // Supplier: Create a new Multimap
      (multimap, assignment) ->
        multimap.put(
          keyMapper.apply(assignment),
          valueMapper.apply(assignment)
        ), // Accumulator
      (m1, m2) -> { // Combiner
        m1.putAll(m2);
        return m1;
      }
    );
  }

  static ValidBetween getValidBetween(
    ValidityConditions_RelStructure validityConditionStruct
  ) {
    return Optional
      .ofNullable(validityConditionStruct)
      .map(
        ValidityConditions_RelStructure::getValidityConditionRefOrValidBetweenOrValidityCondition_
      )
      .filter(elements -> !elements.isEmpty())
      .map(elements -> elements.get(0))
      .flatMap(CalendarUtilities::toValidBetween)
      .orElse(null);
  }

  private static Optional<ValidBetween> toValidBetween(
    Object validityConditionElement
  ) {
    if (validityConditionElement instanceof ValidBetween) {
      return Optional.of((ValidBetween) validityConditionElement);
    }

    if (validityConditionElement instanceof javax.xml.bind.JAXBElement<?>) {
      return handleJaxbElement(
        (javax.xml.bind.JAXBElement<?>) validityConditionElement
      );
    }

    throw new RuntimeException(
      "Only support ValidBetween and AvailabilityCondition as validityCondition"
    );
  }

  private static Optional<ValidBetween> handleJaxbElement(
    javax.xml.bind.JAXBElement<?> jaxbElement
  ) {
    Object value = jaxbElement.getValue();

    if (value instanceof AvailabilityCondition availabilityCondition) {
      return Optional.of(
        new ValidBetween()
          .withFromDate(availabilityCondition.getFromDate())
          .withToDate(availabilityCondition.getToDate())
      );
    }

    throw new RuntimeException(
      "Only support ValidBetween and AvailabilityCondition as validityCondition"
    );
  }

  public static ValidBetween getValidityForFrameOrDefault(
    Common_VersionFrameStructure frameStructure,
    ValidBetween defaultValidity
  ) {
    if (frameStructure.getContentValidityConditions() != null) {
      return getValidBetween(frameStructure.getContentValidityConditions());
    }

    if (frameStructure.getValidityConditions() != null) {
      return getValidBetween(frameStructure.getValidityConditions());
    }

    if (
      frameStructure.getValidBetween() != null &&
      !frameStructure.getValidBetween().isEmpty()
    ) {
      return frameStructure.getValidBetween().get(0);
    }
    return defaultValidity;
  }

  static boolean isWithinValidRange(
    LocalDateTime dateOfOperation,
    ValidBetween validBetween
  ) {
    if (validBetween == null) {
      // Always valid
      return true;
    } else if (
      validBetween.getFromDate() != null && validBetween.getToDate() != null
    ) {
      // Limited by both from and to date
      return (
        !dateOfOperation.isBefore(validBetween.getFromDate()) &&
        !dateOfOperation.isAfter(validBetween.getToDate())
      );
    } else if (validBetween.getFromDate() != null) {
      // Must be after valid start date
      return !dateOfOperation.isBefore(validBetween.getFromDate());
    } else if (validBetween.getToDate() != null) {
      // Must be before valid start date
      return dateOfOperation.isBefore(validBetween.getToDate());
    } else {
      // Both from and to empty
      return true;
    }
  }

  static <V> V getOrDefault(V value, V defaultValue) {
    return value != null ? value : defaultValue;
  }
}

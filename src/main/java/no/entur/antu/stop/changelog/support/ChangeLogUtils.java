package no.entur.antu.stop.changelog.support;

import java.time.Instant;
import java.time.ZoneId;
import javax.annotation.Nullable;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.SiteFrame;

public class ChangeLogUtils {

  private ChangeLogUtils() {}

  private static final String DEFAULT_TIME_ZONE = "Europe/Oslo";

  /**
   * Extract the publication time of a NeTEx PublicationDelivery.
   *
   */
  @Nullable
  public static Instant parsePublicationTime(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    var localPublicationTimestamp =
      netexEntitiesIndex.getPublicationTimestamp();

    if (localPublicationTimestamp == null) {
      return null;
    }

    var timeZone = netexEntitiesIndex
      .getSiteFrames()
      .stream()
      .findFirst()
      .map(ChangeLogUtils::safeGetTimeZone)
      .orElse(DEFAULT_TIME_ZONE);
    return localPublicationTimestamp.atZone(ZoneId.of(timeZone)).toInstant();
  }

  private static String safeGetTimeZone(SiteFrame frame) {
    if (
      frame.getFrameDefaults() != null &&
      frame.getFrameDefaults().getDefaultLocale() != null
    ) {
      return frame.getFrameDefaults().getDefaultLocale().getTimeZone();
    }
    return DEFAULT_TIME_ZONE;
  }
}

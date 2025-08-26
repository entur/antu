package no.entur.antu.stop.changelog.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.junit.jupiter.api.Test;

class ChangeLogUtilsTest {

  private static final String SITE_FRAME =
    """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex" xmlns:ns2="http://www.opengis.net/gml/3.2" xmlns:ns3="http://www.siri.org.uk/siri" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.12:NO-NeTEx-stops:1.4" xsi:schemaLocation="">
                <PublicationTimestamp>2023-06-08T12:09:20.879</PublicationTimestamp>
                <dataObjects>
                    <SiteFrame modification="new" version="1" id="NSR:SiteFrame:858500319">
                        <FrameDefaults>
                            <DefaultLocale>
                                <TimeZone>Europe/Oslo</TimeZone>
                            </DefaultLocale>
                        </FrameDefaults>
                    </SiteFrame>
                </dataObjects>
            </PublicationDelivery>
            """;

  @Test
  void loadStopPlace() {
    NetexParser parser = new NetexParser();
    NetexEntitiesIndex index = parser.parse(
      new ByteArrayInputStream(SITE_FRAME.getBytes(StandardCharsets.UTF_8))
    );
    Instant publicationTime = ChangeLogUtils.parsePublicationTime(index);
    assertNotNull(publicationTime);

    Instant expected = LocalDateTime
      .parse("2023-06-08T12:09:20.879", DateTimeFormatter.ISO_DATE_TIME)
      .atZone(ZoneId.of("Europe/Oslo"))
      .toInstant();
    assertEquals(expected, publicationTime);
  }
}

package no.entur.antu.stop.changelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import no.entur.antu.stop.DefaultStopPlaceRepository;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AntuStopPlaceChangeLogListenerTest {

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
                                <stopPlaces>
                                    <StopPlace created="2017-11-09T19:12:03.026" changed="2021-04-22T18:26:07.254" modification="new" version="13" id="NSR:StopPlace:58586">
                                        <Name lang="nor">Sandefjord lufthavn</Name>
                                        <Description lang="nor"></Description>
                                        <Centroid>
                                            <Location>
                                                <Longitude>10.253136</Longitude>
                                                <Latitude>59.179097</Latitude>
                                            </Location>
                                        </Centroid>
                                        <TransportMode>air</TransportMode>
                                        <StopPlaceType>airport</StopPlaceType>
                                        <quays>
                                            <Quay changed="2018-10-18T16:13:25.256" modification="new" version="13" id="NSR:Quay:100310">
                                                <PrivateCode></PrivateCode>
                                                <Centroid>
                                                    <Location>
                                                        <Longitude>10.253157</Longitude>
                                                        <Latitude>59.179070</Latitude>
                                                    </Location>
                                                </Centroid>
                                            </Quay>
                                        </quays>
                                    </StopPlace>
                                    <StopPlace created="2017-11-09T19:12:03.026" changed="2021-04-22T18:26:07.254" modification="new" version="13" id="NSR:StopPlace:58587">
                                        <Name lang="nor">Sandefjord lufthavn parent stop</Name>
                                        <Description lang="nor"></Description>
                                        <Centroid>
                                            <Location>
                                                <Longitude>10.253136</Longitude>
                                                <Latitude>59.179097</Latitude>
                                            </Location>
                                        </Centroid>
                                        <TransportMode>air</TransportMode>
                                        <StopPlaceType>airport</StopPlaceType>
                                        <keyList>
                                            <KeyValue>
                                                <Key>IS_PARENT_STOP_PLACE</Key>
                                                <Value>true</Value>
                                            </KeyValue>
                                        </keyList>
                                    </StopPlace>
                                    <StopPlace created="2017-11-09T19:12:03.026" changed="2021-04-22T18:26:07.254" modification="new" version="13" id="NSR:StopPlace:58588">
                                        <Name lang="nor">Sandefjord lufthavn child stop</Name>
                                        <Description lang="nor"></Description>
                                        <Centroid>
                                            <Location>
                                                <Longitude>10.253136</Longitude>
                                                <Latitude>59.179097</Latitude>
                                            </Location>
                                        </Centroid>
                                        <TransportMode>air</TransportMode>
                                        <StopPlaceType>airport</StopPlaceType>
                                        <ParentSiteRef ref="NSR:StopPlace:58587" version="3"/>
                                        <quays>
                                            <Quay changed="2018-10-18T16:13:25.256" modification="new" version="13" id="NSR:Quay:100311">
                                                <PrivateCode></PrivateCode>
                                                <Centroid>
                                                    <Location>
                                                        <Longitude>10.253157</Longitude>
                                                        <Latitude>59.179070</Latitude>
                                                    </Location>
                                                </Centroid>
                                            </Quay>
                                        </quays>
                                    </StopPlace>
                                </stopPlaces>
                            </SiteFrame>
                        </dataObjects>
                    </PublicationDelivery>
                    """;

  private Map<StopPlaceId, SimpleStopPlace> stopPlaceCache;
  private Map<QuayId, SimpleQuay> quayCache;
  private StopPlaceRepositoryLoader loader;
  private ChangelogUpdateTimestampRepository timestampRepository;
  private AntuStopPlaceChangeLogListener listener;

  @BeforeEach
  void setUp() {
    this.stopPlaceCache = new HashMap<>();
    this.quayCache = new HashMap<>();
    this.loader =
      new DefaultStopPlaceRepository(null, stopPlaceCache, quayCache);
    this.timestampRepository =
      new ChangelogUpdateTimestampRepository() {
        private Instant timestamp;

        @Override
        public void setTimestamp(Instant publicationTime) {
          this.timestamp = publicationTime;
        }

        @Override
        public Instant getTimestamp() {
          return timestamp;
        }
      };
    this.listener =
      new AntuStopPlaceChangeLogListener(loader, timestampRepository);
  }

  @Test
  void createStopPlace() {
    listener.onStopPlaceCreated(
      "id",
      new ByteArrayInputStream(SITE_FRAME.getBytes(StandardCharsets.UTF_8))
    );
    Instant expectedPublicationTimestamp = LocalDateTime
      .parse("2023-06-08T12:09:20.879", DateTimeFormatter.ISO_DATE_TIME)
      .atZone(ZoneId.of("Europe/Oslo"))
      .toInstant();
    assertEquals(
      expectedPublicationTimestamp,
      timestampRepository.getTimestamp()
    );
    assertEquals(3, stopPlaceCache.size());
    StopPlaceId stopPlaceId = new StopPlaceId("NSR:StopPlace:58586");
    assertTrue(stopPlaceCache.containsKey(stopPlaceId));
    assertEquals("Sandefjord lufthavn", stopPlaceCache.get(stopPlaceId).name());
    assertEquals(2, quayCache.size());
    QuayId quayId = new QuayId("NSR:Quay:100310");
    assertTrue(quayCache.containsKey(quayId));
    assertEquals(
      new QuayCoordinates(10.253157, 59.179070),
      quayCache.get(quayId).quayCoordinates()
    );
    assertEquals(stopPlaceId, quayCache.get(quayId).stopPlaceId());
  }

  @Test
  void deleteStopPlaceWontDeleteParentStops() {
    listener.onStopPlaceCreated(
      "id",
      new ByteArrayInputStream(SITE_FRAME.getBytes(StandardCharsets.UTF_8))
    );
    listener.onStopPlaceDeleted("NSR:StopPlace:58587");
    Instant expectedPublicationTimestamp = LocalDateTime
      .parse("2023-06-08T12:09:20.879", DateTimeFormatter.ISO_DATE_TIME)
      .atZone(ZoneId.of("Europe/Oslo"))
      .toInstant();
    assertEquals(
      expectedPublicationTimestamp,
      timestampRepository.getTimestamp()
    );
    assertEquals(3, stopPlaceCache.size());
  }

  @Test
  void deleteStopPlaceDeletesOrdinaryStopsIncludingQuays() {
    listener.onStopPlaceCreated(
      "id",
      new ByteArrayInputStream(SITE_FRAME.getBytes(StandardCharsets.UTF_8))
    );
    listener.onStopPlaceDeleted("NSR:StopPlace:58588");
    Instant expectedPublicationTimestamp = LocalDateTime
      .parse("2023-06-08T12:09:20.879", DateTimeFormatter.ISO_DATE_TIME)
      .atZone(ZoneId.of("Europe/Oslo"))
      .toInstant();
    assertEquals(
      expectedPublicationTimestamp,
      timestampRepository.getTimestamp()
    );
    assertEquals(2, stopPlaceCache.size());
    assertEquals(1, quayCache.size());
  }

  @Test
  void deactivateStopPlaceWontDeleteParentStops() {
    InputStream inputStream = new ByteArrayInputStream(
      SITE_FRAME.getBytes(StandardCharsets.UTF_8)
    );
    listener.onStopPlaceCreated("id", inputStream);
    listener.onStopPlaceDeactivated("NSR:StopPlace:58587", inputStream);
    Instant expectedPublicationTimestamp = LocalDateTime
      .parse("2023-06-08T12:09:20.879", DateTimeFormatter.ISO_DATE_TIME)
      .atZone(ZoneId.of("Europe/Oslo"))
      .toInstant();
    assertEquals(
      expectedPublicationTimestamp,
      timestampRepository.getTimestamp()
    );
    assertEquals(3, stopPlaceCache.size());
  }

  @Test
  void deactivateStopPlaceDeletesOrdinaryStopsIncludingQuays() {
    InputStream inputStream = new ByteArrayInputStream(
      SITE_FRAME.getBytes(StandardCharsets.UTF_8)
    );
    listener.onStopPlaceCreated("id", inputStream);
    listener.onStopPlaceDeactivated("NSR:StopPlace:58588", inputStream);
    Instant expectedPublicationTimestamp = LocalDateTime
      .parse("2023-06-08T12:09:20.879", DateTimeFormatter.ISO_DATE_TIME)
      .atZone(ZoneId.of("Europe/Oslo"))
      .toInstant();
    assertEquals(
      expectedPublicationTimestamp,
      timestampRepository.getTimestamp()
    );
    assertEquals(2, stopPlaceCache.size());
    assertEquals(1, quayCache.size());
  }
}

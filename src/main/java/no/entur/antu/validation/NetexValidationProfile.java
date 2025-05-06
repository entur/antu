package no.entur.antu.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import no.entur.antu.exception.AntuException;
import no.entur.antu.utils.zip.ZipStreamUtil;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntityIndex;
import org.entur.netex.validation.validator.NetexValidationProgressCallBack;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReport;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate a NeTEx dataset according to a given validation profile.
 * A Validation profile defines the set of rules to be applied during the validation.
 */
public class NetexValidationProfile {

  private static final Logger log = LoggerFactory.getLogger(NetexValidationProfile.class);
  private final Map<ValidationProfile, NetexValidatorsRunner> netexValidatorsRunners;
  private final boolean skipSchemaValidation;
  private final boolean skipNetexValidators;

  public NetexValidationProfile(
    Map<ValidationProfile, NetexValidatorsRunner> netexValidatorsRunners,
    boolean skipSchemaValidation,
    boolean skipNetexValidators
  ) {
    this.netexValidatorsRunners = netexValidatorsRunners;
    this.skipSchemaValidation = skipSchemaValidation;
    this.skipNetexValidators = skipNetexValidators;
  }

  /**
   * Validate a NeTEx file according to a validation profile
   *
   * @param validationProfile  the NeTEx validation profile
   * @param codespace          the dataset codespace.
   * @param validationReportId the report id.
   * @param filename           the name of the NeTEx file.
   * @param fileContent        the binary content of the NeTEx file.
   * @return a ValidationReport listing the findings for this NeTEx file.
   */
  public ValidationReport validate(
    String validationProfile,
    String codespace,
    String validationReportId,
    String filename,
    byte[] fileContent,
    NetexValidationProgressCallBack netexValidationProgressCallBack
  ) {
    if (validationProfile == null) {
      throw new AntuException("Missing validation profile");
    }
    if (codespace == null) {
      throw new AntuException("Missing codespace");
    }
    NetexValidatorsRunner netexValidatorsRunner = getNetexValidatorsRunner(
      validationProfile
    );
    return netexValidatorsRunner.validate(
      codespace,
      validationReportId,
      filename,
      fileContent,
      skipSchemaValidation,
      skipNetexValidators,
      netexValidationProgressCallBack
    );
  }

  private Stream<InputStream> zipFileToStreamOfInputStreams(InputStream zipFileAsInputStream) {
    try {
      Stream<InputStream> extractedStreams =
              ZipStreamUtil.createInputStreamFromZip(zipFileAsInputStream);
      log.info(
              "Successfully prepared ZIP stream for XML processing"
      );
      return extractedStreams;
    } catch (Exception e) {
      log.error("Exception during file extraction" + e);
      throw e;
    }
  }

  /**
   * Validate a NeTEx file according to a validation profile
   *
   * @return a ValidationReport listing the findings for this NeTEx file.
   */
  public ValidationReport crossValidateNetexDataset(InputStream zipFileInputStream, String validationProfile) throws IOException {
    if (validationProfile == null) {
      throw new AntuException("Missing validation profile");
    }


    log.info("Validating netex dataset...");

    NetexParser netexParser = new NetexParser();
    NetexValidatorsRunner netexValidatorsRunner = getNetexValidatorsRunner(validationProfile);

    byte[] zipAsByteArray = zipFileInputStream.readAllBytes();

    AtomicReference<NetexEntityIndex<ServiceJourneyInterchange>> serviceJourneyInterchangeNetexEntityIndex = new AtomicReference<>();

    var iterator = zipFileToStreamOfInputStreams(new ByteArrayInputStream(zipAsByteArray)).iterator();
    while (iterator.hasNext()) {
      InputStream fileAsInputStream = iterator.next();
      NetexEntitiesIndex index = netexParser.parse(fileAsInputStream);
      if (serviceJourneyInterchangeNetexEntityIndex.get() == null) {
        serviceJourneyInterchangeNetexEntityIndex.set(index.getServiceJourneyInterchangeIndex());
      } else {
        serviceJourneyInterchangeNetexEntityIndex.get().putAll(index.getServiceJourneyInterchangeIndex().getAll());
      }
    }

    Set<String> serviceJourneyIds = new HashSet<>();
    for (ServiceJourneyInterchange interchange : serviceJourneyInterchangeNetexEntityIndex.get().getAll()) {
      serviceJourneyIds.add(interchange.getFromJourneyRef().getRef());
      serviceJourneyIds.add(interchange.getToJourneyRef().getRef());
    }

    Map<String, ServiceJourney> mapOfServiceJourneys = new HashMap<>();
    iterator = zipFileToStreamOfInputStreams(new ByteArrayInputStream(zipAsByteArray)).iterator();
    while (iterator.hasNext()) {
      InputStream fileInputStream = iterator.next();
      NetexEntitiesIndex index = netexParser.parse(fileInputStream);
      for (ServiceJourney serviceJourney : index.getServiceJourneyIndex().getAll()) {
        if (serviceJourneyIds.contains(serviceJourney.getId())) {
          mapOfServiceJourneys.put(serviceJourney.getId(), serviceJourney);
          serviceJourneyIds.remove(serviceJourney.getId());
        }
      }
    }
    return new ValidationReport();
  }


  /**
   * Validate a NeTEx file according to a validation profile
   *
   * @return a ValidationReport listing the findings for this NeTEx file.
   */
  public ValidationReport validateDataset(
    ValidationReport validationReport,
    String validationProfile,
    NetexValidationProgressCallBack netexValidationProgressCallBack,
    ZipFile zipFile
  ) {
    if (validationReport == null) {
      throw new AntuException("Missing validation report");
    }
    if (validationProfile == null) {
      throw new AntuException("Missing validation profile");
    }
    NetexValidatorsRunner netexValidatorsRunner = getNetexValidatorsRunner(
      validationProfile
    );

    return netexValidatorsRunner.runNetexDatasetValidatorsNext(
      validationReport,
      netexValidationProgressCallBack,
      zipFile
    );
  }

  private NetexValidatorsRunner getNetexValidatorsRunner(
    String validationProfile
  ) {
    ValidationProfile profile = ValidationProfile
      .findById(validationProfile)
      .orElseThrow(() ->
        new AntuException("Unknown validation profile: " + validationProfile)
      );
    NetexValidatorsRunner netexValidatorsRunner = netexValidatorsRunners.get(
      profile
    );
    if (netexValidatorsRunner == null) {
      throw new AntuException(
        "Unknown validation profile " + validationProfile
      );
    }
    return netexValidatorsRunner;
  }
}

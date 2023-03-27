/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package no.entur.antu.config;

import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validator.NetexValidationProfile;
import no.entur.antu.validator.id.NetexIdValidator;
import no.entur.antu.validator.id.ReferenceToNsrValidator;
import no.entur.antu.validator.id.TrainElementRegistryIdValidator;
import org.entur.netex.validation.configuration.DefaultValidationConfigLoader;
import org.entur.netex.validation.configuration.ValidationConfigLoader;
import org.entur.netex.validation.validator.DefaultValidationEntryFactory;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.id.BlockJourneyReferencesIgnorer;
import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.NetexReferenceValidator;
import org.entur.netex.validation.validator.id.NetexIdRepository;
import org.entur.netex.validation.validator.id.NetexIdUniquenessValidator;
import org.entur.netex.validation.validator.id.ReferenceToValidEntityTypeValidator;
import org.entur.netex.validation.validator.id.ServiceJourneyInterchangeIgnorer;
import org.entur.netex.validation.validator.id.VersionOnLocalNetexIdValidator;
import org.entur.netex.validation.validator.id.VersionOnRefToLocalNetexIdValidator;
import org.entur.netex.validation.validator.schema.NetexSchemaValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static no.entur.antu.Constants.*;

@Configuration
public class ValidatorConfig {

    @Bean
    public NetexSchemaValidator netexSchemaValidator(@Value("${antu.netex.validation.entries.max:100}") int maxValidationError) {
        return new NetexSchemaValidator(maxValidationError);
    }

    @Bean
    public ValidationConfigLoader validationConfigLoader(@Value("${antu.netex.validation.configuration.file:configuration.antu.yaml}")String configurationFile) {
        return new DefaultValidationConfigLoader(configurationFile);
    }

    @Bean
    public ValidationReportEntryFactory validationReportEntryFactory(@Qualifier("validationConfigLoader") ValidationConfigLoader validationConfigLoader) {
        return new DefaultValidationEntryFactory(validationConfigLoader);
    }

    @Bean
    public NetexIdValidator netexIdValidator(@Qualifier("validationReportEntryFactory") ValidationReportEntryFactory validationReportEntryFactory) {
        return new NetexIdValidator(validationReportEntryFactory);
    }

    @Bean
    public VersionOnLocalNetexIdValidator versionOnLocalNetexIdValidator(@Qualifier("validationReportEntryFactory") ValidationReportEntryFactory validationReportEntryFactory) {
        return new VersionOnLocalNetexIdValidator(validationReportEntryFactory);
    }

    @Bean
    public VersionOnRefToLocalNetexIdValidator versionOnRefToLocalNetexIdValidator(@Qualifier("validationReportEntryFactory") ValidationReportEntryFactory validationReportEntryFactory) {
        return new VersionOnRefToLocalNetexIdValidator(validationReportEntryFactory);
    }

    @Bean
    public ReferenceToValidEntityTypeValidator refToValidEntityTypeValidator(@Qualifier("validationReportEntryFactory") ValidationReportEntryFactory validationReportEntryFactory) {
        return new ReferenceToValidEntityTypeValidator(validationReportEntryFactory);
    }

    @Bean
    public ReferenceToNsrValidator nsrRefValidator(@Qualifier("stopPlaceRepository")  StopPlaceRepository stopPlaceRepository) {
        return new ReferenceToNsrValidator(stopPlaceRepository);
    }

    @Bean
    public NetexReferenceValidator netexReferenceValidator(NetexIdRepository netexIdRepository, ReferenceToNsrValidator referenceToNsrValidator, @Qualifier("validationReportEntryFactory") ValidationReportEntryFactory validationReportEntryFactory) {
        List<ExternalReferenceValidator> externalReferenceValidators = new ArrayList<>();
        externalReferenceValidators.add(new BlockJourneyReferencesIgnorer());
        externalReferenceValidators.add(new ServiceJourneyInterchangeIgnorer());
        externalReferenceValidators.add(new TrainElementRegistryIdValidator());
        externalReferenceValidators.add(referenceToNsrValidator);
        return new NetexReferenceValidator(netexIdRepository, externalReferenceValidators, validationReportEntryFactory);
    }

    @Bean
    public NetexIdUniquenessValidator netexIdUniquenessValidator(NetexIdRepository netexIdRepository, @Qualifier("validationReportEntryFactory") ValidationReportEntryFactory validationReportEntryFactory) {
        return new NetexIdUniquenessValidator(netexIdRepository, validationReportEntryFactory);
    }

    @Bean
    public NetexValidationProfile netexValidationProfile(@Qualifier("timetableDataValidatorsRunner") NetexValidatorsRunner timetableDataValidatorsRunner,
                                                         @Qualifier("flexTimetableDataValidatorsRunner") NetexValidatorsRunner flexTimetableDataValidatorsRunner,
                                                         @Qualifier("importFlexTimetableDataValidatorsRunner") NetexValidatorsRunner importFlexTimetableDataValidatorsRunner,
                                                         @Qualifier("flexMergingTimetableDataValidatorsRunner") NetexValidatorsRunner flexMergingTimetableDataValidatorsRunner,
                                                         @Qualifier("swedenTimetableDataSwedenValidatorsRunner") NetexValidatorsRunner timetableSwedenDataValidatorsRunner,
                                                         @Qualifier("stopPlaceDataValidatorsRunner") NetexValidatorsRunner stopDataValidatorsRunner,
                                                         @Value("${antu.netex.validation.schema.skip:false}") boolean skipSchemaValidation,
                                                         @Value("${antu.netex.validation.validators.skip:false}") boolean skipNetexValidators) {
        return new NetexValidationProfile(Map.of(
                VALIDATION_PROFILE_TIMETABLE, timetableDataValidatorsRunner,
                VALIDATION_PROFILE_TIMETABLE_FLEX, flexTimetableDataValidatorsRunner,
                VALIDATION_PROFILE_IMPORT_TIMETABLE_FLEX, importFlexTimetableDataValidatorsRunner,
                VALIDATION_PROFILE_TIMETABLE_FLEX_MERGING, flexMergingTimetableDataValidatorsRunner,
                VALIDATION_PROFILE_TIMETABLE_SWEDEN, timetableSwedenDataValidatorsRunner,
                VALIDATION_PROFILE_STOP, stopDataValidatorsRunner
        ), skipSchemaValidation, skipNetexValidators);
    }


}

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

package no.entur.antu.flex.merging.config;

import org.entur.netex.validation.validator.NetexValidator;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.id.NetexIdRepository;
import org.entur.netex.validation.validator.id.NetexIdUniquenessValidator;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration for validating a dataset containing both fixed and flexible transport timetable data.
 * This profile checks only duplicated NeTEx IDs. It assumes that all other validation rules have been applied to the source datasets before they were merged.
 */
@Configuration
public class TimetableDataFlexMergingValidatorConfig {


    @Bean
    public NetexIdUniquenessValidator flexMergingNetexIdUniquenessValidator(NetexIdRepository netexIdRepository, ValidationReportEntryFactory validationReportEntryFactory) {
        Set<String> ignorableElements = new HashSet<>(NetexIdUniquenessValidator.getDefaultIgnorableElements());
        ignorableElements.add("Authority");
        ignorableElements.add("Operator");
        return new NetexIdUniquenessValidator(netexIdRepository, validationReportEntryFactory, ignorableElements);
    }

    @Bean
    public NetexValidatorsRunner flexMergingTimetableDataValidatorsRunner(@Qualifier("flexMergingNetexIdUniquenessValidator") NetexIdUniquenessValidator netexIdUniquenessValidator) {
        List<NetexValidator> netexValidators = List.of(netexIdUniquenessValidator);
        // do not ignore SiteFrame
        NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of());
        return new NetexValidatorsRunner(netexXMLParser, netexValidators);
    }

}

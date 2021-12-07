/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

package no.entur.antu.routes.validation;


import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.validator.id.IdVersion;
import no.entur.antu.validator.id.NetexIdExtractorHelper;
import no.entur.antu.validator.id.NetexIdValidator;
import no.entur.antu.validator.xpath.ValidationContext;
import no.entur.antu.xml.XMLParserUtil;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static no.entur.antu.Constants.DATASET_CODESPACE;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;


/**
 * Validate NeTEx Ids .
 */
@Component
public class ValidateIdsRouteBuilder extends BaseRouteBuilder {

    @Override
    public void configure() throws Exception {
        super.configure();

        from("direct:validateIds")
                .log(LoggingLevel.INFO, correlation() + "Validating Ids for NeTEx file ${header." + FILE_HANDLE + "}")
                .process(exchange -> {
                    byte[] content = exchange.getIn().getBody(byte[].class);
                    String codespace = exchange.getIn().getHeader(DATASET_CODESPACE, String.class);
                    String fileName = exchange.getIn().getHeader(NETEX_FILE_NAME, String.class);
                    XdmNode document = XMLParserUtil.parseFileToXdmNode(content);
                    ValidationContext validationContext = new ValidationContext(document, XMLParserUtil.getXPathCompiler(), codespace, fileName);

                    List<IdVersion> localIdList = NetexIdExtractorHelper.collectEntityIdentificators(validationContext, Set.of("Codespace"));
                    Set<IdVersion> localIds = new HashSet<>(localIdList);
                    List<IdVersion> localRefs = NetexIdExtractorHelper.collectEntityReferences(validationContext, null);

                    NetexIdValidator netexIdValidator = new NetexIdValidator();
                    exchange.getIn().setBody(netexIdValidator.validateIdStructure(validationContext, localIds));
                })
                .routeId("validate-ids");

    }

}

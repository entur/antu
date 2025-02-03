package no.entur.antu.validation.validator.xpath.rules;

import no.entur.antu.organisation.DefaultOrganisationRepository;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.organisation.OrganisationResource;
import org.entur.netex.validation.test.xpath.support.TestValidationContextBuilder;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashSet;
import java.util.List;

import static no.entur.antu.validation.validator.xpath.rules.ValidateAuthorityRef.CODE_AUTHORITY_REF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidateReferenceToOrgV3Test {

    private static final String NETEX_FRAGMENT =
        """
        <ServiceFrame xmlns="http://www.netex.org.uk/netex">
          <Network>
            <AuthorityRef ref="${AUTHORITY_ID}"/>
          </Network>
        </ServiceFrame>
        """;

    public static final String TEST_VALID_AUTHORITY_ID = "AVI:Authority:Avinor";
    public static final String TEST_INVALID_AUTHORITY_ID = "INVALID:Authority:1";

    private ValidateAuthorityRef validator;
    private final HashSet<String> mockedOrganisationIdsCache = new HashSet<>();

    @BeforeEach
    void setUp() {
        WebClient webClient = mock(WebClient.class);
        WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);

        when(webClient.mutate()).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        OrganisationResource organisationResource = new OrganisationResource(webClient);
        OrganisationRepository organisationV3Repository = new DefaultOrganisationRepository(organisationResource, mockedOrganisationIdsCache);

        validator = new ValidateAuthorityRef(organisationV3Repository);
        mockedOrganisationIdsCache.clear();
    }

    private XPathRuleValidationContext createXPathRuleValidationContext(String testAuthorityId) {
        String netexFragment = NETEX_FRAGMENT.replace("${AUTHORITY_ID}", testAuthorityId);
        return TestValidationContextBuilder.ofNetexFragment(netexFragment).build();
    }

    @Test
    public void validatorShouldReturnNoValidationErrorsIfOrganisationExists() {
        mockedOrganisationIdsCache.add(TEST_VALID_AUTHORITY_ID);
        XPathRuleValidationContext validationContext = createXPathRuleValidationContext(TEST_VALID_AUTHORITY_ID);
        List<ValidationIssue> validationIssues = validator.validate(validationContext);
        Assertions.assertEquals(0, validationIssues.size());
    }

    @Test
    public void validatorShouldReturnValidationErrorIfOrganisationDoesntExist() {
        XPathRuleValidationContext validationContext = createXPathRuleValidationContext(TEST_INVALID_AUTHORITY_ID);
        List<ValidationIssue> validationIssues = validator.validate(validationContext);
        Assertions.assertEquals(1, validationIssues.size());
        ValidationRule brokeRule = validationIssues.get(0).rule();
        Assertions.assertEquals(CODE_AUTHORITY_REF, brokeRule.code());
    }
}

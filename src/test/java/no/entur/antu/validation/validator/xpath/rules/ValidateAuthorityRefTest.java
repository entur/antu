package no.entur.antu.validation.validator.xpath.rules;

import static no.entur.antu.validation.validator.xpath.rules.ValidateAuthorityRef.INVALID_AUTHORITY_REF_RULE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import org.entur.netex.validation.test.xpath.support.TestValidationContextBuilder;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidateAuthorityRefTest {

  private ValidateAuthorityRef validator;
  private OrganisationAliasRepository organisationAliasRepository;

  private final String EXISTING_ORGANISATION_ALIAS = "OrganisationAlias:1";
  private final String NON_EXISTENT_ORGANISATION_ALIAS = "OrganisationAlias:2";

  @BeforeEach
  public void setUp() {
    this.organisationAliasRepository = mock(OrganisationAliasRepository.class);
    when(
      this.organisationAliasRepository.hasOrganisationWithAlias(
          EXISTING_ORGANISATION_ALIAS
        )
    )
      .thenReturn(true);
    when(
      this.organisationAliasRepository.hasOrganisationWithAlias(
          NON_EXISTENT_ORGANISATION_ALIAS
        )
    )
      .thenReturn(false);
    this.validator = new ValidateAuthorityRef(organisationAliasRepository);
  }

  @Test
  void testValidAuthorityRef() {
    XPathRuleValidationContext validationContext =
      validationContextWithAuthorityRef(EXISTING_ORGANISATION_ALIAS);
    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
    );
    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void testInvalidAuthorityRef() {
    XPathRuleValidationContext validationContext =
      validationContextWithAuthorityRef(NON_EXISTENT_ORGANISATION_ALIAS);
    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
    );
    Assertions.assertEquals(1, validationIssues.size());
    Assertions.assertEquals(
      INVALID_AUTHORITY_REF_RULE,
      validationIssues.get(0).rule()
    );
  }

  private static final String NETEX_FRAGMENT =
    """
            <ServiceFrame xmlns="http://www.netex.org.uk/netex" version="1">
              <Network>
                <AuthorityRef ref="%s"></AuthorityRef>
              </Network>
            </ServiceFrame>
        """;

  private XPathRuleValidationContext validationContextWithAuthorityRef(
    String authorityRef
  ) {
    String netexFragment = String.format(NETEX_FRAGMENT, authorityRef);
    return TestValidationContextBuilder.ofNetexFragment(netexFragment).build();
  }
}

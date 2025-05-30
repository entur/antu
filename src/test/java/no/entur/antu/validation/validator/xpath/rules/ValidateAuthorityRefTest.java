package no.entur.antu.validation.validator.xpath.rules;

import static no.entur.antu.validation.validator.xpath.rules.ValidateAuthorityRef.INVALID_AUTHORITY_REF_RULE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import no.entur.antu.organisation.SimpleOrganisationAliasRepository;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import org.entur.netex.validation.test.xpath.support.TestValidationContextBuilder;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidateAuthorityRefTest {

  private ValidateAuthorityRef validator;

  private final String EXISTING_ORGANISATION_ALIAS = "OrganisationAlias:1";
  private final String NON_EXISTENT_ORGANISATION_ALIAS = "OrganisationAlias:2";

  @BeforeEach
  public void setUp() {
    OrganisationAliasRepository organisationAliasRepository =
      new SimpleOrganisationAliasRepository(
        Set.of(EXISTING_ORGANISATION_ALIAS)
      );
    this.validator = new ValidateAuthorityRef(organisationAliasRepository);
  }

  @Test
  void testValidAuthorityRef() {
    XPathRuleValidationContext validationContext =
      validationContextWithAuthorityRef(EXISTING_ORGANISATION_ALIAS);
    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
    );
    assertTrue(validationIssues.isEmpty());
  }

  @Test
  void testInvalidAuthorityRef() {
    XPathRuleValidationContext validationContext =
      validationContextWithAuthorityRef(NON_EXISTENT_ORGANISATION_ALIAS);
    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
    );
    assertEquals(1, validationIssues.size());
    assertEquals(INVALID_AUTHORITY_REF_RULE, validationIssues.get(0).rule());
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

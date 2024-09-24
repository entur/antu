package no.entur.antu.organisation;

import static no.entur.antu.organisation.DefaultOrganisationRepository.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultOrganisationRepositoryTest {

  @Test
  void testIgnoreWhiteSpaceWhenParsingAuthorities() {
    Organisation organisation = new Organisation();
    organisation.id = "1";
    organisation.name = "Ruter";
    organisation.references =
      Map.of(
        REFERENCE_CODESPACE,
        "rut",
        REFERENCE_NETEX_AUTHORITY_IDS_WHITELIST,
        "RUT:Authority:1, RUT:Authority:2, RUT:Authority:3"
      );

    Map<String, Set<String>> authoritiesByOrganisation =
      DefaultOrganisationRepository.parseOrganisations(List.of(organisation));
    Assertions.assertFalse(authoritiesByOrganisation.isEmpty());
    String organisationKey = getOrganisationKey("rut");
    Assertions.assertTrue(
      authoritiesByOrganisation.containsKey(organisationKey)
    );
    Set<String> authorities = authoritiesByOrganisation.get(organisationKey);
    Assertions.assertEquals(
      Set.of("RUT:Authority:1", "RUT:Authority:2", "RUT:Authority:3"),
      authorities
    );
  }
}

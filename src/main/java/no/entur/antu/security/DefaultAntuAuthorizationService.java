package no.entur.antu.security;

import org.rutebanken.helper.organisation.authorization.AuthorizationService;

/**
 * Authorization for the REST endpoints, on the security context the resource server filter established.
 */
public class DefaultAntuAuthorizationService
  implements AntuAuthorizationService {

  private final AuthorizationService<String> authorizationService;

  public DefaultAntuAuthorizationService(
    AuthorizationService<String> authorizationService
  ) {
    this.authorizationService = authorizationService;
  }

  @Override
  public void verifyAdministratorPrivileges() {
    authorizationService.validateRouteDataAdmin();
  }

  @Override
  public void verifyRouteDataEditorPrivileges(String codespace) {
    authorizationService.validateEditRouteData(codespace);
  }
}

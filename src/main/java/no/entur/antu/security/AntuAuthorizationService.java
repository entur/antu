package no.entur.antu.security;

import org.apache.camel.Exchange;

/**
 *  Service that verifies the privileges of the API clients.
 */
public interface AntuAuthorizationService {
  /**
   * Verify that the user has full administrator privileges on route data.
   */
  void verifyAdministratorPrivileges(Exchange e);

  /**
   * Verify that the user can edit route data for the given provider codespace.
   */
  void verifyRouteDataEditorPrivileges(String codespace, Exchange e);
}

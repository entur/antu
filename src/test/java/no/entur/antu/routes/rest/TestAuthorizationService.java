package no.entur.antu.routes.rest;

import org.rutebanken.helper.organisation.authorization.AuthorizationService;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test implementation of the rutebanken authorization service that checks that the user is authenticated.
 */
class TestRutebankenAuthorizationService
  implements AuthorizationService<String> {

  @Override
  public boolean isRouteDataAdmin() {
    checkSecurityContext();
    return true;
  }

  @Override
  public boolean isOrganisationAdmin() {
    checkSecurityContext();
    return true;
  }

  @Override
  public boolean canViewAllOrganisationData() {
    checkSecurityContext();
    return true;
  }

  @Override
  public boolean canViewRouteData(String providerId) {
    checkSecurityContext();
    return true;
  }

  @Override
  public boolean canEditRouteData(String providerId) {
    checkSecurityContext();
    return true;
  }

  @Override
  public boolean canViewBlockData(String providerId) {
    checkSecurityContext();
    return true;
  }

  @Override
  public boolean canViewRoleAssignments() {
    checkSecurityContext();
    return true;
  }

  private void checkSecurityContext() {
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      throw new IllegalStateException("No security context");
    }
  }
}

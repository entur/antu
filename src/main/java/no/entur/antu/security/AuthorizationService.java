package no.entur.antu.security;

public interface AuthorizationService {
  void verifyAdministratorPrivileges();

  void verifyRouteDataEditorPrivileges(String codespace);
}

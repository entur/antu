package no.entur.antu.security;

public interface AntuAuthorizationService {
  void verifyAdministratorPrivileges();

  void verifyRouteDataEditorPrivileges(String codespace);
}

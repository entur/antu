package no.entur.antu.rest;

import no.entur.antu.cache.CacheAdmin;
import no.entur.antu.pipeline.OrganisationAliasCacheRefresher;
import no.entur.antu.pipeline.StopPlaceCacheRefresher;
import no.entur.antu.security.AntuAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator access to the shared caches.
 *
 * <p>No {@code produces} is declared: Spring MVC enforces it, and these endpoints are called by hand and
 * by scripts with whatever {@code Accept} header they happen to send.
 */
@RestController
@RequestMapping("/services/cache-admin")
public class CacheAdminController {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    CacheAdminController.class
  );

  private final AntuAuthorizationService antuAuthorizationService;
  private final CacheAdmin cacheAdmin;
  private final StopPlaceCacheRefresher stopPlaceCacheRefresher;
  private final OrganisationAliasCacheRefresher organisationAliasCacheRefresher;

  public CacheAdminController(
    AntuAuthorizationService antuAuthorizationService,
    CacheAdmin cacheAdmin,
    StopPlaceCacheRefresher stopPlaceCacheRefresher,
    OrganisationAliasCacheRefresher organisationAliasCacheRefresher
  ) {
    this.antuAuthorizationService = antuAuthorizationService;
    this.cacheAdmin = cacheAdmin;
    this.stopPlaceCacheRefresher = stopPlaceCacheRefresher;
    this.organisationAliasCacheRefresher = organisationAliasCacheRefresher;
  }

  @PostMapping("/clear-cache")
  public void clearCache() {
    antuAuthorizationService.verifyAdministratorPrivileges();
    LOGGER.info("Clear cache");
    cacheAdmin.clear();
  }

  /**
   * @return how many keys the pattern matched, which on a shared Redis is the only confirmation the
   *         caller gets that it deleted what it meant to.
   */
  @PostMapping("/delete-by-pattern/{pattern}")
  public String deleteKeysByPattern(@PathVariable String pattern) {
    antuAuthorizationService.verifyAdministratorPrivileges();
    long deleted = cacheAdmin.deleteKeysByPattern(pattern);
    String loggedPattern = LogSafe.of(pattern);
    LOGGER.info("Deleted {} cache keys matching {}", deleted, loggedPattern);
    return Long.toString(deleted);
  }

  @GetMapping("/dump-keys")
  public String dumpKeys() {
    antuAuthorizationService.verifyAdministratorPrivileges();
    LOGGER.info("Dump keys");
    return cacheAdmin.dumpKeys();
  }

  @PostMapping("/refresh-stop-cache")
  public void refreshStopCache() {
    antuAuthorizationService.verifyAdministratorPrivileges();
    stopPlaceCacheRefresher.forceRefresh();
  }

  @PostMapping("/refresh-organisation-cache")
  public void refreshOrganisationCache() {
    antuAuthorizationService.verifyAdministratorPrivileges();
    organisationAliasCacheRefresher.enqueueRefresh();
  }
}

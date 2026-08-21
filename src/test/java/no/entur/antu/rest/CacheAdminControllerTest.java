package no.entur.antu.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import no.entur.antu.cache.CacheAdmin;
import no.entur.antu.pipeline.OrganisationAliasCacheRefresher;
import no.entur.antu.pipeline.StopPlaceCacheRefresher;
import no.entur.antu.security.AntuAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CacheAdminControllerTest {

  private AntuAuthorizationService authorizationService;
  private CacheAdmin cacheAdmin;
  private StopPlaceCacheRefresher stopPlaceCacheRefresher;
  private OrganisationAliasCacheRefresher organisationAliasCacheRefresher;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    authorizationService = mock(AntuAuthorizationService.class);
    cacheAdmin = mock(CacheAdmin.class);
    stopPlaceCacheRefresher = mock(StopPlaceCacheRefresher.class);
    organisationAliasCacheRefresher =
      mock(OrganisationAliasCacheRefresher.class);
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(
          new CacheAdminController(
            authorizationService,
            cacheAdmin,
            stopPlaceCacheRefresher,
            organisationAliasCacheRefresher
          )
        )
        .build();
  }

  /**
   * A POST with no body and no Content-Type has to be accepted: that is how these endpoints are called.
   */
  @Test
  void clearCacheAcceptsAPostWithoutABody() throws Exception {
    mockMvc
      .perform(post("/services/cache-admin/clear-cache"))
      .andExpect(status().isOk());

    verify(cacheAdmin).clear();
  }

  /**
   * No {@code produces} is declared on the controller, so a caller asking for JSON is not refused.
   */
  @Test
  void anyAcceptHeaderIsAccepted() throws Exception {
    mockMvc
      .perform(
        post("/services/cache-admin/clear-cache")
          .header("Accept", "application/json")
      )
      .andExpect(status().isOk());
  }

  @Test
  void keysCanBeDeletedByPattern() throws Exception {
    when(cacheAdmin.deleteKeysByPattern("TEMPORARY_FILE_report-1"))
      .thenReturn(17L);

    mockMvc
      .perform(
        post("/services/cache-admin/delete-by-pattern/TEMPORARY_FILE_report-1")
      )
      .andExpect(status().isOk())
      // The count is the caller's only confirmation that the glob matched what they meant.
      .andExpect(content().string("17"));

    verify(cacheAdmin).deleteKeysByPattern("TEMPORARY_FILE_report-1");
  }

  @Test
  void keysCanBeDumped() throws Exception {
    when(cacheAdmin.dumpKeys()).thenReturn("key1\nkey2");

    mockMvc
      .perform(get("/services/cache-admin/dump-keys"))
      .andExpect(status().isOk())
      .andExpect(content().string("key1\nkey2"));
  }

  /**
   * The refresh endpoints only queue the work: the refresh is expensive and belongs on whichever pod
   * takes the job, not on the one serving the request. The stop place one forces it, because an operator
   * asking for a refresh has a reason the recorded dataset version does not know about.
   */
  @Test
  void refreshEndpointsQueueAJob() throws Exception {
    mockMvc
      .perform(post("/services/cache-admin/refresh-stop-cache"))
      .andExpect(status().isOk());
    mockMvc
      .perform(post("/services/cache-admin/refresh-organisation-cache"))
      .andExpect(status().isOk());

    verify(stopPlaceCacheRefresher).forceRefresh();
    verify(organisationAliasCacheRefresher).enqueueRefresh();
  }

  @Test
  void everyEndpointRequiresAdministratorPrivileges() throws Exception {
    mockMvc.perform(post("/services/cache-admin/clear-cache"));
    mockMvc.perform(post("/services/cache-admin/delete-by-pattern/x"));
    mockMvc.perform(get("/services/cache-admin/dump-keys"));
    mockMvc.perform(post("/services/cache-admin/refresh-stop-cache"));
    mockMvc.perform(post("/services/cache-admin/refresh-organisation-cache"));

    verify(authorizationService, times(5)).verifyAdministratorPrivileges();
  }
}

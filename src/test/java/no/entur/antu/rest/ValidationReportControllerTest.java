package no.entur.antu.rest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import no.entur.antu.security.AntuAuthorizationService;
import no.entur.antu.services.AntuBlobStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ValidationReportControllerTest {

  private static final String REPORT_JSON =
    "{\"validationReportId\":\"test-report-123\",\"codespace\":\"TST\"}";
  private static final String REPORT_PATH =
    "/services/validation-report/TST/test-report-123";

  private AntuAuthorizationService authorizationService;
  private AntuBlobStoreService blobStoreService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    authorizationService = mock(AntuAuthorizationService.class);
    blobStoreService = mock(AntuBlobStoreService.class);
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(
          new ValidationReportController(authorizationService, blobStoreService)
        )
        .build();
  }

  @Test
  void theReportIsReturnedAsStored() throws Exception {
    when(
      blobStoreService.getBlob(
        "reports/TST/validation-report-test-report-123.json"
      )
    )
      .thenReturn(
        new ByteArrayInputStream(REPORT_JSON.getBytes(StandardCharsets.UTF_8))
      );

    mockMvc
      .perform(get(REPORT_PATH))
      .andExpect(status().isOk())
      .andExpect(content().string(REPORT_JSON));
  }

  /**
   * The endpoint used to claim gzip over a plain JSON body. Compression is negotiated by the container
   * now, so the controller must not assert an encoding it does not apply.
   */
  @Test
  void theResponseDoesNotClaimAnEncodingItDoesNotApply() throws Exception {
    when(blobStoreService.getBlob(anyString()))
      .thenReturn(new ByteArrayInputStream(REPORT_JSON.getBytes()));

    mockMvc
      .perform(get(REPORT_PATH))
      .andExpect(header().doesNotExist(HttpHeaders.CONTENT_ENCODING));
  }

  @Test
  void anUnknownReportIs404() throws Exception {
    when(blobStoreService.getBlob(anyString())).thenReturn(null);

    mockMvc.perform(get(REPORT_PATH)).andExpect(status().isNotFound());
  }

  /**
   * The codespace in the path is what the caller has to hold edit rights on, not just any codespace.
   */
  @Test
  void thePathCodespaceIsTheOneAuthorized() throws Exception {
    when(blobStoreService.getBlob(anyString()))
      .thenReturn(new ByteArrayInputStream(REPORT_JSON.getBytes()));

    mockMvc.perform(get(REPORT_PATH));

    verify(authorizationService).verifyRouteDataEditorPrivileges("TST");
  }

  @Test
  void anUnauthorizedCallerNeverReachesTheBucket() {
    doThrow(new AccessDeniedException("nope"))
      .when(authorizationService)
      .verifyRouteDataEditorPrivileges(anyString());

    org.junit.jupiter.api.Assertions.assertThrows(
      Exception.class,
      () -> mockMvc.perform(get(REPORT_PATH))
    );
    org.mockito.Mockito.verifyNoInteractions(blobStoreService);
  }
}

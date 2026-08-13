package no.entur.antu.rest;

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_REPORTS;
import static no.entur.antu.Constants.VALIDATION_REPORT_PREFIX;
import static no.entur.antu.Constants.VALIDATION_REPORT_SUFFIX;

import java.io.InputStream;
import no.entur.antu.security.AntuAuthorizationService;
import no.entur.antu.services.AntuBlobStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the published validation reports, as stored: plain JSON, streamed straight from the bucket.
 *
 * <p>Deliberately sets no {@code Content-Encoding}. Compression is negotiated by the servlet container
 * through {@code server.compression}, which gzips only when the caller asks for it and sets the header
 * itself, so the header is always true.
 */
@RestController
@RequestMapping("/services/validation-report")
public class ValidationReportController {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ValidationReportController.class
  );

  private final AntuAuthorizationService antuAuthorizationService;
  private final AntuBlobStoreService antuBlobStoreService;

  public ValidationReportController(
    AntuAuthorizationService antuAuthorizationService,
    AntuBlobStoreService antuBlobStoreService
  ) {
    this.antuAuthorizationService = antuAuthorizationService;
    this.antuBlobStoreService = antuBlobStoreService;
  }

  // No produces: Spring MVC enforces it at handler matching, so a caller sending anything other than
  // application/json would get an empty 406 before the method runs. The content type is set on the
  // response instead. The Camel route this replaced needed serverRequestValidation=false for the same
  // reason.
  @GetMapping("/{codespace}/{id}")
  public ResponseEntity<InputStreamResource> validationReport(
    @PathVariable String codespace,
    @PathVariable String id
  ) {
    antuAuthorizationService.verifyRouteDataEditorPrivileges(codespace);

    String fileHandle =
      BLOBSTORE_PATH_ANTU_REPORTS +
      codespace +
      VALIDATION_REPORT_PREFIX +
      id +
      VALIDATION_REPORT_SUFFIX;
    String loggedFileHandle = LogSafe.of(fileHandle);
    LOGGER.info("Downloading NeTEx validation report {}", loggedFileHandle);

    InputStream report = antuBlobStoreService.getBlob(fileHandle);
    if (report == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity
      .ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body(new InputStreamResource(report));
  }
}

/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

package no.entur.antu.config;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.rutebanken.helper.gcp.BlobStoreHelper;
import org.rutebanken.helper.gcp.repository.GcsBlobStoreRepository;
import org.rutebanken.helper.storage.repository.BlobStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;

@Configuration
@Profile("gcs-blobstore")
public class GcsStorageConfig {

  private static final Logger LOG = LoggerFactory.getLogger(
    GcsStorageConfig.class
  );

  @Value("${blobstore.gcs.credential.path:#{null}}")
  private String credentialPath;

  @Value("${blobstore.gcs.project.id}")
  private String projectId;

  @Bean
  public Storage storage() {
    String emulatorHost = System.getenv("STORAGE_EMULATOR_HOST");
    if (emulatorHost != null && !emulatorHost.isBlank()) {
      LOG.info(
        "Using GCS emulator at {} for project {}",
        emulatorHost,
        projectId
      );
      return StorageOptions
        .newBuilder()
        .setHost(emulatorHost)
        .setProjectId(projectId)
        .setCredentials(NoCredentials.getInstance())
        .build()
        .getService();
    }
    if (credentialPath == null || credentialPath.isEmpty()) {
      return BlobStoreHelper.getStorage(projectId);
    } else {
      return BlobStoreHelper.getStorage(credentialPath, projectId);
    }
  }

  @Bean
  @Scope("prototype")
  BlobStoreRepository blobStoreRepository(Storage storage) {
    return new GcsBlobStoreRepository(storage);
  }
}

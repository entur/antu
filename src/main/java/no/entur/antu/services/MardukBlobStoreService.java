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

package no.entur.antu.services;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import java.util.Optional;
import javax.annotation.Nullable;
import org.rutebanken.helper.storage.repository.BlobStoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Operations on blobs in the main antu bucket.
 */
@Service
public class MardukBlobStoreService extends AbstractBlobStoreService {

  private static final String UNVERSIONED = "unversioned";

  private final String containerName;
  private final Optional<Storage> storage;

  public MardukBlobStoreService(
    @Value("${blobstore.gcs.marduk.container.name}") String containerName,
    BlobStoreRepository repository,
    Optional<Storage> storage
  ) {
    super(containerName, repository);
    this.containerName = containerName;
    this.storage = storage;
  }

  /**
   * A version for a blob that changes when the blob does, or null if the blob is not there. On GCS that is
   * the generation; a store that keeps no metadata answers with a constant, which is enough to load a
   * dataset once but not to notice it changing.
   *
   * <p>Reads metadata only. {@code getBlob} is not an option for something that is polled:
   * {@code BlobStoreHelper.getBlobInputStream} downloads the whole object twice, once to check its MD5
   * and once to hand it over, and the stop place dataset is tens of megabytes.
   */
  @Nullable
  public String blobVersion(String name) {
    Storage gcs = storage.orElse(null);
    if (gcs == null) {
      return existBlob(name) ? UNVERSIONED : null;
    }
    Blob blob = gcs.get(
      BlobId.of(containerName, name),
      Storage.BlobGetOption.fields(Storage.BlobField.GENERATION)
    );
    if (blob == null || blob.getGeneration() == null) {
      return null;
    }
    return Long.toString(blob.getGeneration());
  }
}

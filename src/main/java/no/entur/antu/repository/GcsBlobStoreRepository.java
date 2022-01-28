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

package no.entur.antu.repository;

import com.google.cloud.storage.Storage;
import org.rutebanken.helper.gcp.BlobStoreHelper;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.io.InputStream;

/**
 * Blob store no.entur.antu.repository targeting Google Cloud Storage.
 */
@Repository
@Profile("gcs-blobstore")
@Scope("prototype")
public class GcsBlobStoreRepository implements BlobStoreRepository {

    private final Storage storage;

    private String containerName;

    public GcsBlobStoreRepository(Storage storage) {
        this.storage = storage;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @Override
    public boolean existBlob(String objectName) {
        return BlobStoreHelper.existBlob(storage, containerName, objectName);
    }

    @Override
    public InputStream getBlob(String name) {
        return BlobStoreHelper.getBlob(storage, containerName, name);
    }

    @Override
    public void uploadBlob(String name, InputStream inputStream) {
        BlobStoreHelper.uploadBlobWithRetry(storage, containerName, name, inputStream, false);
    }

}

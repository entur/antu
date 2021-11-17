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

import no.entur.antu.Constants;
import no.entur.antu.repository.BlobStoreRepository;
import org.apache.camel.Exchange;
import org.apache.camel.Header;

import java.io.InputStream;

public abstract class AbstractBlobStoreService {

    protected final BlobStoreRepository repository;

    protected AbstractBlobStoreService(String containerName, BlobStoreRepository repository) {
        this.repository = repository;
        this.repository.setContainerName(containerName);
    }

    public InputStream getBlob(@Header(value = Constants.FILE_HANDLE) String name, Exchange exchange) {
        return repository.getBlob(name);
    }


    public void uploadBlob(@Header(value = Constants.FILE_HANDLE) String name,
                           @Header(value = Constants.BLOBSTORE_MAKE_BLOB_PUBLIC) boolean makePublic, InputStream inputStream, Exchange exchange) {
        repository.uploadBlob(name, inputStream, makePublic);
    }


}

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

/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.entur.antu.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simple file-based blob store no.entur.antu.repository for testing purpose.
 */
@Component
@Profile("local-disk-blobstore")
@Scope("prototype")
public class LocalDiskBlobStoreRepository implements BlobStoreRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDiskBlobStoreRepository.class);

    @Value("${blobstore.local.folder:files/blob}")
    private String baseFolder;

    private String containerName;

    private String getContainerFolder() {
        return baseFolder + File.separator + containerName;
    }

    @Override
    public InputStream getBlob(String objectName) {
        LOGGER.debug("get blob called in local-disk blob store on {}", objectName);
        Path path = Paths.get(getContainerFolder()).resolve(objectName);
        if (!path.toFile().exists()) {
            LOGGER.debug("getBlob(): File not found in local-disk blob store: {} ", path);
            return null;
        }
        LOGGER.debug("getBlob(): File found in local-disk blob store: {} ", path);
        try {
            // converted as ByteArrayInputStream so that Camel stream cache can reopen it
            // since ByteArrayInputStream.close() does nothing
            return new ByteArrayInputStream(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new no.entur.antu.exception.AntuException(e);
        }
    }

    @Override
    public void uploadBlob(String objectName, InputStream inputStream, boolean makePublic) {
        LOGGER.debug("Upload blob called in local-disk blob store on {}", objectName);
        try {
            Path localPath = Paths.get(objectName);
            Path parentDirectory = localPath.getParent();
            Path folder = parentDirectory == null ? Paths.get(getContainerFolder()) : Paths.get(getContainerFolder()).resolve(parentDirectory);
            Files.createDirectories(folder);

            Path fullPath = Paths.get(getContainerFolder()).resolve(localPath);
            Files.deleteIfExists(fullPath);

            Files.copy(inputStream, fullPath);
        } catch (IOException e) {
            throw new no.entur.antu.exception.AntuException(e);
        }
    }


    @Override
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

}

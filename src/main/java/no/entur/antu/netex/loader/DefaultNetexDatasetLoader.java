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

package no.entur.antu.netex.loader;

import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.AuthorityIdValidator;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DefaultNetexDatasetLoader implements NetexDatasetLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorityIdValidator.class);


    protected final NetexParser netexParser;

    public DefaultNetexDatasetLoader() {
        this.netexParser = new NetexParser();
    }

    @Override
    public void load(InputStream timetableDataset, NetexEntitiesIndex netexEntitiesIndex) {
        try (ZipInputStream zipInputStream = new ZipInputStream(timetableDataset)) {
            parseDataset(zipInputStream, netexEntitiesIndex);
        } catch (IOException e) {
            throw new AntuException("Error while parsing the NeTEx timetable dataset", e);
        }
    }


    /**
     * Parse a zip file containing a NeTEx archive.
     *
     * @param zipInputStream     a stream on a NeTEx zip archive.
     * @param netexEntitiesIndex the NeTEx dataset index to be updated with the content of the NeTEx archive.
     * @throws IOException if the zip file cannot be read.
     */
    protected void parseDataset(ZipInputStream zipInputStream, NetexEntitiesIndex netexEntitiesIndex) throws IOException {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            if (zipEntry.getName().endsWith(".xml")) {
                byte[] allBytes = zipInputStream.readAllBytes();
                netexParser.parse(new ByteArrayInputStream(allBytes), netexEntitiesIndex);
            } else {
                LOGGER.info("Ignoring non-xml file {}", zipEntry.getName());
            }
            zipEntry = zipInputStream.getNextEntry();
        }

    }
}

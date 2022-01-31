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

package no.entur.antu.routes.redis;

import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.services.AntuBlobStoreService;
import org.apache.camel.LoggingLevel;
import org.apache.camel.converter.crypto.CryptoDataFormat;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static no.entur.antu.Constants.ENCRYPTION_KEY;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID;


@Component
public class RedisMemoryStoreRoute extends BaseRouteBuilder {

    private final CryptoDataFormat cryptoFormat;

    public RedisMemoryStoreRoute(AntuBlobStoreService antuBlobStoreService) {
        this.cryptoFormat = new CryptoDataFormat("DES", null);
        cryptoFormat.setShouldAppendHMAC(false);
    }


    @Override
    public void configure() {

        from("direct:downloadBlobFromMemoryStore")
                .process(exchange -> System.out.println("aa"))
                .setBody(header(ENCRYPTION_KEY))
                .unmarshal().base64()
                .process(exchange -> {
                    byte[] encodedKey = exchange.getIn().getBody(byte[].class);
                    SecretKey desKey = new SecretKeySpec(encodedKey, "DES");
                    exchange.getIn().setHeader(CryptoDataFormat.KEY, desKey);
                })
                .bean("redisTemporaryFileRepository", "download(${header." + VALIDATION_REPORT_ID + "},${header." + NETEX_FILE_NAME + "})")
                .unmarshal(cryptoFormat)
                .log(LoggingLevel.INFO, correlation() + "Returning from fetching file ${header." + NETEX_FILE_NAME + "} from memory store.")
                .routeId("memory-store-download");

        from("direct:uploadBlobToMemoryStore")
                .process(exchange -> System.out.println("aa"))
                .setHeader("saveBody", body())
                .setBody(header(ENCRYPTION_KEY))
                .process(exchange -> System.out.println("aa"))
                .unmarshal().base64()
                .process(exchange -> System.out.println("aa"))

                .process(exchange -> {
                    byte[] encodedKey = exchange.getIn().getBody(byte[].class);
                    SecretKey desKey = new SecretKeySpec(encodedKey, "DES");
                    exchange.getIn().setHeader(CryptoDataFormat.KEY, desKey);
                })
                .setBody(header("saveBody"))
                .process(exchange -> System.out.println("aa"))
                .marshal(cryptoFormat)
                .process(exchange -> System.out.println("aa"))
                .bean("redisTemporaryFileRepository", "upload(${header." + VALIDATION_REPORT_ID + "},${header." + NETEX_FILE_NAME + "}, ${body} )")
                .setBody(constant(""))
                .log(LoggingLevel.INFO, correlation() + "Stored file ${header." + NETEX_FILE_NAME + "} in memory store.")
                .routeId("memory-store-upload");
    }
}

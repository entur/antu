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

package no.entur.antu.routes.memorystore;

import no.entur.antu.Constants;
import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.converter.crypto.CryptoDataFormat;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

import static no.entur.antu.Constants.ENCRYPTION_KEY;


/**
 * Encrypt and decrypt the message body with a symmetric key algorithm.
 * This is intended to secure the storage of temporary files in the memory store.
 */
@Component
public class EncryptionRoute extends BaseRouteBuilder {

    public static final String PROP_BACKUP_MESSAGE_BODY = "EnturBackupMessageBody";
    private final CryptoDataFormat cryptoFormat;

    public EncryptionRoute() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[12];
        random.nextBytes(iv);
        GCMParameterSpec paramSpec = new GCMParameterSpec(128, iv);
        CryptoDataFormat theCryptoFormat = new CryptoDataFormat("AES/GCM/NoPadding", null);
        theCryptoFormat.setAlgorithmParameterSpec(paramSpec);
        theCryptoFormat.setShouldAppendHMAC(false);
        this.cryptoFormat = theCryptoFormat;
    }

    @Override
    public void configure() {

        from("direct:createEncryptionKey")
                .log(LoggingLevel.INFO, correlation() + "Create encryption key")
                .process(exchange -> {
                    KeyGenerator generator = KeyGenerator.getInstance("AES");
                    generator.init(128);
                    byte[] encodedKey = generator.generateKey().getEncoded();
                    exchange.getIn().setBody(encodedKey);
                })
                .marshal().base64()
                .setHeader(Constants.ENCRYPTION_KEY, body())
                .log(LoggingLevel.INFO, correlation() + "Created encryption key")
                .routeId("create-encryption-key");

        from("direct:encryptPayload")
                .log(LoggingLevel.INFO, correlation() + "Encrypt payload")
                .to("direct:setEncryptionKey")
                .marshal(cryptoFormat)
                .log(LoggingLevel.INFO, correlation() + "Encrypted payload")
                .routeId("encrypt-payload");

        from("direct:decryptPayload")
                .log(LoggingLevel.INFO, correlation() + "Decrypt payload")
                .to("direct:setEncryptionKey")
                .unmarshal(cryptoFormat)
                .log(LoggingLevel.INFO, correlation() + "Decrypted payload")
                .routeId("decrypt-payload");

        from("direct:setEncryptionKey")
                .filter(header(CryptoDataFormat.KEY).isNull())
                .setProperty(PROP_BACKUP_MESSAGE_BODY, body())
                .setBody(header(ENCRYPTION_KEY))
                .unmarshal().base64()
                .process(exchange -> {
                    byte[] encodedKey = exchange.getIn().getBody(byte[].class);
                    SecretKey key = new SecretKeySpec(encodedKey, "AES");
                    exchange.getIn().setHeader(CryptoDataFormat.KEY, key);
                })
                .setBody(exchangeProperty(PROP_BACKUP_MESSAGE_BODY))
                .removeProperty(PROP_BACKUP_MESSAGE_BODY)
                // end filter
                .end()
                .routeId("set-encryption-key");


    }
}

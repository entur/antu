package no.entur.antu.routes.redis;

import no.entur.antu.exception.AntuException;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class RedisTemporaryFileRepository {

    private static final String TEMPORARY_FILE_KEY_PREFIX = "TEMPORARY_FILE_";

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTemporaryFileRepository.class);

    private final RedissonClient redissonClient;

    public RedisTemporaryFileRepository(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void upload(String validationReportId, String fileName, byte[] content) {
        RBucket<Object> temporaryFile = redissonClient.getBucket(getTemporaryFileKey(validationReportId, fileName));
        temporaryFile.expire(1, TimeUnit.HOURS);
        temporaryFile.set(content);
    }


    public byte[] download(String validationReportId, String fileName) {
        RBucket<Object> temporaryFile = redissonClient.getBucket(getTemporaryFileKey(validationReportId, fileName));
        if (temporaryFile.isExists()) {
            return (byte[]) temporaryFile.get();
        } else {
            throw new AntuException("File " + fileName + "for validation report " + validationReportId + " not found in the memory store");
        }

    }

    private String getTemporaryFileKey(String validationReportId, String fileName) {
        return TEMPORARY_FILE_KEY_PREFIX + validationReportId + '_' + fileName;
    }

}

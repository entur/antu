package no.entur.antu.cache;

import org.redisson.api.RedissonClient;

public class RedissonCacheAdmin implements CacheAdmin {

    private final RedissonClient redissonClient;

    public RedissonCacheAdmin(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void clear() {
        redissonClient.getKeys().flushdbParallelAsync();
    }
}

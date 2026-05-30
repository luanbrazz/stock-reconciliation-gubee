package com.gubee.stock.infrastructure.redis;

import com.gubee.stock.application.port.out.StockLockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStockLockAdapter implements StockLockPort {

    private static final String LOCK_PREFIX = "stock:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final int MAX_RETRIES = 20;
    private static final long RETRY_DELAY_MS = 100;

    private final StringRedisTemplate redisTemplate;

    @Override
    public String acquire(String accountId, String sku) {
        String key = lockKey(accountId, sku);
        String token = UUID.randomUUID().toString();

        for (int i = 0; i < MAX_RETRIES; i++) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, token, LOCK_TTL);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Lock acquired key={}", key);
                return token;
            }

            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for lock", ex);
            }
        }

        throw new IllegalStateException(
                String.format("Could not acquire lock for account=%s sku=%s after %d retries",
                        accountId, sku, MAX_RETRIES));
    }

    @Override
    public void release(String accountId, String sku, String lockToken) {
        String key = lockKey(accountId, sku);
        String current = redisTemplate.opsForValue().get(key);

        if (lockToken.equals(current)) {
            redisTemplate.delete(key);
            log.debug("Lock released key={}", key);
        } else {
            log.warn("Lock token mismatch for key={} — may have expired", key);
        }
    }

    private String lockKey(String accountId, String sku) {
        return LOCK_PREFIX + accountId + ":" + sku;
    }
}
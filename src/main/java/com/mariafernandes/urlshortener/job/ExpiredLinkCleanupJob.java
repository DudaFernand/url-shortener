package com.mariafernandes.urlshortener.job;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ExpiredLinkCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredLinkCleanupJob.class);

    private final ShortUrlRepository repository;
    private final CacheManager cacheManager;

    public ExpiredLinkCleanupJob(ShortUrlRepository repository,
                                 @Autowired(required = false) CacheManager cacheManager) {
        this.repository = repository;
        this.cacheManager = cacheManager;
    }

    @Scheduled(cron = "${app.link.cleanup-cron}")
    public void cleanupExpiredLinks() {
        LocalDateTime now = LocalDateTime.now();
        List<ShortUrl> expired = repository.findByExpiresAtBefore(now);

        if (cacheManager != null) {
            var cache = cacheManager.getCache("shortUrls");
            if (cache != null) {
                expired.forEach(link -> cache.evict(link.getCode()));
            }
        }

        long deleted = repository.deleteByExpiresAtBefore(now);
        if (deleted > 0) {
            log.info("Cleanup: {} links expirados removidos", deleted);
        }
    }
}

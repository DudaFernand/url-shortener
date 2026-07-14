package com.mariafernandes.urlshortener.job;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiredLinkCleanupJobTest {

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private ExpiredLinkCleanupJob job;

    @Test
    void cleanupExpiredLinks_deveEvictarCacheEDeletarLinks() {
        ShortUrl expired = new ShortUrl("abc123", "https://google.com", null);
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(repository.findByExpiresAtBefore(any(LocalDateTime.class)))
            .thenReturn(List.of(expired));
        when(cacheManager.getCache("shortUrls")).thenReturn(cache);
        when(repository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(1L);

        job.cleanupExpiredLinks();

        verify(cache).evict("abc123");
        verify(repository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }

    @Test
    void cleanupExpiredLinks_deveDeletarSemCacheQuandoCacheManagerNulo() {
        ExpiredLinkCleanupJob jobWithoutCache = new ExpiredLinkCleanupJob(repository, null);
        when(repository.findByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(List.of());
        when(repository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(0L);

        jobWithoutCache.cleanupExpiredLinks();

        verify(repository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}

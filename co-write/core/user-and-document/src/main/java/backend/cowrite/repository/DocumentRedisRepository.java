package backend.cowrite.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class DocumentRedisRepository {

    private final StringRedisTemplate redisTemplate;

    //document::count::{documentId}
    private static final String DOCUMENT_COUNT_FORMAT = "document::count::%s";
    private static final Duration COUNT_TTL = Duration.ofHours(24); // ttl 24시간

    public void generateOrUpdateDocumentUpdatedTime(Long documentId) {
        redisTemplate.opsForValue().increment(generateDocumentCountKey(documentId));
        redisTemplate.expire(generateDocumentCountKey(documentId),COUNT_TTL);
    }

    public Long getDocumentUpdatedTimes(Long documentId) {
        String times = redisTemplate.opsForValue().get(generateDocumentCountKey(documentId));
        return times == null ? 0L : Long.parseLong(times);
    }

    public void resetDocumentUpdatedTimes(Long documentId) {
        redisTemplate.delete(generateDocumentCountKey(documentId));
    }

    private String generateDocumentCountKey(Long documentId) {
        return DOCUMENT_COUNT_FORMAT.formatted(documentId);
    }
}

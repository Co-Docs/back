package backend.cowrite.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class DocumentRedisRepository {
    private final StringRedisTemplate redisTemplate;

    // document::reading::{documentId}
    private static final String KEY_FORMAT = "document::reading::%s";

    public void createOrUpdate(Long documentId, String content, Duration duration) {
        redisTemplate.opsForValue().set(generateKey(documentId),content,duration);
    }

    public String read(Long documentId) {
        return redisTemplate.opsForValue().get(generateKey(documentId));
    }

    public void delete(Long articleId) {
        redisTemplate.delete(generateKey(articleId));
    }


    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }

}

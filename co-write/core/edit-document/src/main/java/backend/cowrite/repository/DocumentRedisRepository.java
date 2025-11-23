package backend.cowrite.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class DocumentRedisRepository {
    private final StringRedisTemplate redisTemplate;

    // document::content::{documentId} // document::version::{documentId} // document::operation::{documentId}
    private static final String CONTENT_KEY_FORMAT = "document::content::%s";
    private static final String VERSION_KEY_FORMAT = "document::version::%s";
    private static final String OPERATION_KEY_FORMAT = "document::operation::%s";
    //document::count::{documentId}
    private static final String DOCUMENT_COUNT_FORMAT = "document::count::%s";

    private static final int MAX_OPS = 200;                // 최근 N개만 유지
    private static final int NEXT_VERSION_COUNT = 1;


    public void createOrUpdateContent(Long documentId, String content, Duration duration) {
        redisTemplate.opsForValue().set(generateContentKey(documentId), content, duration);
    }

    public void createOrUpdateVersion(Long documentId, String version) {
        redisTemplate.opsForValue().set(generateVersionKey(documentId), version);
    }

    public void createOrUpdateOperation(Long documentId, String operation, Long version) {
        redisTemplate.opsForZSet().add(generateOperationKey(documentId), operation, version);
        removeOldOperations(documentId);
    }

    public Optional<String> readContent(Long documentId) {
        return Optional.of(
                redisTemplate.opsForValue().get(generateContentKey(documentId))
        );
    }

    public Optional<Long> readVersion(Long documentId) {
        String version = redisTemplate.opsForValue().get(generateVersionKey(documentId));
        return Optional.of(Long.parseLong(version));
    }

    public List<String> readOperation(Long documentId, Long baseVersion, Long newVersion) {
        Set<String> operations = redisTemplate.opsForZSet().rangeByScore(generateOperationKey(documentId), baseVersion+NEXT_VERSION_COUNT, newVersion);
        if(operations == null || operations.isEmpty()) {return List.of();}
        return new ArrayList<>(operations);
    }

    public void generateOrUpdateDocumentUpdatedTime(Long documentId, Duration ttl) {
        redisTemplate.opsForValue().increment(generateDocumentCountKey(documentId));
        redisTemplate.expire(generateDocumentCountKey(documentId), ttl);
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

    private Long getSavedOperationSize(Long documentId) {
        return redisTemplate.opsForZSet().zCard(generateOperationKey(documentId));
    }

    private void removeOldOperations(Long documentId) {
        long savedOperationSize = getSavedOperationSize(documentId);
        if (savedOperationSize > MAX_OPS) {
            long removeIndex = savedOperationSize - MAX_OPS;
            redisTemplate.opsForZSet().removeRange(generateOperationKey(documentId), 0, removeIndex);
        }
    }

    private String generateContentKey(Long documentId) {
        return CONTENT_KEY_FORMAT.formatted(documentId);
    }

    private String generateVersionKey(Long documentId) {
        return VERSION_KEY_FORMAT.formatted(documentId);
    }

    private String generateOperationKey(Long documentId) {
        return OPERATION_KEY_FORMAT.formatted(documentId);
    }
}

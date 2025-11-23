package backend.cowrite.service;

import backend.cowrite.publisher.DocumentSavePublisher;
import backend.cowrite.repository.DocumentRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentUpdateCounter {
    private final DocumentSavePublisher documentSavePublisher;
    private final DocumentRedisRepository documentRedisRepository;
    private final static Long THRESHOLD_COUNT = 10L;
    private final static Duration UPDATE_COUNT_TTL = Duration.ofHours(12L);

    public void update(Long documentId, String editedContent) {
        documentRedisRepository.generateOrUpdateDocumentUpdatedTime(documentId, UPDATE_COUNT_TTL);
        if (isUpdatedTimeReachedThreshold(documentId)) {
            publishSaveEvent(documentId, editedContent);
            reset(documentId);
        }
    }

    private void publishSaveEvent(Long documentId, String editedContent) {
        documentSavePublisher.saveDocument(documentId, editedContent);
    }

    private boolean isUpdatedTimeReachedThreshold(Long documentId) {
        Long documentUpdatedTimes = documentRedisRepository.getDocumentUpdatedTimes(documentId);
        return THRESHOLD_COUNT <= documentUpdatedTimes;
    }

    private void reset(Long documentId) {
        documentRedisRepository.resetDocumentUpdatedTimes(documentId);
    }
}

package backend.cowrite.service;

import backend.cowrite.publisher.DocumentSavePublisher;
import backend.cowrite.repository.DocumentRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentUpdateCounterTest {

    @Mock
    private DocumentSavePublisher documentSavePublisher;

    @Mock
    private DocumentRedisRepository documentRedisRepository;

    private DocumentUpdateCounter documentUpdateCounter;

    private static final Long DOCUMENT_ID = 1L;
    private static final Long THRESHOLD = 10L;

    @BeforeEach
    void setUp() {
        documentUpdateCounter = new DocumentUpdateCounter(documentSavePublisher, documentRedisRepository);
    }

    @Test
    @DisplayName("update() 호출 시 Redis 카운터를 증가시킨다")
    void update_incrementsRedisCounter() {
        when(documentRedisRepository.getDocumentUpdatedTimes(DOCUMENT_ID)).thenReturn(5L);

        documentUpdateCounter.update(DOCUMENT_ID, "some content");

        verify(documentRedisRepository).generateOrUpdateDocumentUpdatedTime(eq(DOCUMENT_ID), any(Duration.class));
    }

    @Test
    @DisplayName("임계값 미만이면 저장 이벤트를 발행하지 않는다")
    void update_belowThreshold_doesNotPublishSaveEvent() {
        when(documentRedisRepository.getDocumentUpdatedTimes(DOCUMENT_ID)).thenReturn(THRESHOLD - 1);

        documentUpdateCounter.update(DOCUMENT_ID, "some content");

        verify(documentSavePublisher, never()).saveDocument(any(), any());
    }

    @Test
    @DisplayName("임계값(10)에 도달하면 저장 이벤트를 발행한다")
    void update_atThreshold_publishesSaveEvent() {
        String editedContent = "final content";
        when(documentRedisRepository.getDocumentUpdatedTimes(DOCUMENT_ID)).thenReturn(THRESHOLD);

        documentUpdateCounter.update(DOCUMENT_ID, editedContent);

        verify(documentSavePublisher).saveDocument(DOCUMENT_ID, editedContent);
    }

    @Test
    @DisplayName("임계값 초과 시에도 저장 이벤트를 발행한다")
    void update_aboveThreshold_publishesSaveEvent() {
        when(documentRedisRepository.getDocumentUpdatedTimes(DOCUMENT_ID)).thenReturn(THRESHOLD + 5);

        documentUpdateCounter.update(DOCUMENT_ID, "content");

        verify(documentSavePublisher).saveDocument(eq(DOCUMENT_ID), any());
    }

    @Test
    @DisplayName("저장 이벤트 발행 후 카운터를 리셋한다")
    void update_atThreshold_resetsCounterAfterPublish() {
        when(documentRedisRepository.getDocumentUpdatedTimes(DOCUMENT_ID)).thenReturn(THRESHOLD);

        documentUpdateCounter.update(DOCUMENT_ID, "content");

        verify(documentRedisRepository).resetDocumentUpdatedTimes(DOCUMENT_ID);
    }

    @Test
    @DisplayName("임계값 미만이면 카운터를 리셋하지 않는다")
    void update_belowThreshold_doesNotResetCounter() {
        when(documentRedisRepository.getDocumentUpdatedTimes(DOCUMENT_ID)).thenReturn(THRESHOLD - 1);

        documentUpdateCounter.update(DOCUMENT_ID, "content");

        verify(documentRedisRepository, never()).resetDocumentUpdatedTimes(any());
    }
}

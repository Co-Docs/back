package backend.cowrite.service.eventhandler;

import backend.cowrite.client.DocumentClient;
import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventPayload;
import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DeleteOperation;
import backend.cowrite.common.event.payload.DocumentUpdateEventPayload;
import backend.cowrite.common.event.payload.InsertOperation;
import backend.cowrite.common.event.payload.Operation;
import backend.cowrite.repository.DocumentRedisRepository;
import backend.cowrite.service.dto.EditedResult;
import backend.cowrite.utils.OperatorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentUpdateEventHandlerTest {

    @Mock
    private DocumentRedisRepository documentRedisRepository;

    @Mock
    private DocumentClient documentClient;

    @Mock
    private OperatorUtil operatorUtil;

    private DocumentUpdateEventHandler handler;

    private static final Long DOCUMENT_ID = 100L;

    @BeforeEach
    void setUp() {
        handler = new DocumentUpdateEventHandler(documentRedisRepository, documentClient, operatorUtil);
    }

    /**
     * Event.createEvent()는 Event<EventPayload>를 반환하지만 핸들러는 Event<DocumentUpdateEventPayload>를 기대한다.
     * 런타임에는 제네릭이 소거되므로 정상 동작한다 (실제 서비스도 동일하게 동작).
     */
    @SuppressWarnings("unchecked")
    private Event<DocumentUpdateEventPayload> buildUpdateEvent(Long version, String operationId, Operation operation) {
        DocumentUpdateEventPayload payload = DocumentUpdateEventPayload.builder()
                .version(version)
                .operationId(operationId)
                .operation(operation)
                .build();
        return (Event<DocumentUpdateEventPayload>) (Event<?>) Event.createEvent(1L, EventType.UPDATE, payload);
    }

    // ─────────────────────────────────────────────────────────────
    // supports()
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UPDATE 타입 이벤트를 지원한다")
    void supports_updateEventType_returnsTrue() {
        Event<DocumentUpdateEventPayload> event = buildUpdateEvent(0L, "op-1", new InsertOperation(0, "x", "s"));
        assertThat(handler.supports(event)).isTrue();
    }

    // ─────────────────────────────────────────────────────────────
    // baseVersion > serverVersion → 예외
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("baseVersion이 serverVersion보다 크면 IllegalArgumentException 발생")
    void handle_baseVersionExceedsServerVersion_throwsException() {
        when(documentRedisRepository.readVersion(DOCUMENT_ID)).thenReturn(Optional.of(3L));

        Event<DocumentUpdateEventPayload> event = buildUpdateEvent(5L, "op-1", new InsertOperation(0, "x", "s"));

        assertThatThrownBy(() -> handler.handle(DOCUMENT_ID, event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseVersion이 serverVersion보다 큽니다");
    }

    // ─────────────────────────────────────────────────────────────
    // baseVersion == serverVersion → 리베이스 없이 바로 적용
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("baseVersion == serverVersion (리베이스 불필요)")
    class SameVersionPath {

        @Test
        @DisplayName("연산이 리베이스 없이 적용되고 버전이 1 증가한다")
        void handle_sameVersion_applyDirectly() {
            InsertOperation operation = new InsertOperation(0, "hello", "session-A");
            when(documentRedisRepository.readVersion(DOCUMENT_ID)).thenReturn(Optional.of(3L));
            when(documentRedisRepository.readContent(DOCUMENT_ID)).thenReturn(Optional.of(" world"));
            when(operatorUtil.operate(" world", operation)).thenReturn("hello world");

            Event<DocumentUpdateEventPayload> event = buildUpdateEvent(3L, "op-abc", operation);
            EditedResult result = handler.handle(DOCUMENT_ID, event);

            assertThat(result.editedContent()).isEqualTo("hello world");
            assertThat(result.version()).isEqualTo(4L); // 3 + 1
            assertThat(result.operationId()).isEqualTo("op-abc");
            assertThat(result.targetPosition()).isEqualTo(0);
        }

        @Test
        @DisplayName("serverVersion이 없으면 0으로 취급하고, baseVersion 0도 같은 버전으로 처리")
        void handle_noServerVersion_treatedAsZero() {
            InsertOperation operation = new InsertOperation(0, "hi", "session-A");
            when(documentRedisRepository.readVersion(DOCUMENT_ID)).thenReturn(Optional.empty());
            when(documentRedisRepository.readContent(DOCUMENT_ID)).thenReturn(Optional.of(""));
            when(operatorUtil.operate("", operation)).thenReturn("hi");

            Event<DocumentUpdateEventPayload> event = buildUpdateEvent(0L, "op-1", operation);
            EditedResult result = handler.handle(DOCUMENT_ID, event);

            assertThat(result.version()).isEqualTo(1L);
            assertThat(result.editedContent()).isEqualTo("hi");
        }

        @Test
        @DisplayName("Redis에 콘텐츠 없으면 DocumentClient에서 가져온다")
        void handle_contentCacheMiss_fetchesFromDocumentClient() {
            InsertOperation operation = new InsertOperation(5, "!", "session-A");
            DocumentClient.DocumentResponse response = mock(DocumentClient.DocumentResponse.class);
            when(response.getContent()).thenReturn("hello");

            when(documentRedisRepository.readVersion(DOCUMENT_ID)).thenReturn(Optional.of(1L));
            when(documentRedisRepository.readContent(DOCUMENT_ID)).thenReturn(Optional.empty());
            when(documentClient.readDocument(DOCUMENT_ID)).thenReturn(Optional.of(response));
            when(operatorUtil.operate("hello", operation)).thenReturn("hello!");

            Event<DocumentUpdateEventPayload> event = buildUpdateEvent(1L, "op-2", operation);
            EditedResult result = handler.handle(DOCUMENT_ID, event);

            assertThat(result.editedContent()).isEqualTo("hello!");
            verify(documentClient).readDocument(DOCUMENT_ID);
            // 가져온 콘텐츠를 Redis에 캐싱한다
            verify(documentRedisRepository, atLeastOnce())
                    .createOrUpdateContent(eq(DOCUMENT_ID), eq("hello"), any(Duration.class));
        }

        @Test
        @DisplayName("Redis와 DocumentClient 모두 콘텐츠 없으면 빈 문자열에서 연산을 수행한다")
        void handle_contentNotFound_usesEmptyString() {
            InsertOperation operation = new InsertOperation(0, "hello", "session-A");

            when(documentRedisRepository.readVersion(DOCUMENT_ID)).thenReturn(Optional.of(0L));
            when(documentRedisRepository.readContent(DOCUMENT_ID)).thenReturn(Optional.empty());
            when(documentClient.readDocument(DOCUMENT_ID)).thenReturn(Optional.empty());
            when(operatorUtil.operate("", operation)).thenReturn("hello");

            Event<DocumentUpdateEventPayload> event = buildUpdateEvent(0L, "op-3", operation);
            EditedResult result = handler.handle(DOCUMENT_ID, event);

            assertThat(result.editedContent()).isEqualTo("hello");
        }

        @Test
        @DisplayName("handle() 후 Redis에 새 버전, 연산, 콘텐츠가 저장된다")
        void handle_sameVersion_persistsChangesToRedis() {
            InsertOperation operation = new InsertOperation(0, "hi", "session-A");
            when(documentRedisRepository.readVersion(DOCUMENT_ID)).thenReturn(Optional.of(5L));
            when(documentRedisRepository.readContent(DOCUMENT_ID)).thenReturn(Optional.of(""));
            when(operatorUtil.operate("", operation)).thenReturn("hi");

            Event<DocumentUpdateEventPayload> event = buildUpdateEvent(5L, "op-x", operation);
            handler.handle(DOCUMENT_ID, event);

            verify(documentRedisRepository).createOrUpdateVersion(DOCUMENT_ID, "6");
            verify(documentRedisRepository).createOrUpdateOperation(eq(DOCUMENT_ID), any(String.class), eq(6L));
            verify(documentRedisRepository).createOrUpdateContent(eq(DOCUMENT_ID), eq("hi"), any(Duration.class));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // baseVersion < serverVersion → 리베이스 적용
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("baseVersion < serverVersion (리베이스 필요)")
    class DifferentVersionPath {

        @Test
        @DisplayName("baseVersion ~ serverVersion 사이 연산들로 리베이스 후 적용된다")
        void handle_differentVersion_rebasesOperation() {
            InsertOperation original = new InsertOperation(3, "X", "session-A");
            InsertOperation rebased = new InsertOperation(5, "X", "session-A");
            List<String> executedOps = List.of("{\"type\":\"INSERT\",\"targetPosition\":1,\"insertText\":\"ab\",\"sessionId\":\"s\"}");

            when(documentRedisRepository.readVersion(DOCUMENT_ID)).thenReturn(Optional.of(5L));
            when(documentRedisRepository.readContent(DOCUMENT_ID)).thenReturn(Optional.of("hello world"));
            when(documentRedisRepository.readOperation(DOCUMENT_ID, 3L, 5L)).thenReturn(executedOps);
            when(operatorUtil.rebaseOperation(original, executedOps)).thenReturn(rebased);
            when(operatorUtil.operate("hello world", rebased)).thenReturn("hello Xworld");

            Event<DocumentUpdateEventPayload> event = buildUpdateEvent(3L, "op-rebase", original);
            EditedResult result = handler.handle(DOCUMENT_ID, event);

            assertThat(result.editedContent()).isEqualTo("hello Xworld");
            assertThat(result.version()).isEqualTo(6L); // serverVersion(5) + 1
            assertThat(result.targetPosition()).isEqualTo(5); // rebased position
            verify(operatorUtil).rebaseOperation(original, executedOps);
        }

        @Test
        @DisplayName("리베이스된 연산이 Redis에 저장된다")
        void handle_differentVersion_persistsRebasedOperation() {
            InsertOperation original = new InsertOperation(0, "A", "session-A");
            InsertOperation rebased = new InsertOperation(3, "A", "session-A");
            List<String> executedOps = List.of("some-op-json");

            when(documentRedisRepository.readVersion(DOCUMENT_ID)).thenReturn(Optional.of(2L));
            when(documentRedisRepository.readContent(DOCUMENT_ID)).thenReturn(Optional.of("hi"));
            when(documentRedisRepository.readOperation(DOCUMENT_ID, 1L, 2L)).thenReturn(executedOps);
            when(operatorUtil.rebaseOperation(original, executedOps)).thenReturn(rebased);
            when(operatorUtil.operate("hi", rebased)).thenReturn("hiA");

            Event<DocumentUpdateEventPayload> event = buildUpdateEvent(1L, "op-y", original);
            handler.handle(DOCUMENT_ID, event);

            verify(documentRedisRepository).createOrUpdateVersion(DOCUMENT_ID, "3");
            verify(documentRedisRepository).createOrUpdateOperation(eq(DOCUMENT_ID), any(String.class), eq(3L));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DeleteOperation의 targetPosition 추출
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("DeleteOperation일 때 targetPosition이 EditedResult에 반영된다")
    void handle_deleteOperation_extractsTargetPosition() {
        DeleteOperation operation = new DeleteOperation(7, 3);
        when(documentRedisRepository.readVersion(DOCUMENT_ID)).thenReturn(Optional.of(0L));
        when(documentRedisRepository.readContent(DOCUMENT_ID)).thenReturn(Optional.of("hello world"));
        when(operatorUtil.operate("hello world", operation)).thenReturn("hello rd");

        Event<DocumentUpdateEventPayload> event = buildUpdateEvent(0L, "op-del", operation);
        EditedResult result = handler.handle(DOCUMENT_ID, event);

        assertThat(result.targetPosition()).isEqualTo(7);
    }
}

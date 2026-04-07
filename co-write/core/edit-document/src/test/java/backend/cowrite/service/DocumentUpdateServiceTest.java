package backend.cowrite.service;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventPayload;
import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DocumentUpdateEventPayload;
import backend.cowrite.common.event.payload.InsertOperation;
import backend.cowrite.service.dto.EditedResult;
import backend.cowrite.service.eventhandler.EventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class DocumentUpdateServiceTest {

    @Mock
    private EventHandler updateHandler; // DocumentUpdateService는 List<EventHandler> (raw type) 사용

    @Mock
    private DocumentUpdateCounter documentUpdateCounter;

    private DocumentUpdateService documentUpdateService;

    private Event<EventPayload> updateEvent;

    @BeforeEach
    void setUp() {
        List<EventHandler> handlers = new ArrayList<>();
        handlers.add(updateHandler);
        documentUpdateService = new DocumentUpdateService(handlers, documentUpdateCounter);

        DocumentUpdateEventPayload payload = DocumentUpdateEventPayload.builder()
                .version(0L)
                .operationId("op-001")
                .operation(new InsertOperation(0, "hello", "session-A"))
                .build();
        updateEvent = Event.createEvent(1L, EventType.UPDATE, payload);
    }

    @Test
    @DisplayName("supports()가 true인 핸들러로 이벤트를 위임하고 결과를 반환한다")
    void handleEvent_delegatesToMatchingHandler() {
        Long documentId = 42L;
        EditedResult expectedResult = new EditedResult("hello", 1L, 0, "op-001");

        when(updateHandler.supports(updateEvent)).thenReturn(true);
        when(updateHandler.handle(documentId, updateEvent)).thenReturn(expectedResult);

        EditedResult result = documentUpdateService.handleEvent(documentId, updateEvent);

        assertThat(result).isEqualTo(expectedResult);
        verify(updateHandler).handle(documentId, updateEvent);
    }

    @Test
    @DisplayName("이벤트 처리 후 DocumentUpdateCounter.update()를 호출한다")
    void handleEvent_updatesCounter() {
        Long documentId = 42L;
        EditedResult expectedResult = new EditedResult("hello", 1L, 0, "op-001");

        when(updateHandler.supports(updateEvent)).thenReturn(true);
        when(updateHandler.handle(documentId, updateEvent)).thenReturn(expectedResult);

        documentUpdateService.handleEvent(documentId, updateEvent);

        verify(documentUpdateCounter).update(documentId, "hello");
    }

    @Test
    @DisplayName("지원하는 핸들러가 없으면 IllegalArgumentException 발생")
    void handleEvent_noMatchingHandler_throwsException() {
        Long documentId = 42L;
        when(updateHandler.supports(updateEvent)).thenReturn(false);

        assertThatThrownBy(() -> documentUpdateService.handleEvent(documentId, updateEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이벤트 핸들러를 찾지 못했습니다");
    }
}

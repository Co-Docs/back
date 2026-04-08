package backend.cowrite.service.eventhandler;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventPayload;
import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DocumentSaveEventPayload;
import backend.cowrite.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentSaveEventHandlerTest {

    @Mock
    private DocumentService documentService;

    private DocumentSaveEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DocumentSaveEventHandler(documentService);
    }

    @SuppressWarnings("unchecked")
    private Event<DocumentSaveEventPayload> buildSaveEvent(String editedContent) {
        DocumentSaveEventPayload payload = DocumentSaveEventPayload.builder()
                .editedContent(editedContent)
                .build();
        return (Event<DocumentSaveEventPayload>) (Event<?>) Event.createEvent(1L, EventType.SAVE, payload);
    }

    @Test
    @DisplayName("SAVE 이벤트를 지원한다")
    void supports_saveEventType_returnsTrue() {
        Event<DocumentSaveEventPayload> event = buildSaveEvent("content");
        assertThat(handler.supports(event)).isTrue();
    }

    @Test
    @DisplayName("handle() 호출 시 title을 null로 하여 DocumentService.updateDocument()를 호출한다")
    void handle_callsUpdateDocumentWithNullTitle() {
        Long documentId = 42L;
        Event<DocumentSaveEventPayload> event = buildSaveEvent("updated content");

        handler.handle(documentId, event);

        verify(documentService).updateDocument(documentId, null, "updated content");
    }

    @Test
    @DisplayName("editedContent가 빈 문자열이어도 updateDocument()가 호출된다")
    void handle_emptyContent_stillCallsUpdateDocument() {
        Long documentId = 10L;
        Event<DocumentSaveEventPayload> event = buildSaveEvent("");

        handler.handle(documentId, event);

        verify(documentService).updateDocument(documentId, null, "");
    }
}

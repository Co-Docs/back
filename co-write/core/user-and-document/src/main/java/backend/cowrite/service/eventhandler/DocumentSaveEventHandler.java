package backend.cowrite.service.eventhandler;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DocumentSaveEventPayload;
import backend.cowrite.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentSaveEventHandler implements EventHandler<DocumentSaveEventPayload> {

    private final DocumentService documentService;

    @Override
    public void handle(Long documentId, Event<DocumentSaveEventPayload> event) {
        String editedContent = event.getPayload().getEditedContent();
        documentService.updateDocument(documentId, null, editedContent);
    }

    @Override
    public boolean supports(Event<DocumentSaveEventPayload> event) {
        return EventType.SAVE == event.getEventType();
    }
}

package backend.cowrite.publisher;

import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DocumentSaveEventPayload;
import backend.cowrite.common.outboxmessagerelay.pub.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DocumentSavePublisher {

    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public void saveDocument(Long documentId, String editedContent) {
        DocumentSaveEventPayload documentSaveEventPayload = DocumentSaveEventPayload.builder()
                .editedContent(editedContent)
                .build();
        outboxEventPublisher.publish(EventType.SAVE, documentSaveEventPayload, documentId);
    }
}

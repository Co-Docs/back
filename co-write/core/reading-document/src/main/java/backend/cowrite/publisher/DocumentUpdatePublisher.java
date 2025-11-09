package backend.cowrite.publisher;

import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DocumentEventPayload;
import backend.cowrite.common.outboxmessagerelay.pub.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DocumentUpdatePublisher {
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public void updateDocument(Long documentId, DocumentEventPayload eventPayloadPayload) {
        outboxEventPublisher.publish(EventType.UPDATE, eventPayloadPayload, documentId);
    }
}

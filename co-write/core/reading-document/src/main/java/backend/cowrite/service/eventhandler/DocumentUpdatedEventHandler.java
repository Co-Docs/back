package backend.cowrite.service.eventhandler;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DocumentUpdatedEventPayload;
import backend.cowrite.repository.DocumentRedisRepository;
import backend.cowrite.utils.TimeCalculatorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
public class DocumentUpdatedEventHandler implements EventHandler<DocumentUpdatedEventPayload> {
    private final DocumentRedisRepository documentUpdatedRepository;

    @Override
    public void handle(Event<DocumentUpdatedEventPayload> event) {
        DocumentUpdatedEventPayload payload = event.getPayload();
        documentUpdatedRepository.createOrUpdate(payload.getDocumentId(), payload.getContent(), TimeCalculatorUtils.calculateDurationToMidnight());
    }

    @Override
    public boolean supports(Event<DocumentUpdatedEventPayload> event) {
        return EventType.DOCUMENT_UPDATE == event.getEventType();
    }
}

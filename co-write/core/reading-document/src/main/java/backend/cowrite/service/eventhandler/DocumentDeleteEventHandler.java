package backend.cowrite.service.eventhandler;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DocumentDeleteEventPayload;
import backend.cowrite.repository.DocumentRedisRepository;
import backend.cowrite.utils.TimeCalculatorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
public class DocumentInsertEventHandler implements EventHandler<DocumentDeleteEventPayload> {
    private final DocumentRedisRepository documentUpdatedRepository;

    @Override
    public void handle(Event<DocumentDeleteEventPayload> event) {
        DocumentDeleteEventPayload payload = event.getPayload();
        documentUpdatedRepository.createOrUpdate(payload.(), payload.getContent(), TimeCalculatorUtils.calculateDurationToMidnight());
    }

    @Override
    public boolean supports(Event<DocumentDeleteEventPayload> event) {
        return EventType.DOCUMENT_UPDATE == event.getEventType();
    }
}

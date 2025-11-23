package backend.cowrite.service.eventhandler;


import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventPayload;
import backend.cowrite.service.dto.EditedResult;

public interface EventHandler<T extends EventPayload> {
    EditedResult handle(Long documentId, Event<T> event);
    boolean supports(Event<T> event);
}

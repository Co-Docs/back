package backend.cowrite.publisher.eventhandler;


import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventPayload;

public interface EventHandler<T extends EventPayload> {
    void handle(Event<T> event);
    boolean supports(Event<T> event);
}

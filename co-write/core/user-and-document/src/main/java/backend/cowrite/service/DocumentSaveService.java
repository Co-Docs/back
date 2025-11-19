package backend.cowrite.service;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventPayload;
import backend.cowrite.service.eventhandler.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentSaveService {

    private List<EventHandler> eventHandlers;

    public void handleEvent(Long documentId, Event<EventPayload> event) {
        EventHandler<EventPayload> eventHandler = findEventHandler(event);
        eventHandler.handle(documentId, event);
    }

    private EventHandler<EventPayload> findEventHandler(Event<EventPayload> event) {
        return eventHandlers.stream()
                .filter(eventHandler -> eventHandler.supports(event))
                .findAny()
                .orElseThrow(()-> new IllegalArgumentException("해당하는 이벤트 핸들러를 찾지 못했습니다."));
    }
}

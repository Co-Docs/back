package backend.cowrite.service;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventPayload;
import backend.cowrite.service.dto.EditedResult;
import backend.cowrite.service.eventhandler.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentUpdateService {
    private final List<EventHandler> eventHandlers;
    private final DocumentUpdateCounter documentUpdateCounter;

    public EditedResult handleEvent(Long documentId, Event<EventPayload> event) {
        EventHandler<EventPayload> eventHandler = findEventHandler(event);
        EditedResult editedResult = eventHandler.handle(documentId, event);
        documentUpdateCounter.update(documentId, editedResult.editedContent());
        return editedResult;
    }

    private EventHandler<EventPayload> findEventHandler(Event<EventPayload> event) {
        return eventHandlers.stream()
                .filter(eventHandler -> eventHandler.supports(event))
                .findAny()
                .orElseThrow(()-> new IllegalArgumentException("해당하는 이벤트 핸들러를 찾지 못했습니다."));
    }
}

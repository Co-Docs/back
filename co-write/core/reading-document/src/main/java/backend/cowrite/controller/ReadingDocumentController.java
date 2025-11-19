package backend.cowrite.controller;

import backend.cowrite.common.event.payload.DocumentUpdateEventPayload;
import backend.cowrite.publisher.DocumentUpdatePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ReadingDocumentController {

    private final DocumentUpdatePublisher documentUpdatePublisher;

    @MessageMapping("/{documentId}")
    public void editDocument(@DestinationVariable Long documentId, @Payload DocumentUpdateEventPayload updateEventPayload) {
        log.info("[ReadingDocumentController] documentId = {}, updateEventPayloadVersion = {}", documentId, updateEventPayload.getVersion());
        documentUpdatePublisher.updateDocument(documentId, updateEventPayload);
    }

}

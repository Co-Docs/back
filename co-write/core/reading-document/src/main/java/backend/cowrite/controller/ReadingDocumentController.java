package backend.cowrite.controller;

import backend.cowrite.common.event.payload.DocumentEventPayload;
import backend.cowrite.publisher.DocumentUpdatePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/app/document")
public class ReadingDocumentController {

    private final DocumentUpdatePublisher documentUpdatePublisher;

    @MessageMapping("/{documentId}")
    public void editDocument(@DestinationVariable Long documentId, @Payload DocumentEventPayload updateEventPayload) {
        documentUpdatePublisher.deleteDocument(documentId, updateEventPayload);
    }

}

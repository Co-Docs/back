package backend.cowrite.consumer;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventPayload;
import backend.cowrite.common.event.EventType;
import backend.cowrite.publisher.DocumentSavePublisher;
import backend.cowrite.service.DocumentUpdateService;
import backend.cowrite.service.dto.EditedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static backend.cowrite.config.WebsocketConfig.ClientSubscribeRoute;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentUpdateEventConsumer {
    private final DocumentUpdateService documentUpdateService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentSavePublisher documentSavePublisher;


    @KafkaListener(
            topics = {EventType.Topic.UPDATE},
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, String> outbox, Acknowledgment ack) {
        log.info("[DocumentUpdateEventConsumer.listen()] documentId = {}, message = {}", outbox.key(), outbox.value());
        Long documentId = Long.valueOf(outbox.key());
        Event<EventPayload> event = Event.fromJson(outbox.value());
        if (event != null) {
            processDocumentUpdate(documentId, event);
        }
        ack.acknowledge();
    }

    private void processDocumentUpdate(Long documentId, Event<EventPayload> event) {
        EditedResult editedResult = documentUpdateService.handleEvent(documentId, event);
        String destination = documentSubscribeRoute(documentId);
        messagingTemplate.convertAndSend(destination,editedResult);
    }

    private String documentSubscribeRoute(Long documentId) {
        return String.format("%s/%s", ClientSubscribeRoute, documentId);
    }

}

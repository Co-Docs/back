package backend.cowrite.consumer;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventPayload;
import backend.cowrite.common.event.EventType;
import backend.cowrite.service.DocumentUpdateService;
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


    @KafkaListener(
            topics = {EventType.Topic.UPDATE},
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, String> outbox, Acknowledgment ack) {
        String stringDocumentId = outbox.key();
        String stringEvent = outbox.value();
        log.info("[DocumentUpdateEventConsumer.listen()] documentId = {}, message = {}", stringDocumentId, stringEvent);
        Long documentId = Long.valueOf(stringDocumentId);
        Event<EventPayload> event = Event.fromJson(stringEvent);
        if (event != null) {
            documentUpdateService.handleEvent(documentId, event);
            String destination = documentSubscribeRoute(stringDocumentId);
            messagingTemplate.convertAndSend(destination,stringEvent);
        }
        ack.acknowledge();
    }

    private String documentSubscribeRoute(String key) {
        return String.format("%s/%s", ClientSubscribeRoute, key);
    }

}

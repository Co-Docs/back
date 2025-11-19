package backend.cowrite.consumer;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventPayload;
import backend.cowrite.common.event.EventType;
import backend.cowrite.service.DocumentSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentSaveEventConsumer {

    private final DocumentSaveService documentSaveService;

    @KafkaListener(
            topics = {EventType.Topic.SAVE},
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, String> outbox, Acknowledgment ack) {
        String outboxDocumentId = outbox.key();
        String outboxEvent = outbox.value();
        log.info("[DocumentSaveEventConsumer.listen()] documentId = {}, message = {}", outboxDocumentId, outboxEvent);
        Long documentId = Long.valueOf(outboxDocumentId);
        Event<EventPayload> event = Event.fromJson(outboxEvent);

        if(event != null) {
            documentSaveService.handleEvent(documentId, event);
        }
        ack.acknowledge();
    }
}

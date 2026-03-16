package backend.cowrite.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentUpdateEventDLQConsumer {

    @KafkaListener(
            topics = "document-update-dlq",
            containerFactory = "dlqKafkaListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, String> outbox, Acknowledgment ack) {
        log.warn("[DLQ] Received failed message: key={}, value={}, headers={}",
                outbox.key(), outbox.value(), outbox.headers());
        ack.acknowledge();
    }
}

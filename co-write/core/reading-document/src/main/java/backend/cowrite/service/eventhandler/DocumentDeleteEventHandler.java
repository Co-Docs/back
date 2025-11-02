package backend.cowrite.service.eventhandler;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DocumentDeleteEventPayload;
import backend.cowrite.repository.DocumentRedisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class DocumentDeleteEventHandler implements EventHandler<DocumentDeleteEventPayload> {
    private final DocumentRedisRepository documentRedisRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(Long documentId, Event<DocumentDeleteEventPayload> event) {
        getOperation(documentId, event.getPayload().getVersion()); // 실행할 operation 가져오기
        operate() // Operation 실행하기
        saveContent() // 결과 content 저장하기
    }

    public List<String> getOperation(Long documentId, Long newVersion) {
        Long baseVersion = documentRedisRepository.readVersion(documentId);
        return documentRedisRepository.readOperation(documentId, baseVersion, newVersion);
    }


    @Override
    public boolean supports(Event<DocumentDeleteEventPayload> event) {
        return EventType.DELETE == event.getEventType();
    }
}

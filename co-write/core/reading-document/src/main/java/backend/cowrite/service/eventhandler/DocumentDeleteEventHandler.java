package backend.cowrite.service.eventhandler;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DeleteOperation;
import backend.cowrite.common.event.payload.DocumentDeleteEventPayload;
import backend.cowrite.config.ObjectMapperConfig;
import backend.cowrite.repository.DocumentRedisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
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
        Long baseVersion = documentRedisRepository.readBaseVersion(documentId);
        Long newVersion = event.getPayload().getVersion();

        if(baseVersion < newVersion) {
            List<String> operations = documentRedisRepository.readOperationsByVersion(documentId, baseVersion, newVersion);
            List<DeleteOperation> mustExecuteOperation = operations.stream().map( operation -> {
                try {
                    return objectMapper.readValue(operation, DeleteOperation.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("DeleteOperation으로 변경이 불가능합니다.");
                }
            }).toList();
        }

        String content = documentRedisRepository.readContent(documentId);
        String newContent =
        List<DeleteOperation> operations = event.getPayload().getOperations();
    }

    @Override
    public boolean supports(Event<DocumentDeleteEventPayload> event) {
        return EventType.DELETE == event.getEventType();
    }
}

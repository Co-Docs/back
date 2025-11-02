package backend.cowrite.service.eventhandler;

import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DeleteOperation;
import backend.cowrite.common.event.payload.DocumentEventPayload;
import backend.cowrite.common.event.payload.InsertOperation;
import backend.cowrite.common.event.payload.Operation;
import backend.cowrite.repository.DocumentRedisRepository;
import backend.cowrite.service.OperatorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;


@Component
@RequiredArgsConstructor
public class DocumentUpdateEventHandler implements EventHandler<DocumentEventPayload> {
    private final DocumentRedisRepository documentRedisRepository;
    private final OperatorUtil operatorUtil;

    @Override
    public void handle(Long documentId, Event<DocumentEventPayload> event) {
        Long newVersion = event.getPayload().getVersion();
        List<Operation> newOperations = event.getPayload().getOperations();
        List<Operation> executeOperation = getOperation(documentId, newVersion, newOperations);
        String savedContent = documentRedisRepository.readContent(documentId);
        String editedContent = operatorUtil.operate(savedContent, executeOperation);
        updateChanges(documentId, newVersion, newOperations, editedContent);
    }

    private void updateChanges(Long documentId, Long newVersion, List<Operation> newOperations, String editedContent) {
        documentRedisRepository.createOrUpdateVersion(documentId, String.valueOf(newVersion));
        for (String operation : operatorUtil.parseOperation(newOperations)) {
            documentRedisRepository.createOrUpdateOperation(documentId, operation, newVersion);
        }
        documentRedisRepository.createOrUpdateContent(documentId, editedContent, Duration.ofHours(5L));
    }

    private List<Operation> getOperation(Long documentId, Long newVersion, List<Operation> newOperations) {
        Long baseVersion = documentRedisRepository.readVersion(documentId);
        List<String> operations = documentRedisRepository.readOperation(documentId, baseVersion, newVersion);
        return operatorUtil.parseOperation(newOperations, operations);
    }

    @Override
    public boolean supports(Event<DocumentEventPayload> event) {
        return EventType.UPDATE == event.getEventType();
    }
}

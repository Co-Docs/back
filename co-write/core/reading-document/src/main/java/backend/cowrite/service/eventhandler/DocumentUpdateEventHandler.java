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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentUpdateEventHandler implements EventHandler<DocumentEventPayload> {
    private final DocumentRedisRepository documentRedisRepository;
    private final OperatorUtil operatorUtil;
    private static final int VERSION_UPDATE_COUNT = 1;

    @Override
    public void handle(Long documentId, Event<DocumentEventPayload> event) {
        Long baseVersion  = event.getPayload().getVersion();
        Long serverVersion = documentRedisRepository.readVersion(documentId);
        validateVersion(baseVersion, serverVersion);
        List<Operation> newOperations = event.getPayload().getOperations();
        String savedContent = documentRedisRepository.readContent(documentId);
        log.info("savedContent = {}", savedContent);
        String editedContent = operatorUtil.operate(savedContent, newOperations);
        log.info("editedContent = {}", editedContent);
        updateChanges(documentId, serverVersion + VERSION_UPDATE_COUNT , newOperations, editedContent);
    }

    private static void validateVersion(Long baseVersion, Long serverVersion) {
        if (!baseVersion.equals(serverVersion)) {
            throw new IllegalArgumentException("서버 operation 버전과 client operationi 버전이 다릅니다.");
        }
    }

    private void updateChanges(Long documentId, Long newVersion, List<Operation> newOperations, String editedContent) {
        documentRedisRepository.createOrUpdateVersion(documentId, String.valueOf(newVersion));
        for (String operation : operatorUtil.parseOperation(newOperations)) {
            documentRedisRepository.createOrUpdateOperation(documentId, operation, newVersion);
        }
        documentRedisRepository.createOrUpdateContent(documentId, editedContent, Duration.ofHours(5L));
    }

    @Override
    public boolean supports(Event<DocumentEventPayload> event) {
        return EventType.UPDATE == event.getEventType();
    }
}

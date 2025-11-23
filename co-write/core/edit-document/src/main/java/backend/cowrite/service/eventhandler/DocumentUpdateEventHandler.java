package backend.cowrite.service.eventhandler;

import backend.cowrite.client.DocumentClient;
import backend.cowrite.common.dataserializer.DataSerializer;
import backend.cowrite.common.event.Event;
import backend.cowrite.common.event.EventType;
import backend.cowrite.common.event.payload.DocumentUpdateEventPayload;
import backend.cowrite.common.event.payload.Operation;
import backend.cowrite.repository.DocumentRedisRepository;
import backend.cowrite.service.dto.EditedResult;
import backend.cowrite.utils.OperatorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;


@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentUpdateEventHandler implements EventHandler<DocumentUpdateEventPayload> {
    private final DocumentRedisRepository documentRedisRepository;
    private final DocumentClient documentClient;
    private final OperatorUtil operatorUtil;
    private static final int VERSION_UPDATE_COUNT = 1;
    private final Duration CONTENT_TTL = Duration.ofHours(5L);

    @Override
    public EditedResult handle(Long documentId, Event<DocumentUpdateEventPayload> event) {
        Long baseVersion = event.getPayload().getVersion();
        Long serverVersion = documentRedisRepository.readVersion(documentId)
                .orElse(0L);
        Operation newOperation = event.getPayload().getOperation();
        validateVersion(baseVersion, serverVersion);
        if (baseVersion.equals(serverVersion)) {
            return applyCurrentOperation(documentId, serverVersion, newOperation);
        } else {
            return applyRebasedOperation(documentId, baseVersion, serverVersion, newOperation);
        }
    }

    private EditedResult applyCurrentOperation(Long documentId, Long serverVersion, Operation newOperation) {
        String editedContent = editContent(documentId, newOperation);
        Long newVersion = serverVersion + VERSION_UPDATE_COUNT;
        updateChanges(documentId, newVersion, newOperation, editedContent);
        return new EditedResult(editedContent, newVersion);
    }

    private EditedResult applyRebasedOperation(Long documentId, Long baseVersion, Long serverVersion, Operation newOperation) {
        Operation rebasedOperation = rebaseOperation(documentId, baseVersion, serverVersion, newOperation);
        String edited = editContent(documentId, rebasedOperation);
        Long newVersion = serverVersion + VERSION_UPDATE_COUNT;
        updateChanges(documentId, newVersion, rebasedOperation, edited);
        return new EditedResult(edited, newVersion);
    }

    private Operation rebaseOperation(Long docId, Long baseVersion, Long serverVersion, Operation newOperation) {
        List<String> executedJson = documentRedisRepository.readOperation(docId, baseVersion, serverVersion);
        return operatorUtil.rebaseOperation(newOperation, executedJson);
    }

    private String editContent(Long documentId, Operation executedOperation) {
        String savedContent = documentRedisRepository.readContent(documentId)
                .or(()-> fetch(documentId))
                .orElseThrow(() -> new IllegalArgumentException("문서 내용이 없습니다."));
       log.info("savedContent = {}", savedContent);
        String editedContent = operatorUtil.operate(savedContent, executedOperation);
        log.info("editedContent = {}", editedContent);
        return editedContent;
    }

    private Optional<String> fetch(Long documentId) {
        Optional<String> savedContent = documentClient.readDocument(documentId)
                .map(DocumentClient.DocumentResponse::getContent);
        savedContent.ifPresent(content -> documentRedisRepository.createOrUpdateContent(documentId, content, CONTENT_TTL));
        return savedContent;
    }

    private static void validateVersion(Long baseVersion, Long serverVersion) {
        if (baseVersion > serverVersion) {
            throw new IllegalArgumentException("baseVersion이 serverVersion보다 큽니다.");
        }
    }

    private void updateChanges(Long docId, Long newVersion, Operation op, String content) {
        documentRedisRepository.createOrUpdateVersion(docId, String.valueOf(newVersion));
        documentRedisRepository.createOrUpdateOperation(docId, DataSerializer.serialize(op), newVersion);
        documentRedisRepository.createOrUpdateContent(docId, content, CONTENT_TTL);
    }

    @Override
    public boolean supports(Event<DocumentUpdateEventPayload> event) {
        return EventType.UPDATE == event.getEventType();
    }
}

package backend.cowrite.service;

import backend.cowrite.entity.Document;
import backend.cowrite.entity.User;
import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import backend.cowrite.repository.DocumentRepository;
import backend.cowrite.service.request.ParticipantsUpdateRequest;
import backend.cowrite.service.response.DocumentDetailResponse;
import backend.cowrite.service.response.DocumentPreviewResponse;
import kuke.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserService userService;
    private final Snowflake snowflake;

    @Transactional(readOnly = true)
    public DocumentPreviewResponse readAll(Long userId, Pageable pageable) {
        List<Document> documents = documentRepository.readAll(userId, pageable);
        return DocumentPreviewResponse.of(documents);
    }

    @Transactional
    public Long addNewDocument(Long myId, String title, String content, List<String> participantsName) {
        User owner = userService.findById(myId);
        List<User> participants = participantsName.stream().map(userService::findByName).toList();
        Document document = Document.addNewDocument(snowflake.nextId(), title, content, owner, participants);
        return documentRepository.save(document).getDocumentId();
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse readDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCS_NOT_FOUND, "해당 documentId의 문서 정보가 존재하지 않습니다." + documentId));
        return DocumentDetailResponse.of(document);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCS_NOT_FOUND, "해당 documentId의 문서 정보가 존재하지 않습니다." + documentId));
        documentRepository.delete(document);
    }

    @Transactional
    public Long updateDocument(Long documentId, String title, String content) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCS_NOT_FOUND, "해당 documentId의 문서 정보가 존재하지 않습니다." + documentId));
        document.updateDifferences(title, content);
        return document.getDocumentId();
    }

    @Transactional
    public Long updateParticipants(Long documentId, List<Long> newParticipantsIds) {
        List<User> newParticipants = newParticipantsIds.stream().map(userService::findById).toList();
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCS_NOT_FOUND, "해당 documentId의 문서 정보가 존재하지 않습니다." + documentId));
        document.addParticipants(newParticipants);
        return document.getDocumentId();
    }
}

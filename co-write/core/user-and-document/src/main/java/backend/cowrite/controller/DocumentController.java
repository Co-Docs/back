package backend.cowrite.controller;

import backend.cowrite.auth.CustomUserDetails;
import backend.cowrite.common.ResponseHandler;
import backend.cowrite.service.DocumentService;
import backend.cowrite.service.request.DocumentRequest;
import backend.cowrite.service.request.DocumentUpdateRequest;
import backend.cowrite.service.request.ParticipantsUpdateRequest;
import backend.cowrite.service.response.DocumentDetailResponse;
import backend.cowrite.service.response.DocumentPreviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/document")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    private ResponseEntity<ResponseHandler<DocumentPreviewResponse>> readAll(@AuthenticationPrincipal CustomUserDetails userDetails, Pageable pageable) {
        log.info("모든 문서 출력 메서드 실행 pageNumber = {}, pageSize = {}", pageable.getPageNumber(), pageable.getPageSize());
        DocumentPreviewResponse documentResponse = documentService.readAll(userDetails.getUser().userId(), pageable);
        return ResponseEntity.ok(ResponseHandler.success(documentResponse));
    }

    @PostMapping
    private ResponseEntity<ResponseHandler<Long>> addNewDocument(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody DocumentRequest documentRequest) {
        log.info("문서 추가 메서드 실행 documentRequest = {}", documentRequest.toString());
        Long saveDocsId = documentService.addNewDocument(userDetails.getUser().userId(), documentRequest.title(), documentRequest.password(), documentRequest.userDocuments());
        return ResponseEntity.ok(ResponseHandler.success(saveDocsId));
    }

    @GetMapping("/{documentId}")
    private ResponseEntity<ResponseHandler<DocumentDetailResponse>> readDocument(@PathVariable("documentId") Long documentId) {
        log.debug("문서 상세 정보 출력 메서드 실행 documentId = {}", documentId);
        DocumentDetailResponse documentDetailResponse = documentService.readDocument(documentId);
        return ResponseEntity.ok(ResponseHandler.success(documentDetailResponse));
    }

    @DeleteMapping("/{documentId}")
    private ResponseEntity<ResponseHandler<Void>> deleteDocument(@PathVariable("documentId") Long documentId) {
        log.debug("문서 삭제 메서드 실행 documentId = {}", documentId);
        documentService.deleteDocument(documentId);
        return ResponseEntity.ok(ResponseHandler.success(null));
    }

    @PatchMapping("/{documentId}")
    private ResponseEntity<ResponseHandler<Long>> updateDocument(@PathVariable("documentId") Long documentId, @RequestBody DocumentUpdateRequest documentUpdateRequest) {
        log.debug("문서 내용 수정 메서드 실행 documentId = {}, documentUpdateRequest = {}", documentId, documentUpdateRequest);
        Long updatedId = documentService.updateDocument(documentId, documentUpdateRequest.title(), documentUpdateRequest.content());
        return ResponseEntity.ok(ResponseHandler.success(updatedId));
    }

    @PutMapping("/{documentId}")
    private ResponseEntity<ResponseHandler<String>> updateParticipants(@PathVariable("documentId") Long documentId, @RequestBody ParticipantsUpdateRequest participantsUpdateRequest) {
        log.debug("문서 참가자 수정 메서드 실행 documentId = {}, participantsUpdateRequest = {}", documentId, participantsUpdateRequest);
        String userNickname = documentService.updateParticipants(documentId, participantsUpdateRequest.username());
        return ResponseEntity.ok(ResponseHandler.success(userNickname));
    }

}

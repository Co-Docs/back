package backend.cowrite.controller;

import backend.cowrite.common.responsehandler.ResponseHandler;
import backend.cowrite.service.ReadDocumentService;
import backend.cowrite.service.response.DocumentPreviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/readDocument")
public class ReadDocumentController {

    private final ReadDocumentService readDocumentService;

    @GetMapping
    private ResponseEntity<ResponseHandler<DocumentPreviewResponse>> readAll(Long userId) {
        DocumentPreviewResponse documentResponse = readDocumentService.readAll(userId);
        return ResponseEntity.ok(ResponseHandler.success(documentResponse));
    }
}

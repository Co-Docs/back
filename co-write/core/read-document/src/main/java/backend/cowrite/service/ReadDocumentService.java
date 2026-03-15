package backend.cowrite.service;

import backend.cowrite.client.DocumentClient;
import backend.cowrite.common.dataserializer.DataSerializer;
import backend.cowrite.repository.ReadDocumentRepository;
import backend.cowrite.service.response.DocumentPreviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadDocumentService {

    private final ReadDocumentRepository readDocumentRepository;
    private final DocumentClient documentClient;
    private final Duration CONTENT_TTL = Duration.ofHours(5L);

    public DocumentPreviewResponse readAll(Long userId) {
        DocumentPreviewResponse documentPreviewResponse = readDocumentRepository.readAll(userId)
                .or(() -> fetch(userId))
                .orElseThrow(()-> new IllegalArgumentException("편집 중인 문서가 없습니다."));
    }

    private Optional<DocumentPreviewResponse> fetch(Long userId) {
        Optional<DocumentPreviewResponse> documentPreviewResponse = documentClient.readAll(userId);
        String serializePreview = DataSerializer.serialize(documentPreviewResponse);
        documentPreviewResponse.ifPresent(previewResponse -> readDocumentRepository.createOrUpdateContent(userId, serializePreview, CONTENT_TTL));
        return documentPreviewResponse;
    }
}

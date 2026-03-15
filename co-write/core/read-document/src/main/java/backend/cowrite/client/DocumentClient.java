package backend.cowrite.client;

import backend.cowrite.service.response.DocumentPreviewResponse;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentClient {

    private RestClient restClient;

    @Value("${endpoints.document-service.url}")
    private String documentServiceUrl;

    @PostConstruct
    public void initRestClient() {
        restClient = RestClient.create(documentServiceUrl);
    }

    public Optional<DocumentPreviewResponse> readAll(Long userId) {
        try {
            DocumentPreviewResponse documentResponse = restClient.get()
                    .uri("/api/document/", userId)
                    .retrieve()
                    .body(DocumentPreviewResponse.class);
            return Optional.of(documentResponse);
        } catch (Exception e) {
            log.error("[DocumentClient.readDocument] userId = {}", userId);
            return Optional.empty();
        }
    }

}

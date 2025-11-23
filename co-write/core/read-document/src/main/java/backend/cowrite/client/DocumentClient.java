package backend.cowrite.client;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentClient {

    private RestClient restClient;

    @Value("")
    private String documentServiceUrl;

    @PostConstruct
    public void initRestClient() {
        restClient = RestClient.create(documentServiceUrl);
    }

    public Optional<DocumentResponse> readDocument(Long documentId) {
        try {
            DocumentResponse documentResponse = restClient.get()
                    .uri("{documentId}", documentId)
                    .retrieve()
                    .body(DocumentResponse.class);
            return Optional.of(documentResponse);
        } catch (Exception e) {
            log.error("[DocumentClient.readDocument] documentId = {}", documentId);
            return Optional.empty();
        }
    }

    @Getter
    public static class DocumentResponse {
        private String title;
        private String content;
    }
}

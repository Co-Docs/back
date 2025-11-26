package backend.cowrite.client;

import backend.cowrite.common.responsehandler.ResponseHandler;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class DocumentClient {

    private RestClient restClient;

    @Value("${endpoints.document-service.url}")
    private String documentServiceUrl;

    @PostConstruct
    public void initRestClient() {
        restClient = RestClient.create(documentServiceUrl);
    }

    public Optional<DocumentResponse> readDocument(Long documentId) {
        try {
            ResponseHandler<DocumentResponse> documentResponse = restClient.get()
                    .uri("/api/document/{documentId}", documentId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseHandler<DocumentResponse>>() {
                    });
            return Optional.of(documentResponse.getData());
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

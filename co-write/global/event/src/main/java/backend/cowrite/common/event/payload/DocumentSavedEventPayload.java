package backend.cowrite.common.event.payload;

import backend.cowrite.common.event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSavedEventPayload implements EventPayload {
    private Long documentId;
    private String title;
    private String password;
    private String content;
}

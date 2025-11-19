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
public class DocumentSaveEventPayload implements EventPayload {
    private String editedContent;
}
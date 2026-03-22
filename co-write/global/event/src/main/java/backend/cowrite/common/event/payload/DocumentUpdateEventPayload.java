package backend.cowrite.common.event.payload;

import backend.cowrite.common.event.EventPayload;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUpdateEventPayload implements EventPayload {
    private Long version;
    private String operationId;
    Operation operation;
}

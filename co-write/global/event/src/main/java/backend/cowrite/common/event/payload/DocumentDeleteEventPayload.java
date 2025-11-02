package backend.cowrite.common.event.payload;

import backend.cowrite.common.event.EventPayload;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDeleteEventPayload implements EventPayload {
    private Long version;
    private Long operationId;
    List<DeleteOperation> operations;
}

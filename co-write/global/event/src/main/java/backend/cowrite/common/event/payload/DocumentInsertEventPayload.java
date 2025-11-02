package backend.cowrite.common.event.payload;

import backend.cowrite.common.event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentInsertEventPayload implements EventPayload {
    private Long version;
    private Long operationId;
    List<InsertOperation> operations;
}

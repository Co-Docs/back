package backend.cowrite.common.event.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteOperation implements Operation{
    OperationType type = OperationType.DELETE;
    int targetPosition;
    int operationCount;
}

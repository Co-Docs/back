package backend.cowrite.common.event.payload;

public class DeleteOperation {
    OperationType type = OperationType.DELETE;
    int targetPosition;
    int operationCount;
}

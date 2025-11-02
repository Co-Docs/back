package backend.cowrite.common.event.payload;

public class InsertOperation {
    OperationType type = OperationType.INSERT;
    int targetPosition;
    String insertText;
}

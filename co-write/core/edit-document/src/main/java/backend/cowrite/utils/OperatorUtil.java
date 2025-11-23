package backend.cowrite.utils;

import backend.cowrite.common.dataserializer.DataSerializer;
import backend.cowrite.common.event.payload.DeleteOperation;
import backend.cowrite.common.event.payload.InsertOperation;
import backend.cowrite.common.event.payload.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OperatorUtil {

    private final OperatorRebaseUtil operatorRebaseUtil;

    public Operation rebaseOperation(Operation newOperation, List<String> executedOperations) {
        List<Operation> deserializedExecutedOperations = new ArrayList<>();
        for (String executedOperation : executedOperations) {
            Operation deserializedOperation = DataSerializer.deserialize(executedOperation, Operation.class);
            deserializedExecutedOperations.add(deserializedOperation);
        }
        return operatorRebaseUtil.rebase(newOperation, deserializedExecutedOperations);
    }

    public String operate(String savedContent, Operation executeOperation) {
        StringBuilder savedContentBuilder = new StringBuilder(savedContent == null ? "" : savedContent);
        if (executeOperation instanceof InsertOperation insertOperation) {
            int pos = validateIndex(insertOperation.getTargetPosition(), savedContentBuilder.length());
            String text = insertOperation.getInsertText() == null ? "" : insertOperation.getInsertText();
            savedContentBuilder.insert(pos, text);
        } else if (executeOperation instanceof DeleteOperation deleteOperation) {
            int start = validateIndex(deleteOperation.getTargetPosition(), savedContentBuilder.length() - 1);
            int count = Math.max(0, deleteOperation.getOperationCount());
            int end = Math.min(start + count, savedContentBuilder.length()); // exclusive
            if (start < end) {
                savedContentBuilder.delete(start, end);
            }
        }
        return savedContentBuilder.toString();
    }

    private int validateIndex(int index, int maxLength) {
        if (maxLength < 0) return 0;
        if (index < 0) return 0;
        if (index > maxLength) return maxLength;
        return index;
    }
}

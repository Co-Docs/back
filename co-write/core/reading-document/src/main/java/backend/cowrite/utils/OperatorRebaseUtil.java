package backend.cowrite.utils;

import backend.cowrite.common.event.payload.DeleteOperation;
import backend.cowrite.common.event.payload.InsertOperation;
import backend.cowrite.common.event.payload.Operation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OperatorRebaseUtil {

    public Operation rebase(Operation newOperator, List<Operation> executedOps) {
        if (newOperator == null) return null;
        Operation cur = copyOf(newOperator);
        if (executedOps == null || executedOps.isEmpty()) return cur;
        for (Operation applied : executedOps) {
            if (cur == null) break;
            cur = rebaseAgainst(cur, applied);
        }
        return cur;
    }

    private Operation rebaseAgainst(Operation mine, Operation other) {
        if (mine instanceof InsertOperation myIns) {
            if (other instanceof InsertOperation otIns) {
                return rebaseInsertAgainstInsert(myIns, otIns);
            } else if (other instanceof DeleteOperation otDel) {
                return rebaseInsertAgainstDelete(myIns, otDel);
            }
        } else if (mine instanceof DeleteOperation myDel) {
            if (other instanceof InsertOperation otIns) {
                return rebaseDeleteAgainstInsert(myDel, otIns);
            } else if (other instanceof DeleteOperation otDel) {
                return rebaseDeleteAgainstDelete(myDel, otDel);
            }
        }
        return mine;
    }

    private Operation rebaseInsertAgainstInsert(InsertOperation mine, InsertOperation other) {
        int q = nz(mine.getTargetPosition());
        int p = nz(other.getTargetPosition());
        int L = len(other.getInsertText());
        if (q >= p) q += L;
        return new InsertOperation(q, nzs(mine.getInsertText()));
    }

    private Operation rebaseInsertAgainstDelete(InsertOperation mine, DeleteOperation other) {
        int q = nz(mine.getTargetPosition());
        int p = nz(other.getTargetPosition());
        int K = Math.max(0, other.getOperationCount());
        if (q >= p + K) {
            q -= K;
        } else if (q >= p) {
            q = p;
        }
        return new InsertOperation(q, nzs(mine.getInsertText()));
    }

    private Operation rebaseDeleteAgainstInsert(DeleteOperation mine, InsertOperation other) {
        int q = nz(mine.getTargetPosition());
        int k = Math.max(0, mine.getOperationCount());
        int p = nz(other.getTargetPosition());
        int L = len(other.getInsertText());
        if (q >= p) q += L;
        if (p >= q && p < q + k) k += L;
        if (k <= 0) return null;
        return new DeleteOperation(q, k);
    }

    private Operation rebaseDeleteAgainstDelete(DeleteOperation mine, DeleteOperation other) {
        int q = nz(mine.getTargetPosition());
        int k = Math.max(0, mine.getOperationCount());
        int p = nz(other.getTargetPosition());
        int K = Math.max(0, other.getOperationCount());
        int pEnd = p + K;

        if (q >= pEnd) {
            q -= K;
        } else if (q >= p) {
            q = p;
        }

        int ov = rawOverlapForDeleteDelete(mine, other);
        k -= ov;
        if (k <= 0) return null;
        return new DeleteOperation(q, k);
    }

    private int rawOverlapForDeleteDelete(DeleteOperation mine, DeleteOperation other) {
        int q = nz(mine.getTargetPosition());
        int k = Math.max(0, mine.getOperationCount());
        int p = nz(other.getTargetPosition());
        int K = Math.max(0, other.getOperationCount());
        int a1 = q, a2 = q + k;
        int b1 = p, b2 = p + K;
        int left = Math.max(a1, b1);
        int right = Math.min(a2, b2);
        int overlap = right - left;
        return Math.max(0, overlap);
    }

    private int nz(Integer v) { return v == null ? 0 : v; }
    private String nzs(String s) { return s == null ? "" : s; }
    private int len(String s) { return s == null ? 0 : s.length(); }

    private Operation copyOf(Operation op) {
        if (op instanceof InsertOperation ins) {
            return new InsertOperation(nz(ins.getTargetPosition()), nzs(ins.getInsertText()));
        } else if (op instanceof DeleteOperation del) {
            return new DeleteOperation(nz(del.getTargetPosition()), Math.max(0, del.getOperationCount()));
        }
        return op;
    }
}

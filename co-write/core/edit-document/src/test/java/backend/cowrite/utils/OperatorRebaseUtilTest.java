package backend.cowrite.utils;

import backend.cowrite.common.event.payload.DeleteOperation;
import backend.cowrite.common.event.payload.InsertOperation;
import backend.cowrite.common.event.payload.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorRebaseUtilTest {

    private OperatorRebaseUtil rebaseUtil;

    @BeforeEach
    void setUp() {
        rebaseUtil = new OperatorRebaseUtil();
    }

    // ─────────────────────────────────────────────────────────────
    // Insert vs Insert
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Insert vs Insert 리베이스")
    class InsertAgainstInsert {

        @Test
        @DisplayName("내 삽입 위치가 상대 삽입 위치보다 뒤면 → 상대 텍스트 길이만큼 뒤로 밀린다")
        void myPositionAfterOther_shiftsByOtherLength() {
            InsertOperation mine = new InsertOperation(7, "hi", "session-A");
            InsertOperation other = new InsertOperation(3, "abc", "session-B");

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(InsertOperation.class);
            InsertOperation insert = (InsertOperation) result;
            assertThat(insert.getTargetPosition()).isEqualTo(10); // 7 + 3
            assertThat(insert.getInsertText()).isEqualTo("hi");
            assertThat(insert.getSessionId()).isEqualTo("session-A");
        }

        @Test
        @DisplayName("내 삽입 위치가 상대 삽입 위치보다 앞이면 → 위치 변화 없음")
        void myPositionBeforeOther_noShift() {
            InsertOperation mine = new InsertOperation(2, "hi", "session-A");
            InsertOperation other = new InsertOperation(5, "abc", "session-B");

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(InsertOperation.class);
            assertThat(((InsertOperation) result).getTargetPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("같은 위치, 내 sessionId가 더 크면 → 상대 텍스트 길이만큼 밀린다")
        void samePosition_mySessionIdGreater_shiftsByOtherLength() {
            InsertOperation mine = new InsertOperation(5, "x", "session-Z");
            InsertOperation other = new InsertOperation(5, "abc", "session-A");

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(InsertOperation.class);
            assertThat(((InsertOperation) result).getTargetPosition()).isEqualTo(8); // 5 + 3
        }

        @Test
        @DisplayName("같은 위치, 내 sessionId가 더 작으면 → 위치 변화 없음 (먼저 삽입)")
        void samePosition_mySessionIdSmaller_noShift() {
            InsertOperation mine = new InsertOperation(5, "x", "session-A");
            InsertOperation other = new InsertOperation(5, "abc", "session-Z");

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(InsertOperation.class);
            assertThat(((InsertOperation) result).getTargetPosition()).isEqualTo(5);
        }

        @Test
        @DisplayName("같은 위치, sessionId가 동일하면 → 위치 변화 없음")
        void samePosition_sameSessionId_noShift() {
            InsertOperation mine = new InsertOperation(5, "x", "session-A");
            InsertOperation other = new InsertOperation(5, "abc", "session-A");

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(InsertOperation.class);
            assertThat(((InsertOperation) result).getTargetPosition()).isEqualTo(5);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Insert vs Delete
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Insert vs Delete 리베이스")
    class InsertAgainstDelete {

        @Test
        @DisplayName("삽입 위치가 삭제 범위 완전히 뒤라면 → 삭제 개수만큼 앞으로 당겨진다")
        void insertAfterDeleteRange_shiftLeft() {
            InsertOperation mine = new InsertOperation(10, "x", "session-A");
            DeleteOperation other = new DeleteOperation(3, 4); // 삭제 [3,7)

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(InsertOperation.class);
            assertThat(((InsertOperation) result).getTargetPosition()).isEqualTo(6); // 10 - 4
        }

        @Test
        @DisplayName("삽입 위치가 삭제 범위 안이라면 → 삭제 시작 위치로 이동")
        void insertInsideDeleteRange_moveToDeleteStart() {
            InsertOperation mine = new InsertOperation(5, "x", "session-A");
            DeleteOperation other = new DeleteOperation(3, 5); // 삭제 [3,8)

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(InsertOperation.class);
            assertThat(((InsertOperation) result).getTargetPosition()).isEqualTo(3);
        }

        @Test
        @DisplayName("삽입 위치가 삭제 시작과 동일하면 → 삭제 시작 위치로 이동 (범위 내 취급)")
        void insertAtDeleteStart_moveToDeleteStart() {
            InsertOperation mine = new InsertOperation(3, "x", "session-A");
            DeleteOperation other = new DeleteOperation(3, 5); // 삭제 [3,8)

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(InsertOperation.class);
            assertThat(((InsertOperation) result).getTargetPosition()).isEqualTo(3);
        }

        @Test
        @DisplayName("삽입 위치가 삭제 범위보다 앞이면 → 위치 변화 없음")
        void insertBeforeDeleteRange_noShift() {
            InsertOperation mine = new InsertOperation(1, "x", "session-A");
            DeleteOperation other = new DeleteOperation(3, 4); // 삭제 [3,7)

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(InsertOperation.class);
            assertThat(((InsertOperation) result).getTargetPosition()).isEqualTo(1);
        }

        @Test
        @DisplayName("삽입 위치가 삭제 범위 끝과 같으면 → 삭제 개수만큼 앞으로 당겨진다")
        void insertAtDeleteEnd_shiftLeft() {
            InsertOperation mine = new InsertOperation(7, "x", "session-A");
            DeleteOperation other = new DeleteOperation(3, 4); // 삭제 [3,7)

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(InsertOperation.class);
            assertThat(((InsertOperation) result).getTargetPosition()).isEqualTo(3); // 7 - 4
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Delete vs Insert
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Delete vs Insert 리베이스")
    class DeleteAgainstInsert {

        @Test
        @DisplayName("삭제 위치가 삽입 위치보다 뒤라면 → 삽입 텍스트 길이만큼 뒤로 밀린다")
        void deleteAfterInsert_shiftRight() {
            DeleteOperation mine = new DeleteOperation(6, 3);
            InsertOperation other = new InsertOperation(3, "ab", "session-B");

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(DeleteOperation.class);
            DeleteOperation del = (DeleteOperation) result;
            assertThat(del.getTargetPosition()).isEqualTo(8); // 6 + 2
            assertThat(del.getOperationCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("삽입이 삭제 범위 내부라면 → 삭제 개수가 삽입 텍스트 길이만큼 늘어난다")
        void insertInsideDeleteRange_expandDeleteCount() {
            DeleteOperation mine = new DeleteOperation(3, 5); // 삭제 [3,8)
            InsertOperation other = new InsertOperation(5, "xyz", "session-B"); // 5 ∈ [3,8)

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(DeleteOperation.class);
            DeleteOperation del = (DeleteOperation) result;
            assertThat(del.getTargetPosition()).isEqualTo(3);
            assertThat(del.getOperationCount()).isEqualTo(8); // 5 + 3
        }

        @Test
        @DisplayName("삽입이 삭제 범위 앞이면 → 삭제 위치만 뒤로 이동, 개수 불변")
        void insertBeforeDeleteRange_onlyShiftPosition() {
            DeleteOperation mine = new DeleteOperation(5, 3);
            InsertOperation other = new InsertOperation(2, "ab", "session-B");

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(DeleteOperation.class);
            DeleteOperation del = (DeleteOperation) result;
            assertThat(del.getTargetPosition()).isEqualTo(7); // 5 + 2
            assertThat(del.getOperationCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("삽입이 삭제 범위 뒤라면 → 아무 변화 없음")
        void insertAfterDeleteRange_noChange() {
            DeleteOperation mine = new DeleteOperation(2, 3); // 삭제 [2,5)
            InsertOperation other = new InsertOperation(8, "xyz", "session-B");

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(DeleteOperation.class);
            DeleteOperation del = (DeleteOperation) result;
            assertThat(del.getTargetPosition()).isEqualTo(2);
            assertThat(del.getOperationCount()).isEqualTo(3);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Delete vs Delete
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Delete vs Delete 리베이스")
    class DeleteAgainstDelete {

        @Test
        @DisplayName("겹침 없음, 내 삭제가 뒤 → 상대 삭제 개수만큼 앞으로 당겨진다")
        void noOverlap_myDeleteAfter_shiftLeft() {
            DeleteOperation mine = new DeleteOperation(8, 3); // [8,11)
            DeleteOperation other = new DeleteOperation(2, 4); // [2,6)

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(DeleteOperation.class);
            DeleteOperation del = (DeleteOperation) result;
            assertThat(del.getTargetPosition()).isEqualTo(4); // 8 - 4
            assertThat(del.getOperationCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("겹침 없음, 내 삭제가 앞 → 아무 변화 없음")
        void noOverlap_myDeleteBefore_noChange() {
            DeleteOperation mine = new DeleteOperation(1, 3); // [1,4)
            DeleteOperation other = new DeleteOperation(6, 4); // [6,10)

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(DeleteOperation.class);
            DeleteOperation del = (DeleteOperation) result;
            assertThat(del.getTargetPosition()).isEqualTo(1);
            assertThat(del.getOperationCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("내 삭제가 상대 삭제 범위 완전히 안 → null 반환 (이미 지워진 문자들)")
        void myDeleteCompletelyInsideOther_returnsNull() {
            DeleteOperation mine = new DeleteOperation(3, 3); // [3,6)
            DeleteOperation other = new DeleteOperation(2, 6); // [2,8) — mine 포함

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("내 삭제와 상대 삭제가 완전히 동일 → null 반환")
        void identicalDeleteRanges_returnsNull() {
            DeleteOperation mine = new DeleteOperation(3, 4); // [3,7)
            DeleteOperation other = new DeleteOperation(3, 4); // [3,7)

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("내 삭제가 상대 범위 앞에서 시작해 일부 겹침 → 겹친 부분 제거, 위치 불변")
        void partialOverlap_myDeleteStartsBefore_trimRight() {
            DeleteOperation mine = new DeleteOperation(1, 5); // [1,6)
            DeleteOperation other = new DeleteOperation(4, 4); // [4,8)  → overlap = [4,6) = 2

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(DeleteOperation.class);
            DeleteOperation del = (DeleteOperation) result;
            assertThat(del.getTargetPosition()).isEqualTo(1);
            assertThat(del.getOperationCount()).isEqualTo(3); // 5 - 2
        }

        @Test
        @DisplayName("내 삭제가 상대 범위 안에서 시작해 밖에서 끝남 → 겹친 부분 제거, 위치 이동")
        void partialOverlap_myDeleteStartsInside_trimLeft() {
            DeleteOperation mine = new DeleteOperation(4, 5); // [4,9)
            DeleteOperation other = new DeleteOperation(2, 4); // [2,6)  → overlap = [4,6) = 2

            Operation result = rebaseUtil.rebase(mine, List.of(other));

            assertThat(result).isInstanceOf(DeleteOperation.class);
            DeleteOperation del = (DeleteOperation) result;
            assertThat(del.getTargetPosition()).isEqualTo(2); // q=4, inside [2,6) → q = p = 2
            assertThat(del.getOperationCount()).isEqualTo(3); // 5 - 2
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 복합 리베이스 (순서 중요)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("복합 리베이스 (여러 operation)")
    class MultipleOperationsRebase {

        @Test
        @DisplayName("여러 연산에 대해 순서대로 리베이스가 적용된다")
        void rebaseAgainstMultipleOps() {
            // "hello world" (11자)
            // op1: insert "XX" at 5 → "helloXX world" (13자)
            // op2: insert "YY" at 0 → "YYhelloXX world" (15자)
            // mine: insert "Z" at 6 (original version) → 최종 어디에 삽입되어야 하나?
            //   after op1 (insert at 5, L=2): mine.pos(6) > op1.pos(5) → 6+2=8
            //   after op2 (insert at 0, L=2): mine.pos(8) > op2.pos(0) → 8+2=10
            InsertOperation mine = new InsertOperation(6, "Z", "session-A");
            InsertOperation op1 = new InsertOperation(5, "XX", "session-B");
            InsertOperation op2 = new InsertOperation(0, "YY", "session-C");

            Operation result = rebaseUtil.rebase(mine, List.of(op1, op2));

            assertThat(result).isInstanceOf(InsertOperation.class);
            assertThat(((InsertOperation) result).getTargetPosition()).isEqualTo(10);
        }

        @Test
        @DisplayName("null operation 입력 → null 반환")
        void nullOperation_returnsNull() {
            Operation result = rebaseUtil.rebase(null, List.of(new InsertOperation(0, "x", "s")));
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 executedOps → operation 그대로 반환")
        void emptyExecutedOps_returnsCopyOfMine() {
            InsertOperation mine = new InsertOperation(5, "abc", "session-A");

            Operation result = rebaseUtil.rebase(mine, List.of());

            assertThat(result).isInstanceOf(InsertOperation.class);
            InsertOperation insert = (InsertOperation) result;
            assertThat(insert.getTargetPosition()).isEqualTo(5);
            assertThat(insert.getInsertText()).isEqualTo("abc");
        }

        @Test
        @DisplayName("null executedOps → operation 그대로 반환")
        void nullExecutedOps_returnsCopyOfMine() {
            InsertOperation mine = new InsertOperation(3, "hi", "session-A");

            Operation result = rebaseUtil.rebase(mine, null);

            assertThat(result).isInstanceOf(InsertOperation.class);
            assertThat(((InsertOperation) result).getTargetPosition()).isEqualTo(3);
        }
    }
}

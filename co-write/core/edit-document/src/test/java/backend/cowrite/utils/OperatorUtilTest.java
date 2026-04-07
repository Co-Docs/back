package backend.cowrite.utils;

import backend.cowrite.common.event.payload.DeleteOperation;
import backend.cowrite.common.event.payload.InsertOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorUtilTest {

    private OperatorUtil operatorUtil;

    @BeforeEach
    void setUp() {
        operatorUtil = new OperatorUtil(new OperatorRebaseUtil());
    }

    // ─────────────────────────────────────────────────────────────
    // Insert 연산 (operate)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("InsertOperation으로 operate()")
    class InsertOperate {

        @Test
        @DisplayName("중간 위치에 삽입")
        void insertInMiddle() {
            String result = operatorUtil.operate("hello world", new InsertOperation(5, " beautiful", "s"));
            assertThat(result).isEqualTo("hello beautiful world");
        }

        @Test
        @DisplayName("맨 앞(0)에 삽입")
        void insertAtStart() {
            String result = operatorUtil.operate("world", new InsertOperation(0, "hello ", "s"));
            assertThat(result).isEqualTo("hello world");
        }

        @Test
        @DisplayName("맨 끝(length)에 삽입")
        void insertAtEnd() {
            String result = operatorUtil.operate("hello", new InsertOperation(5, " world", "s"));
            assertThat(result).isEqualTo("hello world");
        }

        @Test
        @DisplayName("위치가 길이보다 크면 → 맨 끝에 삽입")
        void insertPositionBeyondLength_clampedToEnd() {
            String result = operatorUtil.operate("hello", new InsertOperation(100, "!", "s"));
            assertThat(result).isEqualTo("hello!");
        }

        @Test
        @DisplayName("위치가 음수이면 → 맨 앞에 삽입")
        void insertNegativePosition_clampedToStart() {
            String result = operatorUtil.operate("hello", new InsertOperation(-1, "X", "s"));
            assertThat(result).isEqualTo("Xhello");
        }

        @Test
        @DisplayName("savedContent가 null이면 → 빈 문자열에서 삽입")
        void insertIntoNullContent() {
            String result = operatorUtil.operate(null, new InsertOperation(0, "hello", "s"));
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("빈 텍스트 삽입 → 원본 그대로")
        void insertEmptyText() {
            String result = operatorUtil.operate("hello", new InsertOperation(2, "", "s"));
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("null 텍스트 삽입 → 원본 그대로")
        void insertNullText() {
            String result = operatorUtil.operate("hello", new InsertOperation(2, null, "s"));
            assertThat(result).isEqualTo("hello");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Delete 연산 (operate)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DeleteOperation으로 operate()")
    class DeleteOperate {

        @Test
        @DisplayName("중간 범위 삭제")
        void deleteMiddleRange() {
            String result = operatorUtil.operate("hello world", new DeleteOperation(5, 6));
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("맨 앞에서 삭제")
        void deleteFromStart() {
            String result = operatorUtil.operate("hello world", new DeleteOperation(0, 6));
            assertThat(result).isEqualTo("world");
        }

        @Test
        @DisplayName("끝까지 삭제 (count가 나머지 전부)")
        void deleteToEnd() {
            String result = operatorUtil.operate("hello world", new DeleteOperation(5, 6));
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("count가 문자열 끝을 초과해도 → 끝까지만 삭제")
        void deleteCountExceedsLength_clampedToEnd() {
            String result = operatorUtil.operate("hello", new DeleteOperation(2, 100));
            assertThat(result).isEqualTo("he");
        }

        @Test
        @DisplayName("count가 0이면 → 아무것도 삭제하지 않음")
        void deleteCountZero_noChange() {
            String result = operatorUtil.operate("hello", new DeleteOperation(2, 0));
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("count가 음수이면 → 아무것도 삭제하지 않음")
        void deleteCountNegative_noChange() {
            String result = operatorUtil.operate("hello", new DeleteOperation(2, -1));
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("위치가 길이를 초과하면 → 아무것도 삭제하지 않음 (start == end == length)")
        void deletePositionBeyondLength_noChange() {
            // validateIndex(10, length=5) = 5, end = min(5+2, 5) = 5 → start == end → no deletion
            String result = operatorUtil.operate("hello", new DeleteOperation(10, 2));
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("savedContent가 null이면 → 빈 문자열에서 삭제 → 빈 문자열 반환")
        void deleteFromNullContent() {
            String result = operatorUtil.operate(null, new DeleteOperation(0, 3));
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("단일 문자 삭제")
        void deleteSingleChar() {
            String result = operatorUtil.operate("hello", new DeleteOperation(2, 1));
            assertThat(result).isEqualTo("helo");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 동시편집 시나리오 (operate 연속 적용)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("연속 편집 시나리오")
    class SequentialEditScenario {

        @Test
        @DisplayName("삽입 후 삭제 → 올바른 최종 상태")
        void insertThenDelete() {
            String afterInsert = operatorUtil.operate("hello", new InsertOperation(5, " world", "s"));
            String afterDelete = operatorUtil.operate(afterInsert, new DeleteOperation(5, 6));
            assertThat(afterDelete).isEqualTo("hello");
        }

        @Test
        @DisplayName("삭제 후 삽입 → 올바른 최종 상태")
        void deleteThenInsert() {
            String afterDelete = operatorUtil.operate("hello world", new DeleteOperation(5, 6));
            String afterInsert = operatorUtil.operate(afterDelete, new InsertOperation(5, "!", "s"));
            assertThat(afterInsert).isEqualTo("hello!");
        }
    }
}

package backend.cowrite.common.event.payload;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OperationType {
    DELETE,
    INSERT
}

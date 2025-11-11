package backend.cowrite.common.event.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InsertOperation implements Operation{
    int targetPosition;
    String insertText;
}

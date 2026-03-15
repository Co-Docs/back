package backend.cowrite.service.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public record DocumentPreviewResponse(
        String title,
        List<String> userNicknames
) {

}
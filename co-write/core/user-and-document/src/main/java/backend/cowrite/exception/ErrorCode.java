package backend.cowrite.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    TEMPORARY_ERROR(500,"C_000","임시 에러코드입니다.");

    private final int state;
    private final String code;
    private final String message;

}

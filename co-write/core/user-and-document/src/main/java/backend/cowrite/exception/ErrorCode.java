package backend.cowrite.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    TEMPORARY_ERROR(500,"C_000","임시 에러코드입니다."),
    USER_NOT_FOUND(400, "C_001", "해당하는 유저 정보가 존재하지 않습니다."),
    DOCS_NOT_FOUND(400, "C_002", "해당하는 문서 정보가 존재하지 않습니다.");


    private final int state;
    private final String code;
    private final String message;

}

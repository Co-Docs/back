package backend.cowrite.exception;

import backend.cowrite.common.ResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ResponseHandler<ErrorResponse>> handleException(Exception e) {
        log.info("예외 발생: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.TEMPORARY_ERROR);
        return ResponseEntity.ok(ResponseHandler.fail(errorResponse,e.getMessage()));
    }
}

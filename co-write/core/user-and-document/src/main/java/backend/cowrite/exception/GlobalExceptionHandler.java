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
    protected ResponseHandler<ResponseEntity<ErrorResponse>> handleException(Exception e) {
        log.info("예외 발생: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.TEMPORARY_ERROR);
        return ResponseHandler.fail(ResponseEntity.ok(errorResponse),e.getMessage());
    }
}

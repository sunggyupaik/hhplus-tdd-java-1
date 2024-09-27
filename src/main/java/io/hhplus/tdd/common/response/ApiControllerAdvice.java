package io.hhplus.tdd.common.response;

import io.hhplus.tdd.common.exception.ExceededBalanceException;
import io.hhplus.tdd.common.exception.InsufficientBalanceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
    }

    @ExceptionHandler(value = InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalanceException(Exception e) {
        return ResponseEntity.status(400).body(new ErrorResponse("400", e.getMessage()));
    }

    @ExceptionHandler(value = ExceededBalanceException.class)
    public ResponseEntity<ErrorResponse> handleExceededBalanceException(Exception e) {
        return ResponseEntity.status(400).body(new ErrorResponse("400", e.getMessage()));
    }
}

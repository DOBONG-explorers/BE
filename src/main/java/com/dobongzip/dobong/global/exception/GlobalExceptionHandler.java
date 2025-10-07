package com.dobongzip.dobong.global.exception;

import com.dobongzip.dobong.global.response.CommonResponse;
import com.dobongzip.dobong.global.response.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        logError(ex, request);
        StatusCode code = ex.getStatusCode();
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(CommonResponse.onFailure(code));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CommonResponse<Void>> handleCustomException(CustomException ex, HttpServletRequest request) {
        logError(ex, request);
        StatusCode code = ex.getStatusCode();
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(CommonResponse.onFailure(code));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        logError(e, request);
        return ResponseEntity
                .status(StatusCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(CommonResponse.onFailure(StatusCode.INTERNAL_SERVER_ERROR));
    }

    private void logError(Exception e, HttpServletRequest request) {
        log.error("Request URI : [{}] {}", request.getMethod(), request.getRequestURI());
        log.error("Exception : ", e);
        log.error(e.getMessage());
    }
}

package com.dobongzip.dobong.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {

    private final boolean success;
    private final int httpStatus;
    private final String message;
    private final T data;

    private CommonResponse(boolean success, int httpStatus, String message, T data) {
        this.success = success;
        this.httpStatus = httpStatus;
        this.message = message;
        this.data = data;
    }

    // 성공 + 데이터 포함
    public static <T> CommonResponse<T> onSuccess(T data) {
        return new CommonResponse<>(true, HttpStatus.OK.value(), "요청이 성공적으로 처리되었습니다.", data);
    }

    // 성공 + 데이터 없음
    public static CommonResponse<Void> onSuccess() {
        return new CommonResponse<>(true, HttpStatus.OK.value(), "요청이 성공적으로 처리되었습니다.", null);
    }

    // 실패 응답
    public static CommonResponse<Void> onFailure(StatusCode statusCode) {
        return new CommonResponse<>(false, statusCode.getHttpStatus().value(), statusCode.getDescription(), null);
    }
}

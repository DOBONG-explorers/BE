package com.dobongzip.dobong.global.response;


import org.springframework.http.HttpStatus;

public enum StatusCode {
    SUCCESS(HttpStatus.OK, "SUCCESS200", "SUCCESS"),

    // StatusCode.java
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "AUTH4001", "비밀번호는 6~20자이며, 영문 대/소문자 및 특수문자 중 2가지 이상을 포함해야 합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH404", "존재하지 않는 사용자입니다."),
    PROFILE_NOT_COMPLETED(HttpStatus.OK, "AUTH2001", "프로필 정보가 아직 입력되지 않았습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON4001", "잘못된 요청입니다."),


    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH401", "이메일 또는 비밀번호가 일치하지 않습니다."),
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "AUTH4002", "이미 등록된 사용자입니다."),
    FAILURE_TEST(HttpStatus.INTERNAL_SERVER_ERROR, "TEST001", "테스트 실패 응답입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5001", "서버에서 알 수 없는 에러가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String description;

    StatusCode(HttpStatus httpStatus, String code, String description) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.description = description;
    }

    public HttpStatus getHttpStatus() { return this.httpStatus; }
    public String getCode() { return this.code; }
    public String getDescription() { return this.description; }
}



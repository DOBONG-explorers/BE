package com.dobongzip.dobong.global.response;

import org.springframework.http.HttpStatus;

public enum StatusCode {
    SUCCESS(HttpStatus.OK, "SUCCESS200", "SUCCESS"),

    // ── 인증/회원 ─────────────────────────────────────────────────────────────
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "AUTH4001", "비밀번호는 6~20자이며, 영문 대/소문자 및 특수문자 중 2가지 이상을 포함해야 합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH404", "존재하지 않는 사용자입니다."),
    PROFILE_NOT_COMPLETED(HttpStatus.OK, "AUTH2001", "프로필 정보가 아직 입력되지 않았습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH401", "이메일 또는 비밀번호가 일치하지 않습니다."),
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "AUTH4002", "이미 등록된 사용자입니다."),

    // ── 공통 요청/검증 ────────────────────────────────────────────────────────
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON4001", "잘못된 요청입니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "COMMON4002", "날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)"),

    // ── 리뷰 ─────────────────────────────────────────────────────────────────
    REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "REVIEW4001", "이미 이 장소에 작성한 리뷰가 있습니다. 수정 기능을 이용하세요."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404", "리뷰를 찾을 수 없습니다."),
    REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "REVIEW403", "리뷰에 대한 권한이 없습니다."),
    REVIEW_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "REVIEW401", "로그인이 필요합니다."),

    // ── 유저 관리/소셜 ───────────────────────────────────────────────────────
    USER_NOT_FOUND_BY_EMAIL(HttpStatus.BAD_REQUEST, "USER4004", "해당 이메일로 가입된 사용자가 없습니다."),
    NOT_ALLOWED_FOR_SOCIAL_LOGIN(HttpStatus.FORBIDDEN, "USER4031", "소셜 로그인 계정은 비밀번호를 설정할 수 없습니다."),
    EMAIL_NOT_PROVIDED(HttpStatus.UNAUTHORIZED, "SOCIAL4011", "소셜 로그인 시 이메일 제공에 동의해야 합니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "AUTH4003", "현재 비밀번호가 일치하지 않습니다."), // ← 코드 중복 방지로 AUTH4003 사용 권장
    PASSWORD_CONFIRM_NOT_MATCH(HttpStatus.BAD_REQUEST, "AUTH4004", "새 비밀번호 확인이 일치하지 않습니다."), // ← 코드 중복 방지로 AUTH4004

    // ── 외부 API(서울시/도봉구) ──────────────────────────────────────────────
    SEOUL_EVENT_API_FAILED(HttpStatus.BAD_GATEWAY, "EXT5021", "서울시 문화행사 API 호출에 실패했습니다."),
    SEOUL_EVENT_API_BAD_RESPONSE(HttpStatus.BAD_GATEWAY, "EXT5022", "서울시 문화행사 API 응답 형식이 올바르지 않습니다."),
    DOBONG_OPENAPI_FAILED(HttpStatus.BAD_GATEWAY, "EXT5023", "도봉구 오픈API 호출에 실패했습니다."),
    DOBONG_OPENAPI_BAD_RESPONSE(HttpStatus.BAD_GATEWAY, "EXT5024", "도봉구 오픈API 응답 형식이 올바르지 않습니다."),

    // ── 기타 ────────────────────────────────────────────────────────────────
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

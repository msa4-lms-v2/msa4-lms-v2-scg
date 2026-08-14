package com.msa4lmsv2scg.global.response.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CustomResponseCode {
    SUCCESS(HttpStatus.OK, "00", "정상 처리되었습니다.")
    , LOGIN_FAILED_ERROR(HttpStatus.UNAUTHORIZED, "E01", "로그인에 실패했습니다.")
    , UNAUTHENTICATED_ERROR(HttpStatus.UNAUTHORIZED, "E02", "인증이 필요합니다.")
    , FORBIDDEN_ERROR(HttpStatus.FORBIDDEN, "E03", "요청을 수행할 권한이 없습니다.")
    , INVALID_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "E04", "유효하지 않은 토큰입니다.")
    , DATA_NOT_FOUND_ERROR(HttpStatus.NOT_FOUND, "E10", "데이터를 찾을 수 없습니다.")
    , DUPLICATE_ERROR(HttpStatus.CONFLICT, "E11", "중복된 데이터입니다.")
    , VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "E21", "입력값 검증에 실패했습니다.")
    , DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E80", "데이터베이스 오류가 발생했습니다.")
    , SERVICE_UNAVAILABLE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E90", "연동 서비스를 사용할 수 없습니다.")
    , SERVICE_TIMEOUT_ERROR(HttpStatus.GATEWAY_TIMEOUT, "E91", "연동 서비스 응답 시간이 초과되었습니다.")
    , CIRCUIT_BREAKER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E92", "서킷 브레이커가 요청을 차단했습니다.")
    , RECOVERY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E93", "복구 처리가 필요합니다.")
    , MANUAL_ACTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E94", "수동 처리가 필요합니다.")
    , SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E99", "시스템 오류가 발생했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    CustomResponseCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}

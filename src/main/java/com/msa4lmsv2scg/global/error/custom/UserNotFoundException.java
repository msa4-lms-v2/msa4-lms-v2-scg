package com.msa4lmsv2scg.global.error.custom;

public class UserNotFoundException extends RuntimeException {
    // 커스텀 에러는 주로 RuntimeException 상속받음
    public UserNotFoundException(String message) {
        super(message);
    }
}

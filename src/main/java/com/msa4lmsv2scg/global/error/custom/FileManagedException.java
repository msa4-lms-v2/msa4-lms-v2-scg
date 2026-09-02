package com.msa4lmsv2scg.global.error.custom;

public class FileManagedException extends RuntimeException {
    // 커스텀 에러는 주로 RuntimeException 상속받음
    public FileManagedException(String message) {
        super(message);
    }
}

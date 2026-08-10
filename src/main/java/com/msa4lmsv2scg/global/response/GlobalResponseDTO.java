package com.msa4lmsv2scg.global.response;

import com.msa4lmsv2scg.global.response.constant.CustomResponseCode;

public record GlobalResponseDTO<T> (
        String code
        , String message
        , T data
){
    public static <T> GlobalResponseDTO<T> from(CustomResponseCode customResponseCode, T data){
        return new GlobalResponseDTO<T>(customResponseCode.getCode(), customResponseCode.name(), data);
    }

    // data가 null인 경우
    public static GlobalResponseDTO<Void> from(CustomResponseCode customResponseCode){
        return new GlobalResponseDTO<Void>(customResponseCode.getCode(), customResponseCode.name(), null);
    }

    // SCG_SUCCESS
    public static <T> GlobalResponseDTO<T> SCG_SUCCESS(T data){
        return GlobalResponseDTO.<T>from(CustomResponseCode.SCG_SUCCESS, data);
        // return new GlobalResponseDTO<T>(CustomResponseCode.SCG_SUCCESS.getCode(), CustomResponseCode.SCG_SUCCESS.name(), data);
    }

    // data가 없는 SCG_SUCCESS 패턴
    public static GlobalResponseDTO<Void> SCG_SUCCESS(){
        return GlobalResponseDTO.<Void>from(CustomResponseCode.SCG_SUCCESS);
        // return new GlobalResponseDTO<Void>(CustomResponseCode.SCG_SUCCESS.getCode(), CustomResponseCode.SCG_SUCCESS.name(), null);
    }
}

package com.msa4lmsv2scg.global.response.constant;

import com.msa4lmsv2scg.global.response.GlobalResponseDTO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomResponseCodeTest {

    @Test
    void exposesOnlyCommonErrorContractCodes() {
        Map<CustomResponseCode, String> expectedCodes = Map.ofEntries(
                Map.entry(CustomResponseCode.SUCCESS, "00"),
                Map.entry(CustomResponseCode.LOGIN_FAILED_ERROR, "E01"),
                Map.entry(CustomResponseCode.UNAUTHENTICATED_ERROR, "E02"),
                Map.entry(CustomResponseCode.FORBIDDEN_ERROR, "E03"),
                Map.entry(CustomResponseCode.INVALID_TOKEN_ERROR, "E04"),
                Map.entry(CustomResponseCode.DATA_NOT_FOUND_ERROR, "E10"),
                Map.entry(CustomResponseCode.DUPLICATE_ERROR, "E11"),
                Map.entry(CustomResponseCode.VALIDATION_ERROR, "E21"),
                Map.entry(CustomResponseCode.DB_ERROR, "E80"),
                Map.entry(CustomResponseCode.SERVICE_UNAVAILABLE_ERROR, "E90"),
                Map.entry(CustomResponseCode.SERVICE_TIMEOUT_ERROR, "E91"),
                Map.entry(CustomResponseCode.CIRCUIT_BREAKER_ERROR, "E92"),
                Map.entry(CustomResponseCode.RECOVERY_ERROR, "E93"),
                Map.entry(CustomResponseCode.MANUAL_ACTION_ERROR, "E94"),
                Map.entry(CustomResponseCode.SYSTEM_ERROR, "E99")
        );

        assertThat(CustomResponseCode.values()).containsExactlyInAnyOrderElementsOf(expectedCodes.keySet());
        expectedCodes.forEach((responseCode, expectedCode) ->
                assertThat(responseCode.getCode()).isEqualTo(expectedCode));
    }

    @Test
    void responseUsesClientMessageInsteadOfEnumName() {
        GlobalResponseDTO<Void> response = GlobalResponseDTO.from(CustomResponseCode.SERVICE_TIMEOUT_ERROR);

        assertThat(response.code()).isEqualTo("E91");
        assertThat(response.message()).isEqualTo("연동 서비스 응답 시간이 초과되었습니다.");
    }
}

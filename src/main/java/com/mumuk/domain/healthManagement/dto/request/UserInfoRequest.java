package com.mumuk.domain.healthManagement.dto.request;

import com.mumuk.domain.healthManagement.entity.Gender;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class UserInfoRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class UserInfoReq {

        private Gender gender;
        @NotNull
        @Min(value = 1, message = "키는 1cm 이상이어야 합니다")
        private Double height;
        @NotNull
        @Min(value = 1, message = "몸무게는 1kg 이상이어야 합니다")
        private Double weight;
    }


}

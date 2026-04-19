package com.mumuk.domain.ingredient.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mumuk.domain.ingredient.entity.DdayFcmSetting;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.ingredient.entity.IngredientNotification;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

public class IngredientRequest {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterReq {
        @NotNull
        @JsonProperty("name")
        private String name;

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate expireDate;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateExpireDateReq {

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate expireDate;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateDdaySettingReq{

        @NotNull
        private List<DdayFcmSetting> daySetting;
    }
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateQuantityReq{

        @NotNull
        private Integer quantity;
    }
}

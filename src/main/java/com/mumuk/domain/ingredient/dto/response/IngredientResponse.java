package com.mumuk.domain.ingredient.dto.response;

import com.mumuk.domain.ingredient.entity.Ingredient;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class IngredientResponse {

    @Getter
    @AllArgsConstructor
    public static class RetrieveRes {
        private Long ingredient_id;
        private String name;
        private LocalDate expireDate;
        private LocalDateTime createdAt;
        private Integer quantity;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ExpireDateManegeRes {
        private String name;
        private LocalDate expireDate;
        private String dDay;


        public ExpireDateManegeRes(String name, LocalDate expireDate, String dDay) {
            this.name = name;
            this.expireDate = expireDate;
            this.dDay = dDay;
        }
    }

}

package com.mumuk.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class UserRecipeRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ClickLikeReq{

        @NotNull
        private Long recipeId;
    }
}

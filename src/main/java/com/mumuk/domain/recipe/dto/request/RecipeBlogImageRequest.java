package com.mumuk.domain.recipe.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RecipeBlogImageRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchImageReq {

        @NotBlank(message = "레시피명은 필수입니다")
        @Size(max = 100, message = "레시피명은 100자를 초과할 수 없습니다")
        private String recipeName;

        // 강제 새로고침 여부
        private boolean forceRefresh = false;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshImageReq {

        @NotBlank(message = "레시피명은 필수입니다")
        @Size(max = 100, message = "레시피명은 100자를 초과할 수 없습니다")
        private String recipeName;
    }
}

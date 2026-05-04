package com.mumuk.domain.recipe.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

public class RecipeBlogImageResponse {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageSearchResult {
        private String recipeName;
        private String imageUrl;
        private String searchSource; // "naver_image", "naver_blog", "fallback"
        private LocalDateTime searchedAt;
    }
}

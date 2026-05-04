package com.mumuk.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class UserRecipeResponse {

    @Getter
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserRecipeRes {
        private Long id;
        private String title;
        private String recipeImage;
        private String description;
        private Long cookingTime;
        private Long calories;
        private Long protein;
        private Long carbohydrate;
        private Long fat;
        private List<String> categories;
        private String ingredients;
        private String sourceUrl;
        private List<RecipeIngredientDTO> recipeIngredients;
        private List<String> inFridgeIngredients;
        private List<String> notInFridgeIngredients;
        private boolean viewed;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime viewedAt;
        private boolean liked;
    }

    @Getter
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecipeIngredientDTO {
        private String name;
        private boolean isInFridge;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeSummaryDTO {
        private Long recipeId;
        private String name;
        private String imageUrl;
        private boolean liked;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeSummaryDTOList {
        private List<RecipeSummaryDTO> recipeSummaries;
    }

    @Builder
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LikedRecipeListDTO {
        private Long userId;
        private List<UserRecipeResponse.RecipeSummaryDTO> likedRecipes;
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private int pageSize;
        private boolean hasNext;
    }
}

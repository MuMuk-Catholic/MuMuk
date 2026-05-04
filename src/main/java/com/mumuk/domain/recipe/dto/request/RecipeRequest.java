package com.mumuk.domain.recipe.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class RecipeRequest {

    @Getter
    @NoArgsConstructor
    public static class CreateReq {
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다")
        private String title;

        @Size(max = 150, message = "이미지 URL은 150자를 초과할 수 없습니다")
        private String recipeImage;

        @Size(max = 100, message = "설명은 100자를 초과할 수 없습니다")
        private String description;

        @Min(value = 1, message = "조리시간은 1분 이상이어야 합니다")
        private Long cookingTime;

        @Min(value = 0, message = "칼로리는 0 이상이어야 합니다")
        private Long calories;

        @Min(value = 0, message = "단백질은 0 이상이어야 합니다")
        private Long protein;

        @Min(value = 0, message = "탄수화물은 0 이상이어야 합니다")
        private Long carbohydrate;

        @Min(value = 0, message = "지방은 0 이상이어야 합니다")
        private Long fat;

        @Size(min = 1, message = "최소 1개 이상의 카테고리를 선택해야 합니다")
        private List<String> categories; // 복수 카테고리 지원

        @Size(max = 200, message = "재료는 200자를 초과할 수 없습니다")
        private String ingredients;

        public void setTitle(String title) { this.title = title; }
        public void setRecipeImage(String recipeImage) { this.recipeImage = recipeImage; }
        public void setDescription(String description) { this.description = description; }
        public void setCookingTime(Long cookingTime) { this.cookingTime = cookingTime; }
        public void setCalories(Long calories) { this.calories = calories; }
        public void setProtein(Long protein) { this.protein = protein; }
        public void setCarbohydrate(Long carbohydrate) { this.carbohydrate = carbohydrate; }
        public void setFat(Long fat) { this.fat = fat; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    }


}

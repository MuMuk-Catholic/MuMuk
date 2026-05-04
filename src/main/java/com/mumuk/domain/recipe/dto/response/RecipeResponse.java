package com.mumuk.domain.recipe.dto.response;

import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.converter.RecipeConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public class RecipeResponse {

    @Getter
    @AllArgsConstructor
    public static class DetailRes {
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

        /**
         * Recipe 엔티티로부터 DetailRes를 생성합니다.
         * @param recipe Recipe 엔티티
         * @return DetailRes 객체
         */
        public static DetailRes from(Recipe recipe) {
            return RecipeConverter.toDetailRes(recipe);
        }
    }



    @Getter
    @AllArgsConstructor
    public static class IngredientMatchingRes {
        private Long recipeId;
        private String recipeTitle;
        private List<String> match;
        private List<String> mismatch;
        private List<ReplaceableIngredient> replaceable;
    }

    @Getter
    @AllArgsConstructor
    public static class ReplaceableIngredient {
        private String recipeIngredient;
        private String userIngredient;
    }
}

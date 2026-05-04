package com.mumuk.domain.recipe.converter;

import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.recipe.entity.RecipeCategory;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;

import java.util.List;
import java.util.stream.Collectors;

public class RecipeConverter {

    // 등록용 DTO → 엔티티
    public static Recipe toRecipe(RecipeRequest.CreateReq req) {
        Recipe recipe = new Recipe();
        recipe.setTitle(req.getTitle());
        recipe.setRecipeImage(req.getRecipeImage());
        recipe.setDescription(req.getDescription());
        recipe.setCookingTime(req.getCookingTime());
        recipe.setCalories(req.getCalories());
        recipe.setProtein(req.getProtein());
        recipe.setCarbohydrate(req.getCarbohydrate());
        recipe.setFat(req.getFat());
        // 카테고리 문자열 리스트를 Enum 리스트로 변환
        if (req.getCategories() != null) {
            List<RecipeCategory> categoryEnums = req.getCategories().stream()
                    .map(categoryStr -> RecipeCategory.valueOf(categoryStr.toUpperCase()))
                    .collect(Collectors.toList());
            recipe.setCategories(categoryEnums);
        }
        recipe.setIngredients(req.getIngredients());
        return recipe;
    }

    // 엔티티 → 상세 응답 DTO
    public static RecipeResponse.DetailRes toDetailRes(Recipe recipe) {
        // 카테고리 리스트를 문자열 리스트로 변환
        List<String> categoryNames = recipe.getCategories().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return new RecipeResponse.DetailRes(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getRecipeImage(),
                recipe.getDescription(),
                recipe.getCookingTime(),
                recipe.getCalories(),
                recipe.getProtein(),
                recipe.getCarbohydrate(),
                recipe.getFat(),
                categoryNames,
                recipe.getIngredients()
        );
    }

    // 엔티티 → 간단한 응답 DTO
    public static UserRecipeResponse.RecipeSummaryDTO toRecipeSummaryDTO(Recipe recipe) {
        return UserRecipeResponse.RecipeSummaryDTO.builder()
                .recipeId(recipe.getId())
                .name(recipe.getTitle())
                .imageUrl(recipe.getRecipeImage())
                .liked(false) // 기본값은 false
                .build();
    }

    // 엔티티 + 찜 여부 → 간단한 응답 DTO
    public static UserRecipeResponse.RecipeSummaryDTO toRecipeSummaryDTO(Recipe recipe, Boolean isLiked) {
        return UserRecipeResponse.RecipeSummaryDTO.builder()
                .recipeId(recipe.getId())
                .name(recipe.getTitle())
                .imageUrl(recipe.getRecipeImage())
                .liked(isLiked != null ? isLiked : false)
                .build();
    }

}

package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;

import java.util.List;

public interface RecipeRecommendService {

    /**
     * 사용자의 재료를 기반으로 레시피를 추천합니다.
     */
    List<UserRecipeResponse.RecipeSummaryDTO> recommendRecipesByIngredient(Long userId);



    /**
     * 특정 카테고리들에 해당하는 레시피를 추천합니다.
     */
    List<UserRecipeResponse.RecipeSummaryDTO> recommendRecipesByCategories(Long userId, String categories);

    /**
     * 랜덤하게 레시피를 추천합니다.
     */
    List<UserRecipeResponse.RecipeSummaryDTO> recommendRandomRecipes(Long userId);

    /**
     * OCR 결과를 기반으로 레시피를 추천합니다.
     */
    List<UserRecipeResponse.RecipeSummaryDTO> recommendRecipesByOcr(Long userId);

    /**
     * 사용자의 건강 목표를 기반으로 레시피를 추천합니다.
     */
    List<UserRecipeResponse.RecipeSummaryDTO> recommendRecipesByHealthGoal(Long userId);

    /**
     * 여러 조건을 조합하여 레시피를 추천합니다.
     */
    List<UserRecipeResponse.RecipeSummaryDTO> recommendRecipesByCombined(Long userId);

    /**
     * AI를 사용하여 재료 기반 레시피를 생성하고 저장합니다.
     */
    List<RecipeResponse.DetailRes> createAndSaveRecipesByIngredient(Long userId);

    /**
     * AI를 사용하여 랜덤 레시피를 생성하고 저장합니다.
     * @param userId 사용자 ID
     * @param topic 선택적 주제 (null이면 완전 랜덤)
     */
    List<RecipeResponse.DetailRes> createAndSaveRandomRecipes(Long userId, String topic);

    /**
     * AI를 사용하여 랜덤 레시피를 생성하고 저장합니다. (기존 호환성 유지)
     * 주제 없이 완전 랜덤하게 레시피를 생성합니다.
     */
    List<RecipeResponse.DetailRes> createAndSaveRandomRecipes(Long userId);

    /**
     * AI를 사용하여 키워드 기반 랜덤 레시피를 생성하고 저장합니다.
     * @param userId 사용자 ID
     * @param keyword 키워드 (null이면 완전 랜덤)
     */
    List<RecipeResponse.DetailRes> createAndSaveRandomRecipesByKeyword(Long userId, String keyword);
}

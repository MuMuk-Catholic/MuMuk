package com.mumuk.domain.recipe.controller;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.service.RecipeRecommendService;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipe/recommend")
@Tag(name = "레시피 추천 관련", description = "AI 기반 레시피 추천 및 생성 API")
@RequiredArgsConstructor
public class RecipeRecommendController {

    private final RecipeRecommendService recommendService;

    @Operation(summary = "AI 추천 레시피 조회 (냉장고 재료 기반)", description = "사용자의 보유 재료와 알레르기 정보를 기반으로 AI가 추천하는 레시피를 조회합니다.")
    @GetMapping("/ingredient")
    public Response<List<UserRecipeResponse.RecipeSummaryDTO>> recommendRecipesByIngredient(@AuthUser Long userId) {
        List<UserRecipeResponse.RecipeSummaryDTO> result = recommendService.recommendRecipesByIngredient(userId);
        return Response.ok(result);
    }



    @Operation(summary = "카테고리 기반 레시피 조회", description = "특정 카테고리에 해당하는 레시피를 조회합니다.")
    @GetMapping("/categories/{categories}")
    public Response<List<UserRecipeResponse.RecipeSummaryDTO>> recommendRecipesByCategories(@AuthUser Long userId, @PathVariable String categories) {
        List<UserRecipeResponse.RecipeSummaryDTO> result = recommendService.recommendRecipesByCategories(userId, categories);
        return Response.ok(result);
    }

    @Operation(summary = "무작위 레시피 조회", description = "랜덤하게 선택된 레시피를 조회합니다.")
    @GetMapping("/random")
    public Response<List<UserRecipeResponse.RecipeSummaryDTO>> recommendRandomRecipes(@AuthUser Long userId) {
        List<UserRecipeResponse.RecipeSummaryDTO> result = recommendService.recommendRandomRecipes(userId);
        return Response.ok(result);
    }

    @Operation(summary = "AI 추천 레시피 조회 (OCR 기반)", description = "OCR로 추출된 건강 정보를 기반으로 AI가 추천하는 레시피를 조회합니다.")
    @GetMapping("/health-info")
    public Response<List<UserRecipeResponse.RecipeSummaryDTO>> recommendRecipesByOcr(@AuthUser Long userId) {
        List<UserRecipeResponse.RecipeSummaryDTO> result = recommendService.recommendRecipesByOcr(userId);
        return Response.ok(result);
    }

    @Operation(summary = "AI 추천 레시피 조회 (건강목표 기반)", description = "사용자의 건강 목표를 기반으로 AI가 추천하는 레시피를 조회합니다.")
    @GetMapping("/health-goal")
    public Response<List<UserRecipeResponse.RecipeSummaryDTO>> recommendRecipesByHealthGoal(@AuthUser Long userId) {
        List<UserRecipeResponse.RecipeSummaryDTO> result = recommendService.recommendRecipesByHealthGoal(userId);
        return Response.ok(result);
    }

    @Operation(summary = "AI 추천 레시피 조회 (사용자 맞춤)", description = "여러 조건을 조합하여 사용자 맞춤형 레시피를 추천합니다.")
    @GetMapping("/combined")
    public Response<List<UserRecipeResponse.RecipeSummaryDTO>> recommendRecipesByCombined(@AuthUser Long userId) {
        List<UserRecipeResponse.RecipeSummaryDTO> result = recommendService.recommendRecipesByCombined(userId);
        return Response.ok(result);
    }

    @Operation(summary = "AI 추천 레시피 등록 (냉장고 재료 기반)", description = "사용자의 보유 재료와 알레르기 정보를 기반으로 AI가 새로운 레시피를 생성하고 저장합니다.")
    @PostMapping("/ingredient")
    public Response<List<RecipeResponse.DetailRes>> createAndSaveRecipesByIngredient(@AuthUser Long userId) {
        List<RecipeResponse.DetailRes> result = recommendService.createAndSaveRecipesByIngredient(userId);
        return Response.ok(result);
    }

    @Operation(summary = "AI 추천 레시피 등록 (랜덤)", description = "AI가 랜덤하게 새로운 레시피를 생성하고 저장합니다.")
    @PostMapping("/random")
    public Response<List<RecipeResponse.DetailRes>> createAndSaveRandomRecipes(@AuthUser Long userId) {
        List<RecipeResponse.DetailRes> result = recommendService.createAndSaveRandomRecipes(userId);
        return Response.ok(result);
    }

    @Operation(summary = "AI 추천 레시피 등록 (키워드 기반)", description = "키워드를 기반으로 AI가 새로운 레시피를 생성하고 저장합니다. 키워드가 없으면 완전 랜덤하게 생성합니다.")
    @PostMapping("/random/{keyword}")
    public Response<List<RecipeResponse.DetailRes>> createAndSaveRandomRecipesByKeyword(@AuthUser Long userId, @PathVariable String keyword) {
        List<RecipeResponse.DetailRes> result = recommendService.createAndSaveRandomRecipesByKeyword(userId, keyword);
        return Response.ok(result);
    }
}
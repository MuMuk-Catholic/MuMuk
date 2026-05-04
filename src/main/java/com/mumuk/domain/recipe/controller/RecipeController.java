package com.mumuk.domain.recipe.controller;

import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.service.RecipeService;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;

@RestController
@Tag(name = "레시피 및 카테고리 관련")
@RequestMapping("/api/recipe")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Operation(summary = "레시피 등록")
    @PostMapping
    public Response<String> createRecipe(@Valid @RequestBody RecipeRequest.CreateReq request) {
        recipeService.createRecipe(request);
        return Response.ok(ResultCode.RECIPE_CREATE_OK, "레시피 등록 완료");
    }

    @Operation(summary = "레시피 삭제")
    @DeleteMapping("/{id}")
    public Response<String> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return Response.ok(ResultCode.RECIPE_DELETE_OK, "레시피 삭제 완료");
    }

    @Operation(summary = "레시피 상세 조회")
    @GetMapping("/{id}")
    public Response<RecipeResponse.DetailRes> getRecipe(@PathVariable Long id) {
        RecipeResponse.DetailRes response = recipeService.getRecipeDetail(id);
        return Response.ok(ResultCode.RECIPE_FETCH_OK, response);
    }

    @Operation(summary = "레시피 부분 수정")
    @PatchMapping("/{id}")
    public Response<String> updateRecipe(@PathVariable Long id, @Valid @RequestBody RecipeRequest.CreateReq request) {
        recipeService.updateRecipe(id, request);
        return Response.ok(ResultCode.RECIPE_UPDATE_OK, "레시피 수정 완료");
    }

    @Operation(summary = "카테고리별 레시피 이름 조회 (단일 카테고리)")
    @GetMapping("/category/{category}/names")
    public Response<List<String>> getRecipeNamesByCategory(@PathVariable String category) {
        List<String> names = recipeService.findNamesByCategory(category);
        return Response.ok(ResultCode.RECIPE_FETCH_OK, names);
    }

    @Operation(summary = "카테고리별 레시피 이름 조회 (콤마 구분)")
    @GetMapping("/categories/{categories}/names")
    public Response<List<String>> getRecipeNamesByCategories(@PathVariable String categories) {
        List<String> names = recipeService.findNamesByCategories(categories);
        return Response.ok(ResultCode.RECIPE_FETCH_OK, names);
    }

    @Operation(summary = "레시피 전체 목록 조회")
    @GetMapping
    public Response<List<RecipeResponse.DetailRes>> getAllRecipes() {
        List<RecipeResponse.DetailRes> recipes = recipeService.getAllRecipes();
        return Response.ok(ResultCode.RECIPE_FETCH_OK, recipes);
    }

    @Operation(summary = "레시피 요약 목록 조회")
    @GetMapping("/summaries")
    public Response<List<UserRecipeResponse.RecipeSummaryDTO>> getRecipeSummaries(@AuthUser Long userId) {
        List<UserRecipeResponse.RecipeSummaryDTO> recipes = recipeService.getRecipeSummaries(userId);
        return Response.ok(ResultCode.RECIPE_FETCH_OK, recipes);
    }

    @Operation(summary = "AI 기반 재료 매칭")
    @GetMapping("/{recipeId}/ingredients/match/ai")
    public Response<RecipeResponse.IngredientMatchingRes> matchIngredientsByAI(@AuthUser Long userId, @PathVariable Long recipeId) {
        RecipeResponse.IngredientMatchingRes result = recipeService.matchIngredientsByAI(userId, recipeId);
        return Response.ok(ResultCode.RECIPE_FETCH_OK, result);
    }

    @Operation(summary = "단순 재료 매칭")
    @GetMapping("/{recipeId}/ingredients/match/simple")
    public Response<RecipeResponse.IngredientMatchingRes> matchIngredientsSimple(@AuthUser Long userId, @PathVariable Long recipeId) {
        RecipeResponse.IngredientMatchingRes result = recipeService.matchIngredientsSimple(userId, recipeId);
        return Response.ok(ResultCode.RECIPE_FETCH_OK, result);
    }
}

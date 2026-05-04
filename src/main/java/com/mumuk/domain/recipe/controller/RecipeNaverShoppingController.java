package com.mumuk.domain.recipe.controller;

import com.mumuk.domain.recipe.dto.response.RecipeNaverShoppingResponse;
import com.mumuk.domain.recipe.service.RecipeNaverShoppingService;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/recipe")
public class RecipeNaverShoppingController {

    private final RecipeNaverShoppingService recipeNaverShoppingService;

    public RecipeNaverShoppingController(RecipeNaverShoppingService recipeNaverShoppingService) {
        this.recipeNaverShoppingService = recipeNaverShoppingService;
    }

    @Operation(summary = "네이버 쇼핑에서 재료 검색", description = "레시피에 필요한 재료중 사용자에게 없는 재료를 네이버 쇼핑에서 검색합니다(검색어 1개당 3개의 상품 조회).")
    @GetMapping("/search-naver-shopping/{recipeId}")
    public RecipeNaverShoppingResponse searchNaverShopping(@AuthUser Long userId, @PathVariable Long recipeId) {
        return recipeNaverShoppingService.searchNaverShopping(userId, recipeId);
    }
}

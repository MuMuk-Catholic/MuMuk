package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.dto.response.RecipeNaverShoppingResponse;
public interface RecipeNaverShoppingService {
    RecipeNaverShoppingResponse searchNaverShopping(Long userId, Long recipeId);
}

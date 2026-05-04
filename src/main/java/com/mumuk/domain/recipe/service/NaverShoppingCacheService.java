package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.dto.response.RecipeNaverShoppingResponse;

import java.util.List;

public interface NaverShoppingCacheService {
    List<RecipeNaverShoppingResponse.NaverShopping> fetchAndCacheProduct(String url);
    List<RecipeNaverShoppingResponse.NaverShopping> getCachedProduct(String url);
}

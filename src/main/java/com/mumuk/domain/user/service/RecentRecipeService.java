package com.mumuk.domain.user.service;

import com.mumuk.domain.recipe.entity.Recipe;

import java.util.List;

public interface RecentRecipeService {
    void addRecentRecipe(Long userId, Long recipeId);
}

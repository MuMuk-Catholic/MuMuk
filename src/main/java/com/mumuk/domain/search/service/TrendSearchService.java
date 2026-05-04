package com.mumuk.domain.search.service;

import com.mumuk.domain.search.dto.response.SearchResponse;

import java.util.List;

public interface TrendSearchService {
    public void increaseKeywordCount(Long recipeId);

    public void cacheTrendRecipe();

    public  List<Long> getCachedTrendRecipe();

    public SearchResponse.TrendRecipeTitleRes getTrendRecipeTitleList();

    public List<SearchResponse.TrendRecipeDetailRes> getTrendRecipeDetailList(Long userId);

}

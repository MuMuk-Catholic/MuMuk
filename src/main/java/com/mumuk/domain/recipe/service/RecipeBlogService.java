package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.dto.response.RecipeBlogResponse;

public interface RecipeBlogService {
    RecipeBlogResponse searchBlogByKeyword(String keyword);
}

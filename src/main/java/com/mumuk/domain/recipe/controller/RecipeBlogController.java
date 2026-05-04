package com.mumuk.domain.recipe.controller;


import com.mumuk.domain.recipe.dto.response.RecipeBlogResponse;
import com.mumuk.domain.recipe.service.RecipeBlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "블로그 레시피 관련")
@RequestMapping("/api/recipe")
public class RecipeBlogController {

    private final RecipeBlogService recipeBlogService;

    public RecipeBlogController(RecipeBlogService recipeBlogService) {
        this.recipeBlogService = recipeBlogService;
    }

    @Operation(summary = "네이버 블로그 레시피", description = "키워드를 파라미터로 받아, 관련된 블로그 데이터 리스트를 반환합니다.")
    @GetMapping("/search-blog")
    public RecipeBlogResponse searchBlog(@RequestParam @NotBlank(message = "검색 키워드는 필수입니다") String keyword) {
        return recipeBlogService.searchBlogByKeyword(keyword);
    }
}

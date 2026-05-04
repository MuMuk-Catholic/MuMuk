package com.mumuk.domain.search.controller;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.search.dto.request.SearchRequest;
import com.mumuk.domain.search.dto.response.SearchResponse;
import com.mumuk.domain.search.service.*;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@Tag(name = "레시피 검색 및 추천/최근/인기 검색어 관련")
public class SearchController {


    private final TrendSearchService trendSearchService;



    public SearchController( TrendSearchService trendSearchService) {
        this.trendSearchService = trendSearchService;

    }


    @Operation(summary = "인기 레시피 검색어 조회",
            description = "검색 화면에서 사용. user-recipe-controller에서 레시피 상세 조회시, 해당 레시피의 id가 현 시간대의 redis에 저장됨." +
                    "레시피 검색어 조회시 !이전 시간대!의 인기 레시피가 조회됨  ")
    @GetMapping("/trends/recipe-title")
    public Response<SearchResponse.TrendRecipeTitleRes> getTrendRecipeTitle(){
        SearchResponse.TrendRecipeTitleRes trendRecipeTitle=trendSearchService.getTrendRecipeTitleList();
        return Response.ok(ResultCode.SEARCH_GET_TRENDRECIPETITLE_OK ,trendRecipeTitle);
    }

    @Operation(summary = "인기 레시피 세부 목록 조회",
            description = "홈 화면에서 사용. user-recipe-controller에서 레시피 상세 조회시, 해당 레시피의 id가 현 시간대의 redis에 저장됨." +
                    "레시피 검색어 조회시 !이전 시간대!의 인기 레시피가 조회됨")
    @GetMapping("/trends/recipe-detail")
    public Response<List<SearchResponse.TrendRecipeDetailRes>> getTrendRecipeDetail(@AuthUser Long userId){
        List <SearchResponse.TrendRecipeDetailRes> trendRecipeDetail=trendSearchService.getTrendRecipeDetailList(userId);
        return Response.ok(ResultCode.SEARCH_GET_TRENDRECIPEDETAIL_OK ,trendRecipeDetail);
    }
}

package com.mumuk.domain.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class SearchResponse {

    @Getter
    public static class TrendRecipeTitleRes {
        private final List<String> trendRecipeTitleList;
        private final LocalDateTime localDateTime;

        public TrendRecipeTitleRes(List<String> trendRecipeTitleList) {
            this.trendRecipeTitleList = trendRecipeTitleList;
            this.localDateTime = LocalDateTime.now();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class TrendRecipeDetailRes {
        private Long recipeId;
        private String title;
        private String imageUrl;
        private Long calories;
        private boolean isLiked;
    }


}

package com.mumuk.domain.recipe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.util.List;
@Getter
@AllArgsConstructor
public class RecipeNaverShoppingResponse {

    private List<NaverShopping> naverShoppings;

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NaverShopping {
        private String title;
        private Integer price;
        private String link;
        private String imageUrl;
    }
}

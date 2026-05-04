package com.mumuk.domain.recipe.service;
import com.mumuk.domain.recipe.dto.response.RecipeNaverShoppingResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.security.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecipeNaverShoppingServiceImpl implements RecipeNaverShoppingService {


    private final NaverShoppingCacheService naverShoppingCacheService;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;


    public RecipeNaverShoppingServiceImpl(NaverShoppingCacheService naverShoppingCacheService, UserRepository userRepository, RecipeRepository recipeRepository) {

        this.naverShoppingCacheService = naverShoppingCacheService;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;

    }

    @Override
    public RecipeNaverShoppingResponse searchNaverShopping(Long userId, Long recipeId) {


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));

        //레시피 재료 리스트: 레시피 재료 문자열 파싱
        String recipeIngredientsString = recipe.getIngredients();
        if (recipeIngredientsString == null || recipeIngredientsString.trim().isEmpty()) {
            recipeIngredientsString = "";
        }
        List<String> recipeIngredients = Arrays.stream(recipeIngredientsString.split(","))
                .map(String::trim)
                .filter(ingredient -> !ingredient.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // 사용자가 보유한 재료 리스트
        List<String> inUserIngredients = Optional.ofNullable(user.getIngredients())
                .orElse(Collections.emptyList()).stream()
                .map(ingredient -> {
                    String name = ingredient.getName();
                    return name;
                })
                .collect(Collectors.toList());

        //냉장고에 없는 재료 리스트
        List<String> notInFridgeIngredients = new ArrayList<>();

        for (String ingredient : recipeIngredients) {
            //{재료 이름, 냉장고에 있는지 여부}를 리스트에 저장
            boolean isInFridge = inUserIngredients.contains(ingredient);
            if (!isInFridge) { //레시피 재료가 사용자가 보유한 재료에 없으면 없는 재료 리스트에 추가
                notInFridgeIngredients.add(ingredient);
            }
        }

        try {

            List<RecipeNaverShoppingResponse.NaverShopping> allNaverShoppingProducts = notInFridgeIngredients.stream()
                    .flatMap(ingredient -> {

                        String encodedString = URLEncoder.encode(ingredient, StandardCharsets.UTF_8);
                        String url = "https://openapi.naver.com/v1/search/shop?query=" +encodedString;
                        // 캐시된 상품 조회
                        List<RecipeNaverShoppingResponse.NaverShopping> ingredientProducts = naverShoppingCacheService.getCachedProduct(url);

                        // 없으면 api 호출하고 레디스에 저장
                        if (ingredientProducts == null || ingredientProducts.isEmpty()) {
                            ingredientProducts = naverShoppingCacheService.fetchAndCacheProduct(url);

                        }
                        return ingredientProducts.stream();
                    })
                    .collect(Collectors.toList());

            return new RecipeNaverShoppingResponse(allNaverShoppingProducts);

        } catch (Exception e) {
            log.info("error",e);
            throw new BusinessException(ErrorCode.NAVER_SHOPPING_API_ERROR);
        }
    }

}

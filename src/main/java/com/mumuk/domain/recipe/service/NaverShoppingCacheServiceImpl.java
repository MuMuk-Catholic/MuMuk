package com.mumuk.domain.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.domain.recipe.dto.response.RecipeNaverShoppingResponse;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.client.NaverShoppingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class NaverShoppingCacheServiceImpl implements NaverShoppingCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration PRODUCT_CACHE_TTL = Duration.ofDays(3);// 3일 동안 캐시
    private final NaverShoppingClient naverShoppingClient;
    private final ObjectMapper objectMapper;

    public NaverShoppingCacheServiceImpl(RedisTemplate<String, Object> redisTemplate, NaverShoppingClient naverShoppingClient, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.naverShoppingClient = naverShoppingClient;
        this.objectMapper = objectMapper;

    }
    @Override
    public List<RecipeNaverShoppingResponse.NaverShopping> fetchAndCacheProduct(String url) {

        List<RecipeNaverShoppingResponse.NaverShopping> naverShoppingProducts = new ArrayList<>();

        try {
            String json = naverShoppingClient.searchShopping(url);
            JsonNode rootNode = objectMapper.readTree(json);
            JsonNode itemsNode = rootNode.path("items");

            int count = Math.min(itemsNode.size(), 3);
            for (int i = 0; i < count; i++) {
                JsonNode itemNode = itemsNode.get(i);
                // 상품명 추출
                String titleWithTags = itemNode.path("title").asText();
                // 상품명의 <b> 태그와 </b> 태그를 모두 빈 문자열로 대체
                String title = titleWithTags.replaceAll("<b>", "").replaceAll("</b>", "");
                //가격, 링크, 이미지를 추출
                String link = itemNode.path("link").asText();
                Integer productPrice = itemNode.path("lprice").asInt();
                String imgUrl = itemNode.path("image").asText();

                naverShoppingProducts.add(RecipeNaverShoppingResponse.NaverShopping.builder()
                        .title(title)
                        .link(link)
                        .price(productPrice)
                        .imageUrl(imgUrl)
                        .build());
            }
            //레디스에 저장
            if (!naverShoppingProducts.isEmpty()) {
                String key = toRedisKey(url);
                redisTemplate.opsForValue().set(key, naverShoppingProducts, PRODUCT_CACHE_TTL);
            }

            return naverShoppingProducts;

        } catch (Exception e) {
            log.error("쇼핑 검색 실패", e);
            throw new BusinessException(ErrorCode.NAVER_SHOPPING_API_ERROR);
        }

    }

    @Override
    public List<RecipeNaverShoppingResponse.NaverShopping> getCachedProduct(String url) {
        String key = toRedisKey(url);
        List<RecipeNaverShoppingResponse.NaverShopping> value = (List<RecipeNaverShoppingResponse.NaverShopping>)redisTemplate.opsForValue().get(key);
        return value != null ? value : null;
    }

    public static String toRedisKey(String url) {
        return "naverShopping:" + DigestUtils.md5DigestAsHex(url.getBytes(StandardCharsets.UTF_8));
    }

}

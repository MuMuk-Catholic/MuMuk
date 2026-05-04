package com.mumuk.domain.search.service;

import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.search.dto.response.SearchResponse;
import com.mumuk.domain.user.entity.UserRecipe;
import com.mumuk.domain.user.repository.UserRecipeRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class TrendSearchServiceImpl implements TrendSearchService {
    private static final String CACHEKEY = "trend_recipe";
    private final String KEY_PREFIX= "trend_keywords";
    private final RedisTemplate<String, String> redisTemplate;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private final RecipeRepository recipeRepository;
    private final UserRecipeRepository userRecipeRepository;


    public TrendSearchServiceImpl(RedisTemplate<String, String> redisTemplate, RecipeRepository recipeRepository, UserRecipeRepository userRecipeRepository) {
        this.redisTemplate = redisTemplate;
        this.recipeRepository = recipeRepository;
        this.userRecipeRepository = userRecipeRepository;
    }

    @Override
    // 검색시 카운트 증가
    public void increaseKeywordCount(Long recipeId) {

        // ex) 2025080723 -> 2025년 8월 7일 23시~ 24시 사이에 검색된 레시피를 집계하기 위한 key 생성
        String KEY=KEY_PREFIX+ LocalDateTime.now().format(formatter);

        redisTemplate.opsForZSet().incrementScore(KEY,String.valueOf(recipeId),1);

        redisTemplate.expire(KEY, 2, TimeUnit.HOURS);
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    // 1시간에 한 번씩 zset에 저장된 검색어 순위들을 불러옴
    // 스케쥴링을 통해 return한 값들은 별다른 처리가 없다면 버려지기 떄문에, 순위측정을 따로 분리
    public void cacheTrendRecipe() {
        // 현재 시간이 23시일 때, 22시~23시 사이예 집계된 레피시를 보여주기 위한 KEY를 생성
        String KEY=KEY_PREFIX+ LocalDateTime.now().minusHours(1).format(formatter);
        Set<String> trendRecipeIdSet = redisTemplate.opsForZSet().reverseRange(KEY,0,9);

        if (trendRecipeIdSet != null && !trendRecipeIdSet.isEmpty()) {
            redisTemplate.delete(CACHEKEY);
            redisTemplate.opsForList().rightPushAll(CACHEKEY,new ArrayList<>(trendRecipeIdSet));
        }
    }

    @Override
    public List<Long> getCachedTrendRecipe() {
        List<String> recipeIdList= redisTemplate.opsForList().range(CACHEKEY,0,9);

        if (recipeIdList == null){
            return Collections.emptyList();
        }

        return recipeIdList.stream().map(Long::valueOf).toList();
    }

    @Override
    @Transactional(readOnly = true)
    // 레시피 이름만 반환
    public SearchResponse.TrendRecipeTitleRes getTrendRecipeTitleList() {

        // 캐시된 리스트를 불러옴
        List<Long> recipeIdList = getCachedTrendRecipe();

        Map<Long, Recipe> recipeMap= recipeRepository.findAllById(recipeIdList).stream()
                .collect(Collectors.toMap(Recipe::getId, Function.identity()));

        // redis에는 있으나 DB에는 없는 경우, null이 발생하므로 npe 발생 상황 제거
        List<String> trendRecipeTitleList=recipeIdList.stream()
                .map(id -> {
                    Recipe recipe = recipeMap.get(id);
                    return recipe != null ? recipe.getTitle() : null;
                })
                .filter(Objects::nonNull)
                .toList();

        return new SearchResponse.TrendRecipeTitleRes(trendRecipeTitleList);
    }

    @Override
    @Transactional(readOnly = true)
    // 레시피 정보까지 반환
    public List<SearchResponse.TrendRecipeDetailRes> getTrendRecipeDetailList(Long userId) {

        List<Long> recipeIdList = getCachedTrendRecipe();

        Map<Long,Recipe> recipeMap = recipeRepository.findAllById(recipeIdList).stream()
                .collect(Collectors.toMap(Recipe::getId, Function.identity()));

        Map<Long, UserRecipe> userRecipeMap = userRecipeRepository.findByUserIdAndRecipeIdIn(userId,recipeIdList)
                .stream()
                .collect(Collectors.toMap(userRecipe -> userRecipe.getRecipe().getId(), Function.identity()));

        List<SearchResponse.TrendRecipeDetailRes> result= recipeIdList.stream()
                .filter(recipeMap::containsKey)
                .map(id->{
                    Recipe recipe = recipeMap.get(id);
                    boolean isLiked=userRecipeMap.containsKey(id)&&userRecipeMap.get(id).getLiked();

                    return new SearchResponse.TrendRecipeDetailRes(
                            recipe.getId(),
                            recipe.getTitle(),
                            recipe.getRecipeImage(),
                            recipe.getCalories(),
                            isLiked
                    );
                }).toList();

        return result;

    }
}
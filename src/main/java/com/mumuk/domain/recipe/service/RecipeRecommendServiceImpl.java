package com.mumuk.domain.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.domain.healthManagement.dto.response.AllergyResponse;
import com.mumuk.domain.healthManagement.service.AllergyService;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;
import com.mumuk.domain.ingredient.service.IngredientService;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.entity.RecipeCategory;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.recipe.converter.RecipeConverter;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.domain.user.repository.UserRecipeRepository;
import com.mumuk.domain.user.entity.UserRecipe;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.client.GeminiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import org.springframework.web.reactive.function.client.WebClient;

import com.mumuk.domain.ocr.entity.UserHealthData;
import com.mumuk.domain.ocr.repository.UserHealthDataRepository;
import com.mumuk.domain.healthManagement.service.HealthGoalService;
import com.mumuk.domain.recipe.service.RecipeBlogImageService;



@Slf4j
@Service
public class RecipeRecommendServiceImpl implements RecipeRecommendService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final IngredientService ingredientService;
    private final AllergyService allergyService;
    private final RecipeRepository recipeRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserHealthDataRepository userHealthDataRepository;
    private final HealthGoalService healthGoalService;
    private final RecipeBlogImageService recipeBlogImageService;

    // ===== 상수 정의 =====
    // 설정 변경 시 이 부분만 수정하면 됩니다

    /** 레시피 캐시 유효기간 (POST API용) */
    private static final Duration RECIPE_CACHE_TTL = Duration.ofDays(30);

    /** 레시피 제목 저장용 ZSet 키 (search domain과 동일) */
    private static final String RECIPE_TITLES_KEY = "recipetitles";

    /** 최대 추천 개수 (모든 추천 API에서 사용) */
    private static final int MAX_RECOMMENDATIONS = 4;

    /** 무작위 샘플 크기 (GET API용) */
    private static final int RANDOM_SAMPLE_SIZE = 12;

    /** POST API로 생성할 레시피 개수 */
    private static final int POST_RECIPE_COUNT = 5;

    /** 이미지 URL 최대 길이 (엔티티 컬럼 길이와 일치) */
    private static final int MAX_IMAGE_URL_LENGTH = 500;
    /** 성능 최적화를 위한 상세 로그 출력 여부 (운영에서는 false 권장) */
    private static final boolean ENABLE_VERBOSE_LOG = false;

    public RecipeRecommendServiceImpl(GeminiClient geminiClient, ObjectMapper objectMapper,
                                      UserRepository userRepository, UserRecipeRepository userRecipeRepository,
                                      IngredientService ingredientService, AllergyService allergyService,
                                      RecipeRepository recipeRepository, RedisTemplate<String, Object> redisTemplate,
                                      UserHealthDataRepository userHealthDataRepository, HealthGoalService healthGoalService,
                                      RecipeBlogImageService recipeBlogImageService) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.userRecipeRepository = userRecipeRepository;
        this.ingredientService = ingredientService;
        this.allergyService = allergyService;
        this.recipeRepository = recipeRepository;
        this.redisTemplate = redisTemplate;
        this.userHealthDataRepository = userHealthDataRepository;
        this.healthGoalService = healthGoalService;
        this.recipeBlogImageService = recipeBlogImageService;
    }



    @Override
    public List<UserRecipeResponse.RecipeSummaryDTO> recommendRecipesByIngredient(Long userId) {
        // 사용자 존재 검증 (값은 사용하지 않음)
        getUser(userId);

        // 사용자 보유 재료 및 알레르기 정보 조회
        List<String> availableIngredients = getUserIngredients(userId);
        List<String> allergyTypes = getUserAllergies(userId);

        // 레시피 적합도 평가 (무작위 샘플에서 상위 MAX_RECOMMENDATIONS개 선택)
        List<RecipeWithScore> recipesWithScores = evaluateRecipeSuitabilityByIngredient(
                getRandomRecipesForEvaluation(RANDOM_SAMPLE_SIZE), availableIngredients, allergyTypes);

        // 점수 내림차순 정렬 후 상위 MAX_RECOMMENDATIONS개 선택
        recipesWithScores.sort((a, b) -> Double.compare(b.score, a.score));
        List<RecipeWithScore> topRecipes = recipesWithScores.stream()
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());

        // 찜 여부 조회
        List<Long> recipeIds = topRecipes.stream()
                .map(recipeWithScore -> recipeWithScore.recipe.getId())
                .collect(Collectors.toList());
        Map<Long, Boolean> likedMap = getUserRecipeLikedMap(userId, recipeIds);

        // RecipeSummaryDTO로 변환하여 반환
        return topRecipes.stream()
                .map(rws -> RecipeConverter.toRecipeSummaryDTO(rws.recipe, likedMap.get(rws.recipe.getId())))
                .collect(Collectors.toList());
    }



    @Override
    public List<UserRecipeResponse.RecipeSummaryDTO> recommendRecipesByCategories(Long userId, String categories) {
        // 카테고리 기반 무작위 추천: 사용자 재료/알레르기 미사용. 사용자 존재만 검증.
        getUser(userId);

        // 카테고리별 레시피 조회
        List<Recipe> recipes = getRecipesByCategories(categories);

        if (recipes.isEmpty()) {
            return new ArrayList<>();
        }

        // 상위 MAX_RECOMMENDATIONS개 레시피 선택
        List<Recipe> topRecipes = recipes.stream()
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());

        // 찜 여부 조회
        List<Long> recipeIds = topRecipes.stream()
                .map(Recipe::getId)
                .collect(Collectors.toList());
        Map<Long, Boolean> likedMap = getUserRecipeLikedMap(userId, recipeIds);

        // RecipeSummaryDTO로 변환하여 반환
        return topRecipes.stream()
                .map(recipe -> UserRecipeResponse.RecipeSummaryDTO.builder()
                        .recipeId(recipe.getId())
                        .name(recipe.getTitle())
                        .imageUrl(recipe.getRecipeImage())
                        .liked(likedMap.get(recipe.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRecipeResponse.RecipeSummaryDTO> recommendRandomRecipes(Long userId) {
        // 무작위 추천: 사용자 재료/알레르기 미사용. 사용자 존재만 검증.
        getUser(userId);

        // 랜덤 레시피 조회 (무작위 샘플에서 상위 MAX_RECOMMENDATIONS개 선택)
        List<Recipe> recipes = getRandomRecipesForEvaluation(RANDOM_SAMPLE_SIZE);

        if (recipes.isEmpty()) {
            return new ArrayList<>();
        }

        // 상위 MAX_RECOMMENDATIONS개 레시피 선택
        List<Recipe> topRecipes = recipes.stream()
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());

        // 찜 여부 조회
        List<Long> recipeIds = topRecipes.stream()
                .map(Recipe::getId)
                .collect(Collectors.toList());
        Map<Long, Boolean> likedMap = getUserRecipeLikedMap(userId, recipeIds);

        // RecipeSummaryDTO로 변환하여 반환
        return topRecipes.stream()
                .map(recipe -> UserRecipeResponse.RecipeSummaryDTO.builder()
                        .recipeId(recipe.getId())
                        .name(recipe.getTitle())
                        .imageUrl(recipe.getRecipeImage())
                        .liked(likedMap.get(recipe.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * OCR 기반 레시피 추천
     */
    @Override
    public List<UserRecipeResponse.RecipeSummaryDTO> recommendRecipesByOcr(Long userId) {
        if (ENABLE_VERBOSE_LOG) log.info("OCR 기반 레시피 추천 시작 - userId: {}", userId);

        // 사용자 정보 조회 (사용자 존재 검증)
        getUser(userId);
        // 사용자 알레르기 정보만 조회 (재료 정보는 불필요)
        List<String> allergyTypes = getUserAllergies(userId);

        // OCR 건강 데이터 조회
        Map<String, String> ocrHealthData = getLatestOcrHealthData(userId);

        if (ocrHealthData == null || ocrHealthData.isEmpty()) {
            log.warn("사용자의 OCR 건강 데이터 없음. 기본 재료 기반 추천으로 대체");
            return recommendRecipesByIngredient(userId);
        }

        // OCR 데이터를 기반으로 건강 정보 생성
        String healthInfo = buildOcrHealthInfo(ocrHealthData);

        // DB 레벨에서 랜덤 샘플링으로 48개 조회
        List<Recipe> sampledRecipes = getRandomRecipesForEvaluation(RANDOM_SAMPLE_SIZE);

        if (sampledRecipes.isEmpty()) {
            log.warn("DB에 레시피가 없습니다.");
            return new ArrayList<>();
        }

        if (ENABLE_VERBOSE_LOG) log.info("랜덤 선택된 레시피 수: {}", sampledRecipes.size());

        // AI가 각 레시피의 적합도를 평가 (랜덤 선택된 레시피 평가)
        List<RecipeWithScore> recipesWithScores = evaluateRecipeSuitabilityByHealth(
                sampledRecipes, new ArrayList<>(), allergyTypes, healthInfo);

        // 적합도 점수로 내림차순 정렬 (높은 점수가 위로)
        recipesWithScores.sort((a, b) -> Double.compare(b.score, a.score));

        // RecipeSummaryDTO로 변환하여 반환 (상위 MAX_RECOMMENDATIONS개만)
        List<RecipeWithScore> topRecipes = recipesWithScores.stream()
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());

        List<Long> recipeIds = topRecipes.stream()
                .map(recipeWithScore -> recipeWithScore.recipe.getId())
                .collect(Collectors.toList());
        Map<Long, Boolean> likedMap = getUserRecipeLikedMap(userId, recipeIds);

        if (ENABLE_VERBOSE_LOG) log.info("OCR 기반 레시피 추천 완료 - 추천된 레시피 수: {}", topRecipes.size());
        return topRecipes.stream()
                .map(rws -> RecipeConverter.toRecipeSummaryDTO(rws.recipe, likedMap.get(rws.recipe.getId())))
                .collect(Collectors.toList());
    }

    /**
     * HealthGoal 기반 레시피 추천
     */
    @Override
    public List<UserRecipeResponse.RecipeSummaryDTO> recommendRecipesByHealthGoal(Long userId) {
        if (ENABLE_VERBOSE_LOG) log.info("HealthGoal 기반 레시피 추천 시작 - userId: {}", userId);

        // 사용자 정보 조회 (사용자 존재 검증)
        getUser(userId);
        // 사용자 알레르기 정보만 조회 (재료 정보는 불필요)
        List<String> allergyTypes = getUserAllergies(userId);

        // HealthGoal 정보 조회
        List<String> healthGoals = getUserHealthGoals(userId);

        if (healthGoals == null || healthGoals.isEmpty()) {
            log.warn("사용자의 HealthGoal이 설정되지 않음. 기본 재료 기반 추천으로 대체");
            return recommendRecipesByIngredient(userId);
        }

        // DB 레벨에서 랜덤 샘플링으로 RANDOM_SAMPLE_SIZE개 조회
        List<Recipe> sampledRecipes = getRandomRecipesForEvaluation(RANDOM_SAMPLE_SIZE);

        if (sampledRecipes.isEmpty()) {
            log.warn("DB에 레시피가 없습니다.");
            return new ArrayList<>();
        }

        if (ENABLE_VERBOSE_LOG) log.info("랜덤 선택된 레시피 수: {}", sampledRecipes.size());

        // AI가 각 레시피의 적합도를 평가 (랜덤 선택된 레시피 평가)
        List<RecipeWithScore> scoredRecipes = evaluateRecipeSuitabilityByHealthGoal(
                sampledRecipes, new ArrayList<>(), allergyTypes, healthGoals);

        // 적합도 점수로 내림차순 정렬 (높은 점수가 위로)
        scoredRecipes.sort((a, b) -> Double.compare(b.score, a.score));

        // RecipeSummaryDTO로 변환하여 반환 (상위 MAX_RECOMMENDATIONS개만)
        List<RecipeWithScore> topRecipes = scoredRecipes.stream()
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());

        List<Long> recipeIds = topRecipes.stream()
                .map(recipeWithScore -> recipeWithScore.recipe.getId())
                .collect(Collectors.toList());
        Map<Long, Boolean> likedMap = getUserRecipeLikedMap(userId, recipeIds);

        if (ENABLE_VERBOSE_LOG) log.info("HealthGoal 기반 레시피 추천 완료 - 추천된 레시피 수: {}", topRecipes.size());
        return topRecipes.stream()
                .map(rws -> RecipeConverter.toRecipeSummaryDTO(rws.recipe, likedMap.get(rws.recipe.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 재료 + OCR + HealthGoal 통합 레시피 추천
     */
    @Override
    public List<UserRecipeResponse.RecipeSummaryDTO> recommendRecipesByCombined(Long userId) {
        if (ENABLE_VERBOSE_LOG) log.info("통합 레시피 추천 시작 - userId: {}", userId);

        // 사용자 정보 조회 (사용자 존재 검증)
        getUser(userId);
        // 사용자 보유 재료 및 알레르기 정보 조회
        List<String> availableIngredients = getUserIngredients(userId);
        List<String> allergyTypes = getUserAllergies(userId);

        // OCR 건강 데이터 조회
        Map<String, String> ocrHealthData = getLatestOcrHealthData(userId);

        // HealthGoal 정보 조회
        List<String> healthGoals = getUserHealthGoals(userId);

        // DB 레벨에서 랜덤 샘플링으로 RANDOM_SAMPLE_SIZE개 조회
        List<Recipe> sampledRecipes = getRandomRecipesForEvaluation(RANDOM_SAMPLE_SIZE);

        if (sampledRecipes.isEmpty()) {
            log.warn("DB에 레시피가 없습니다.");
            return new ArrayList<>();
        }

        if (ENABLE_VERBOSE_LOG) log.info("랜덤 선택된 레시피 수: {}", sampledRecipes.size());

        // AI가 각 레시피의 적합도를 평가 (랜덤 선택된 레시피 평가)
        List<RecipeWithScore> scoredRecipes = evaluateRecipeSuitabilityByCombined(
                sampledRecipes, availableIngredients, allergyTypes, ocrHealthData, healthGoals);

        // 적합도 점수로 내림차순 정렬 (높은 점수가 위로)
        scoredRecipes.sort((a, b) -> Double.compare(b.score, a.score));

        // RecipeSummaryDTO로 변환하여 반환 (상위 MAX_RECOMMENDATIONS개만)
        List<RecipeWithScore> topRecipes = scoredRecipes.stream()
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());

        List<Long> recipeIds = topRecipes.stream()
                .map(recipeWithScore -> recipeWithScore.recipe.getId())
                .collect(Collectors.toList());
        Map<Long, Boolean> likedMap = getUserRecipeLikedMap(userId, recipeIds);

        if (ENABLE_VERBOSE_LOG) log.info("통합 레시피 추천 완료 - 추천된 레시피 수: {}", topRecipes.size());
        return topRecipes.stream()
                .map(rws -> RecipeConverter.toRecipeSummaryDTO(rws.recipe, likedMap.get(rws.recipe.getId())))
                .collect(Collectors.toList());
    }

    // 레시피와 점수를 함께 저장하는 내부 클래스
    private static class RecipeWithScore {
        Recipe recipe;
        double score;

        RecipeWithScore(Recipe recipe, double score) {
            this.recipe = recipe;
            this.score = score;
        }
    }

    /**
     * 재료 기반 레시피 적합도 평가 (배치 처리)
     */
    private List<RecipeWithScore> evaluateRecipeSuitabilityByIngredient(List<Recipe> recipes,
                                                                        List<String> availableIngredients,
                                                                        List<String> allergyTypes) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        // 중복 제거만 수행
        List<String> uniqueIngredients = new ArrayList<>(new LinkedHashSet<>(availableIngredients));

        if (ENABLE_VERBOSE_LOG) {
            log.info("=== 적합도 평가 시작 ===");
            log.info("사용자 보유 재료: {}", String.join(", ", uniqueIngredients));
            log.info("사용자 알레르기 정보: {}", allergyTypes.isEmpty() ? "없음" : String.join(", ", allergyTypes));
            log.info("전체 레시피 수: {}", recipes.size());
            log.info("평가할 레시피 수: {}", recipes.size());
        }

        try {
            // 배치 처리
            recipesWithScores.addAll(processBatch(recipes, uniqueIngredients, allergyTypes));
        } catch (Exception e) {
            log.warn("배치 처리 실패, 개별 처리로 전환: {}", e.getMessage());
            recipesWithScores.addAll(processIndividual(recipes, uniqueIngredients, allergyTypes));
        }

        if (ENABLE_VERBOSE_LOG) log.info("=== 적합도 평가 완료 ===");
        return recipesWithScores;
    }

    /**
     * DB 레벨에서 랜덤 레시피 샘플링 (성능 최적화)
     *
     * @param sampleSize 샘플링할 레시피 개수
     * @return 랜덤하게 선택된 레시피 목록
     */
    private List<Recipe> getRandomRecipesForEvaluation(int sampleSize) {
        try {
            // 전체 레시피 수 조회
            long totalCount = recipeRepository.count();

            if (totalCount == 0) {
                return new ArrayList<>();
            }

            // 전체 레시피 수가 샘플 크기보다 작으면 모두 반환
            if (totalCount <= sampleSize) {
                return recipeRepository.findAll();
            }

            // 1차 시도: DB의 RANDOM() 함수를 사용한 효율적인 랜덤 샘플링
            try {
                List<Recipe> randomRecipes = recipeRepository.findRandomRecipes(sampleSize);
                if (randomRecipes.size() >= sampleSize) {
                    log.debug("DB RANDOM() 함수를 사용한 랜덤 샘플링 성공: {}개", randomRecipes.size());
                    return randomRecipes;
                }
            } catch (Exception e) {
                log.debug("DB RANDOM() 함수 실패, PK 범위 방식으로 대체: {}", e.getMessage());
            }

            // 2차 시도: PK 범위를 이용한 랜덤 샘플링 (대용량 테이블용)
            try {
                List<Recipe> randomRecipes = recipeRepository.findRandomRecipesByPkRange(sampleSize);
                if (randomRecipes.size() >= sampleSize) {
                    log.debug("PK 범위 랜덤 샘플링 성공: {}개", randomRecipes.size());
                    return randomRecipes;
                }
            } catch (Exception e) {
                log.debug("PK 범위 랜덤 샘플링 실패, 전체 조회로 대체: {}", e.getMessage());
            }

            // 3차 시도: 전체 조회 후 메모리에서 샘플링 (fallback)
            log.warn("DB 레벨 랜덤 샘플링 실패, 전체 조회 후 메모리 샘플링으로 대체");
            List<Recipe> allRecipes = recipeRepository.findAll();
            return sampleRecipesForEvaluation(allRecipes, sampleSize);

        } catch (Exception e) {
            log.error("레시피 랜덤 샘플링 중 예상치 못한 오류 발생: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 메모리에서 레시피 랜덤 샘플링 (fallback)
     */
    private List<Recipe> sampleRecipesForEvaluation(List<Recipe> recipes, int sampleSize) {
        if (recipes.size() <= sampleSize) {
            return recipes; // 샘플 크기보다 작으면 모두 사용
        }

        // 랜덤 샘플링
        List<Recipe> shuffled = new ArrayList<>(recipes);
        Collections.shuffle(shuffled);

        return shuffled.subList(0, sampleSize);
    }

    /**
     * 배치 처리 메서드
     */
    private List<RecipeWithScore> processBatch(List<Recipe> recipes,
                                               List<String> availableIngredients,
                                               List<String> allergyTypes) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        if (ENABLE_VERBOSE_LOG) {
            log.info("배치 처리 시작 - 사용자 재료: {}, 알레르기: {}",
                    String.join(", ", availableIngredients),
                    allergyTypes.isEmpty() ? "없음" : String.join(", ", allergyTypes));
        }

        try {
            // 배치 처리: 모든 레시피를 한 번에 AI에게 전달
            String batchPrompt = createBatchIngredientSuitabilityPrompt(recipes, availableIngredients, allergyTypes);
            if (ENABLE_VERBOSE_LOG) log.info("배치 프롬프트 생성 완료");

            String batchResponse = callAI(batchPrompt);
            if (ENABLE_VERBOSE_LOG) log.info("AI 배치 응답: {}", batchResponse);

            // AI 응답에서 각 레시피의 점수 파싱
            Map<String, Double> scores = parseBatchScores(batchResponse, recipes);
            log.info("파싱된 점수: {}", scores);

            for (Recipe recipe : recipes) {
                double score = scores.getOrDefault(recipe.getTitle(), 5.0);
                if (ENABLE_VERBOSE_LOG) log.info("레시피 '{}' 적합도 점수: {}", recipe.getTitle(), score);

                if (score > 0) {
                    recipesWithScores.add(new RecipeWithScore(recipe, score));
                } else {
                    log.info("레시피 {} 제외됨 (AI가 알레르기 충돌로 판단)", recipe.getTitle());
                }
            }

        } catch (Exception e) {
            log.warn("배치 적합도 평가 실패: {}", e.getMessage());
            throw e; // 상위에서 개별 처리로 전환하도록 예외 재발생
        }

        return recipesWithScores;
    }

    /**
     * 개별 처리 메서드
     */
    private List<RecipeWithScore> processIndividual(List<Recipe> recipes,
                                                    List<String> availableIngredients,
                                                    List<String> allergyTypes) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        if (ENABLE_VERBOSE_LOG) {
            log.info("개별 처리 시작 - 사용자 재료: {}, 알레르기: {}",
                    String.join(", ", availableIngredients),
                    allergyTypes.isEmpty() ? "없음" : String.join(", ", allergyTypes));
        }

        for (Recipe recipe : recipes) {
            try {
                log.info("레시피 '{}' 재료: {}", recipe.getTitle(), recipe.getIngredients());

                String prompt = createIngredientSuitabilityPrompt(recipe, availableIngredients, allergyTypes);
                double score = callAIForSuitabilityScore(prompt);

                if (ENABLE_VERBOSE_LOG) log.info("레시피 '{}' 적합도 점수: {}", recipe.getTitle(), score);

                if (score > 0) {
                    recipesWithScores.add(new RecipeWithScore(recipe, score));
                } else {
                    log.info("레시피 {} 제외됨 (AI가 알레르기 충돌로 판단)", recipe.getTitle());
                }
            } catch (Exception e) {
                log.warn("레시피 {} 적합도 평가 실패: {}", recipe.getTitle(), e.getMessage());
                recipesWithScores.add(new RecipeWithScore(recipe, 5.0));
            }
        }

        return recipesWithScores;
    }

    /**
     * 건강 정보 기반 배치 처리 메서드
     */
    private List<RecipeWithScore> processBatchByHealth(List<Recipe> recipes,
                                                       List<String> availableIngredients,
                                                       List<String> allergyTypes,
                                                       String healthInfo) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        if (ENABLE_VERBOSE_LOG) {
            log.info("건강 정보 기반 배치 처리 시작 - 사용자 재료: {}, 알레르기: {}, 건강정보: {}",
                    String.join(", ", availableIngredients),
                    allergyTypes.isEmpty() ? "없음" : String.join(", ", allergyTypes),
                    healthInfo);
        }

        try {
            // 배치 처리: 모든 레시피를 한 번에 AI에게 전달
            String batchPrompt = createBatchHealthSuitabilityPrompt(recipes, availableIngredients, allergyTypes, healthInfo);
            if (ENABLE_VERBOSE_LOG) log.info("건강 정보 기반 배치 프롬프트 생성 완료");

            String batchResponse = callAI(batchPrompt);
            if (ENABLE_VERBOSE_LOG) log.info("AI 배치 응답: {}", batchResponse);

            // AI 응답에서 각 레시피의 점수 파싱
            Map<String, Double> scores = parseBatchHealthScores(batchResponse, recipes);
            log.info("파싱된 점수: {}", scores);

            for (Recipe recipe : recipes) {
                double score = scores.getOrDefault(recipe.getTitle(), 5.0);
                if (ENABLE_VERBOSE_LOG) log.info("레시피 '{}' 적합도 점수: {}", recipe.getTitle(), score);

                if (score > 0) {
                    recipesWithScores.add(new RecipeWithScore(recipe, score));
                } else {
                    log.info("레시피 {} 제외됨 (AI가 알레르기 충돌로 판단)", recipe.getTitle());
                }
            }

        } catch (Exception e) {
            log.warn("건강 정보 기반 배치 적합도 평가 실패: {}", e.getMessage());
            throw e; // 상위에서 개별 처리로 전환하도록 예외 재발생
        }

        return recipesWithScores;
    }

    /**
     * 건강 정보 기반 개별 처리 메서드
     */
    private List<RecipeWithScore> processIndividualByHealth(List<Recipe> recipes,
                                                            List<String> availableIngredients,
                                                            List<String> allergyTypes,
                                                            String healthInfo) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        if (ENABLE_VERBOSE_LOG) {
            log.info("건강 정보 기반 개별 처리 시작 - 사용자 재료: {}, 알레르기: {}, 건강정보: {}",
                    String.join(", ", availableIngredients),
                    allergyTypes.isEmpty() ? "없음" : String.join(", ", allergyTypes),
                    healthInfo);
        }

        for (Recipe recipe : recipes) {
            try {
                log.info("레시피 '{}' 재료: {}", recipe.getTitle(), recipe.getIngredients());

                String prompt = createHealthSuitabilityPrompt(recipe, availableIngredients, allergyTypes, healthInfo);
                double score = callAIForSuitabilityScore(prompt);

                if (ENABLE_VERBOSE_LOG) log.info("레시피 '{}' 적합도 점수: {}", recipe.getTitle(), score);

                if (score > 0) {
                    recipesWithScores.add(new RecipeWithScore(recipe, score));
                } else {
                    log.info("레시피 {} 제외됨 (AI가 알레르기 충돌로 판단)", recipe.getTitle());
                }
            } catch (Exception e) {
                log.warn("레시피 {} 적합도 평가 실패: {}", recipe.getTitle(), e.getMessage());
                recipesWithScores.add(new RecipeWithScore(recipe, 5.0));
            }
        }

        return recipesWithScores;
    }

    /**
     * 건강 정보 기반 레시피 적합도 평가
     */
    private List<RecipeWithScore> evaluateRecipeSuitabilityByHealth(List<Recipe> recipes,
                                                                    List<String> availableIngredients,
                                                                    List<String> allergyTypes,
                                                                    String healthInfo) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        // 중복 제거만 수행
        List<String> uniqueIngredients = new ArrayList<>(new LinkedHashSet<>(availableIngredients));

        if (ENABLE_VERBOSE_LOG) {
            log.info("=== 건강 정보 기반 적합도 평가 시작 ===");
            log.info("사용자 보유 재료: {}", String.join(", ", uniqueIngredients));
            log.info("사용자 알레르기 정보: {}", allergyTypes.isEmpty() ? "없음" : String.join(", ", allergyTypes));
            log.info("사용자 건강 정보: {}", healthInfo);
            log.info("전체 레시피 수: {}", recipes.size());
            log.info("평가할 레시피 수: {}", recipes.size());
        }

        try {
            // 배치 처리
            recipesWithScores.addAll(processBatchByHealth(recipes, uniqueIngredients, allergyTypes, healthInfo));
        } catch (Exception e) {
            log.warn("배치 처리 실패, 개별 처리로 전환: {}", e.getMessage());
            recipesWithScores.addAll(processIndividualByHealth(recipes, uniqueIngredients, allergyTypes, healthInfo));
        }

        if (ENABLE_VERBOSE_LOG) log.info("=== 건강 정보 기반 적합도 평가 완료 ===");
        return recipesWithScores;
    }

    /**
     * 재료 기반 적합도 평가 프롬프트 생성
     */
    private String createIngredientSuitabilityPrompt(Recipe recipe,
                                                     List<String> availableIngredients,
                                                     List<String> allergyTypes) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("레시피 정보:\n")
                .append("- 제목: ").append(recipe.getTitle()).append("\n")
                .append("- 재료: ").append(recipe.getIngredients()).append("\n\n");

        // 우선순위 기반 통합 프롬프트 사용 (재료 중심)
        promptBuilder.append(buildPriorityBasedPrompt(availableIngredients, allergyTypes, null, new ArrayList<>()));
        promptBuilder.append("\n").append(buildPriorityBasedSuitabilityPromptCommon());

        return promptBuilder.toString();
    }

    /**
     * 건강 정보 기반 적합도 평가 프롬프트 생성 (우선순위 기반으로 통합)
     */
    private String createHealthSuitabilityPrompt(Recipe recipe,
                                                 List<String> availableIngredients,
                                                 List<String> allergyTypes,
                                                 String healthInfo) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("레시피 정보:\n")
                .append("- 제목: ").append(recipe.getTitle()).append("\n")
                .append("- 재료: ").append(recipe.getIngredients()).append("\n")
                .append("- 영양정보 - 칼로리: ").append(recipe.getCalories()).append("kcal, 단백질: ").append(recipe.getProtein()).append("g, 탄수화물: ").append(recipe.getCarbohydrate()).append("g, 지방: ").append(recipe.getFat()).append("g\n\n");

        // OCR 건강 정보를 Map으로 변환
        Map<String, String> ocrHealthData = new HashMap<>();
        if (healthInfo != null && !healthInfo.equals("건강 정보 없음")) {
            String[] lines = healthInfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("- ") && line.contains(":")) {
                    String[] parts = line.substring(2).split(":", 2);
                    if (parts.length == 2) {
                        ocrHealthData.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }

        // 우선순위 기반 통합 프롬프트 사용
        promptBuilder.append(buildPriorityBasedPrompt(availableIngredients, allergyTypes, ocrHealthData, new ArrayList<>()));
        promptBuilder.append("\n").append(buildPriorityBasedSuitabilityPromptCommon());

        return promptBuilder.toString();
    }

    /**
     * AI를 호출하여 적합도 점수를 받아오는 메서드
     */
    private double callAIForSuitabilityScore(String prompt) {
        try {
            String response = callAI(prompt);
            if (response == null || response.isEmpty()) {
                return 5.0; // 기본 점수
            }

            // AI 응답에서 점수 추출
            String scoreStr = response.trim();

            // JSON 응답인 경우 처리
            if (scoreStr.startsWith("{")) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(scoreStr);
                    String content = jsonNode.path("choices").path(0).path("message").path("content").asText();
                    if (!content.isEmpty()) {
                        scoreStr = content.trim();
                    }
                } catch (Exception e) {
                    log.warn("JSON 파싱 실패: {}", e.getMessage());
                    return 5.0;
                }
            }

            // 숫자만 추출 (소수점 포함)
            scoreStr = scoreStr.replaceAll("[^0-9.]", "");

            if (scoreStr.isEmpty()) {
                log.warn("점수 추출 실패, 기본값 5.0 사용");
                return 5.0;
            }

            double score = Double.parseDouble(scoreStr);

            // 점수 범위 검증 (0.0 ~ 10.0)
            if (score < 0.0 || score > 10.0) {
                log.warn("AI 응답의 점수가 범위를 벗어남: {}, 기본값 5.0 사용", score);
                return 5.0;
            }

            return score;
        } catch (Exception e) {
            log.warn("AI 적합도 평가 실패: {}, 기본값 5.0 사용", e.getMessage());
            return 5.0;
        }
    }



    private List<Recipe> callAIAndSaveRecipes(String prompt) {
        try {
            String response = callAI(prompt);
            if (response == null || response.isEmpty()) {
                throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
            }

            // Gemini 클라이언트는 이미 텍스트 콘텐츠를 반환하므로 바로 사용
            if (ENABLE_VERBOSE_LOG) log.info("AI 원본 응답: {}", response);

            // AI 응답에서 JSON 부분 추출 (코드블록 제거)
            String jsonContent = extractJsonFromAIResponse(response);
            if (ENABLE_VERBOSE_LOG) log.info("추출된 JSON: {}", jsonContent);

            // AI 응답을 JSON으로 파싱
            JsonNode recommendationsRoot = objectMapper.readTree(jsonContent);
            JsonNode recommendationsNode = recommendationsRoot.path("recommendations");
            if (recommendationsNode.isMissingNode() || !recommendationsNode.isArray()) {
                throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
            }

            List<Recipe> recipes = new ArrayList<>();

            for (JsonNode rec : recommendationsNode) {
                try {
                    Recipe recipe = parseRecipeFromJson(rec);
                    if (recipe != null && !isDuplicateRecipe(recipe)) {
                        // DB 저장 전에 이미지 URL 미리 가져오기
                        String imageUrl = recipeBlogImageService.searchRecipeImage(recipe.getTitle());
                        if (isValidHttpUrl(imageUrl) && imageUrl.length() <= MAX_IMAGE_URL_LENGTH) {
                            recipe.setRecipeImage(imageUrl);
                            log.info("레시피 이미지 설정 완료: {} -> {}", recipe.getTitle(), imageUrl);

                            // 이미지가 있는 경우에만 DB에 저장
                            Recipe savedRecipe = recipeRepository.save(recipe);
                            recipes.add(savedRecipe);
                            log.info("레시피 저장 성공: {}", recipe.getTitle());

                            // DB 저장 성공 시 Redis에 제목 기반으로 캐싱 (30일)
                            cacheRecipeTitle(savedRecipe.getTitle());
                        } else {
                            log.warn("레시피 '{}' 이미지 검증 실패(유효한 URL 아님 또는 길이 초과). DB 등록을 건너뜁니다. url='{}'", recipe.getTitle(), imageUrl);
                        }
                    } else {
                        log.info("중복 레시피 제외: {}", recipe != null ? recipe.getTitle() : "null");
                    }
                } catch (Exception e) {
                    log.warn("레시피 파싱/저장 실패: {}", e.getMessage());
                }
            }

            if (recipes.isEmpty()) {
                throw new BusinessException(ErrorCode.OPENAI_EMPTY_RECOMMENDATIONS);
            }

            return recipes;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 추천 및 저장 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
    }





    /**
     * AI 응답에서 JSON 부분 추출 (코드블록, 마크다운 등 제거)
     */
    private String extractJsonFromAIResponse(String aiContent) {
        // ```json ... ``` 또는 ``` ... ``` 코드블록 제거
        String content = aiContent.trim();

        // 코드블록 패턴 제거
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }

        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }

        content = content.trim();

        // 중괄호로 시작하고 끝나는 JSON 찾기
        int startBrace = content.indexOf("{");
        int endBrace = content.lastIndexOf("}");

        if (startBrace >= 0 && endBrace > startBrace) {
            return content.substring(startBrace, endBrace + 1);
        }

        // JSON을 찾을 수 없으면 원본 반환
        return content;
    }

    /**
     * JSON에서 레시피 정보를 직접 파싱 (간소화)
     */
    private Recipe parseRecipeFromJson(JsonNode rec) {
        try {
            String title = rec.path("title").asText();
            String description = rec.path("description").asText();
            String category = rec.path("category").asText();
            String ingredients = rec.path("ingredients").asText();

            // 필수 필드 검증
            if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || category.isEmpty()) {
                log.warn("필수 필드 누락 - title: {}, category: {}", title, category);
                return null;
            }

            Recipe recipe = new Recipe();
            recipe.setTitle(title);
            recipe.setRecipeImage("default-recipe-image.jpg");
            recipe.setDescription(description);
            recipe.setCookingTime(rec.path("cookingTime").asLong(0));
            recipe.setCalories(rec.path("calories").asLong(0));
            recipe.setProtein(rec.path("protein").asLong(0));
            recipe.setCarbohydrate(rec.path("carbohydrate").asLong(0));
            recipe.setFat(rec.path("fat").asLong(0));
            recipe.setCategories(parseCategories(category));
            recipe.setIngredients(ingredients);

            return recipe;
        } catch (Exception e) {
            log.warn("레시피 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 레시피 중복 검사 (Redis → DB 순서로 체크)
     * Search domain과 동일한 방식으로 ZSet 사용
     */
    private boolean isDuplicateRecipe(Recipe recipe) {
        String title = recipe.getTitle();

        // 1. Redis ZSet에서 제목 기반 중복 체크 (search domain과 동일한 방식)
        try {
            Double score = redisTemplate.opsForZSet().score(RECIPE_TITLES_KEY, title);
            if (score != null) {
                log.info("Redis에서 중복 레시피 발견: {}", title);
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis 중복 체크 실패: {}", e.getMessage());
        }

        // 2. DB에서 제목 기반 중복 체크
        boolean isDuplicate = recipeRepository.existsByTitle(title);
        if (isDuplicate) {
            log.info("DB에서 중복 레시피 발견: {}", title);
            // Redis ZSet에도 중복 정보 추가 (search domain과 동일한 방식)
            try {
                // score는 시간 기반으로 부여하면 정리에 유리합니다.
                redisTemplate.opsForZSet().add(RECIPE_TITLES_KEY, title, System.currentTimeMillis());
            } catch (Exception e) {
                log.warn("Redis 중복 정보 추가 실패: {}", e.getMessage());
            }
        }

        return isDuplicate;
    }



    /**
     * 카테고리 정보를 파싱하는 메서드
     */
    private List<RecipeCategory> parseCategories(String category) {
        try {
            String[] categoryArray = category.split(",");
            List<RecipeCategory> recipeCategories = new ArrayList<>();

            for (String cat : categoryArray) {
                try {
                    RecipeCategory recipeCategory = RecipeCategory.valueOf(cat.trim());
                    recipeCategories.add(recipeCategory);
                } catch (IllegalArgumentException e) {
                    log.warn("알 수 없는 카테고리: {}", cat.trim());
                }
            }

            if (recipeCategories.isEmpty()) {
                log.warn("모든 카테고리가 유효하지 않음: {}, OTHER로 설정", category);
                recipeCategories.add(RecipeCategory.OTHER);
            }

            return recipeCategories;
        } catch (Exception e) {
            log.warn("카테고리 처리 실패: {}, OTHER로 설정", category);
            return Arrays.asList(RecipeCategory.OTHER);
        }
    }

    // Gemini API를 사용하여 AI 호출 (Pro 모델 사용)
    private String callAI(String prompt) {
        try {
            return geminiClient.chatAccurate(prompt).block(Duration.ofSeconds(30));
        } catch (Exception e) {
            log.error("Gemini Pro API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
    }



    // 2. 역할별 private 메서드 분리 및 간결화
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private List<String> getUserIngredients(Long userId) {
        List<IngredientResponse.RetrieveRes> ingredients = ingredientService.getAllIngredient(userId);
        if (ingredients.isEmpty()) {
            log.warn("사용자 {}의 재료 정보가 없습니다.", userId);
            throw new BusinessException(ErrorCode.RECIPE_EMPTY_INGREDIENTS);
        }
        return ingredients.stream()
                .map(IngredientResponse.RetrieveRes::getName)
                .collect(Collectors.toList());
    }

    private List<String> getUserAllergies(Long userId) {
        try {
            AllergyResponse.AllergyListRes allergyList = allergyService.getAllergyList(userId);
            return allergyList.getAllergyOptions().stream()
                    .map(a -> a.getAllergyType().name())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("사용자 {}의 알레르기 정보 조회 실패: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }







    /**
     * 공통 프롬프트 부분 생성
     */
    private String buildRecipePostPromptCommon() {
        return "너는 사용자가 제시한 재료를 바탕으로, 현실적이고 맛있는 요리 레시피를 추천하는 전문 레시피 큐레이터 AI다. 너의 임무는 주어진 규칙을 엄격히 준수하여, 사용자가 실제로 만들고 싶어지는 훌륭한 레시피를 JSON 형식으로 생성하는 것이다.\n\n" +
                "## 1. 최상위 원칙: 억지 조합 절대 금지\n\n" +
                "가장 중요한 원칙은 **'현실적인 요리'** 추천이다. 사용자가 제시한 재료 목록을 해석할 때, 아래의 지침을 반드시 따른다.\n\n" +
                "* **'A 또는 B' 해석:** 입력에 '또는', '/' 등이 포함된 경우, 이는 **별개의 선택지**임을 의미한다. '코드 또는 햇살숭어'가 주어지면, '코드'를 사용한 요리 **혹은** '햇살숭어'를 사용한 요리를 각각 추천해야 한다. **두 재료를 한 요리에 섞는 것은 절대 금지된다.**\n" +
                "* **'(선택)' 재료 해석:** '(선택)', '(옵션)' 등으로 표시된 재료는 필수가 아니다. 해당 재료가 없어도 완성되는 레시피를 우선적으로 고려해야 한다.\n" +
                "* **재료의 본질 파악:** 사용자의 입력은 '만들 요리 재료 목록'이 아니라, '사용 가능한 재료 팔레트'이다. 이 팔레트에서 **가장 적절한 일부 재료를 선택**하여 만들 수 있는 최상의 요리를 추천해야 한다.\n\n" +
                "---\n\n" +
                "## 2. 작업 순서 (Workflow)\n\n" +
                "너는 다음의 사고 과정에 따라 작업을 수행해야 한다.\n\n" +
                "1.  **입력 재료 분석 (Parse):** 사용자의 재료 목록을 보고 '또는', '(선택)' 등의 논리적/선택적 요소를 파악하여 재료 그룹을 나눈다.\n" +
                "2.  **핵심 재료 선정 (Select Core Ingredient):** 분석된 재료 그룹 중, 요리의 중심이 될 **하나의 핵심 재료**를 선택한다. (예: '코드'를 선택)\n" +
                "3.  **레시피 아이디어 생성 (Brainstorm):** 선택된 핵심 재료로 만들 수 있는, 대중적이고 상식적인 요리 아이디어를 떠올린다. (예: '대구 맑은탕', '대구 스테이크')\n" +
                "4.  **규칙 검증 (Validate):** 생성된 아이디어가 아래 **[3. 레시피 생성 상세 규칙]**을 모두 만족하는지 엄격하게 검증한다. 특히, 레시피 제목과 재료 구성이 규칙에 맞는지 확인한다.\n" +
                "5.  **JSON 형식화 (Format):** 검증을 통과한 레시피만 최종 JSON 형식으로 출력한다.\n\n" +
                "---\n\n" +
                "## 3. 레시피 생성 상세 규칙\n\n" +
                "### ※ 레시피 제목 (Title)\n" +
                "* **보편성:** 누구나 검색해서 조리법을 찾을 수 있는 대중적인 이름이어야 한다.\n" +
                "    * (O) 된장찌개, 토마토 바질 파스타\n" +
                "    * (X) 돼지고기 앞다리살 감자 상추 된장찌개\n" +
                "* **재료 명시:** 요리의 정체성을 보여주는 **핵심 재료 1~2개**를 제목에 포함한다.\n" +
                "    * 단, 파스타, 덮밥, 볶음밥 등 주식(탄수화물) 기반 요리는 최대 3개까지 허용.\n" +
                "    * (O) 고추장 돼지고기 볶음\n" +
                "    * (X) 고추장 감자 양파 돼지고기 볶음\n\n" +
                "### ※ 재료 구성 (Ingredients)\n" +
                "* **필수 재료 포함:** 해당 요리를 만드는 데 필요한 **주요 재료를 최소 2개 이상** 빠짐없이 포함한다. (조미료도 명시)\n" +
                "    * (예) 제육볶음 → 돼지고기, 양파, 고추장, 간장, 설탕, 식용유 (6개 이상)\n" +
                "* **선택 재료 제외:** 필수가 아닌 선택(옵션) 재료는 포함하지 않는다.\n" +
                "* **일반화:** 대체 가능한 여러 재료는 포괄적인 상위 개념으로 작성한다.\n" +
                "    * (예) 앞다리살, 삼겹살, 목살 → `돼지고기`\n" +
                "    * (예) 상추, 로메인, 양상추 → `샐러드 채소`\n" +
                "    * (예) 숭어, 대구 → `흰살생선`\n" +
                "* **언어:** 제목, 재료, 설명 모두 **순수 한글**로만 작성한다. (외래어 표기는 허용: 스파게티, 파스타)\n\n" +
                "### ※ 카테고리 (Category)\n" +
                "* **적절성:** 각 요리의 특성에 맞는 카테고리를 아래 목록에서 선택한다. (여러 개 선택 가능, 쉼표로 구분)\n" +
                "* **기타:** 적절한 카테고리가 없을 경우 'OTHER'를 단독으로 사용한다.\n" +
                "* **사용 가능 카테고리:** `BODY_WEIGHT_MANAGEMENT`, `HEALTH_MANAGEMENT`, `WEIGHT_LOSS`, `MUSCLE_GAIN`, `SUGAR_REDUCTION`, `BLOOD_PRESSURE`, `CHOLESTEROL`, `DIGESTION`, `OTHER`\n\n" +
                "---\n\n" +
                "## 4. JSON 출력 형식\n\n" +
                "* 아래 명시된 JSON 구조와 필드명을 반드시 준수한다.\n" +
                "* `description`은 UI 상단에 한 줄로 보여줄 예정이므로, 간결하고 매력적으로 작성한다. (예: '바삭한 계란전으로 든든한 한끼 완성!')\n" +
                "* 영양 정보(칼로리, 단백질 등)는 일반적인 해당 요리의 평균값을 추정하여 기입한다.\n\n" +
                "{\n" +
                "  \"recommendations\": [\n" +
                "    {\n" +
                "      \"title\": \"레시피 제목\",\n" +
                "      \"description\": \"레시피 설명\",\n" +
                "      \"ingredients\": \"재료1, 재료2, 재료3\",\n" +
                "      \"category\": \"BODY_WEIGHT_MANAGEMENT,HEALTH_MANAGEMENT\",\n" +
                "      \"cookingTime\": 30,\n" +
                "      \"calories\": 300,\n" +
                "      \"protein\": 20,\n" +
                "      \"carbohydrate\": 30,\n" +
                "      \"fat\": 10\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
    }

    /**
     * 알러지 정보 프롬프트 생성 (공통)
     */
    private String buildAllergyPrompt(List<String> allergyTypes) {
        if (allergyTypes == null || allergyTypes.isEmpty()) {
            return "※ 사용자 알레르기 정보: 없음 (알레르기 제한 없음)\n";
        }

        return "※ 사용자 알레르기 정보: " + String.join(", ", allergyTypes) + "\n" +
                "※ 중요: 위 알레르기 성분이 포함된 요리는 절대 추천하지 마세요.\n";
    }

    /**
     * 재료 기반 프롬프트 생성
     */
    private String buildRecipePostPromptIngredient(List<String> availableIngredients) {
        StringBuilder promptBuilder = new StringBuilder();

        // 중복 제거만 수행
        List<String> uniqueIngredients = new ArrayList<>(new LinkedHashSet<>(availableIngredients));

        promptBuilder.append("사용자가 보유한 식재료 목록을 기반으로, 실존하는 요리를 추천해줘.\n\n")
                .append(buildRecipePostPromptCommon())
                .append("\n\n※ 사용자가 보유한 식재료 목록:\n")
                .append(String.join(", ", uniqueIngredients)).append("\n\n")
                .append("\n위 재료들을 활용하여 만들 수 있는 보편적인 요리를 ").append(POST_RECIPE_COUNT).append("가지 추천해줘.");

        String prompt = promptBuilder.toString();
        log.info("프롬프트 길이: {} characters", prompt.length());
        log.info("전달된 재료: {} (중복제거 후: {}개)", String.join(", ", uniqueIngredients), uniqueIngredients.size());

        return prompt;
    }

    /**
     * 랜덤 프롬프트 생성
     * 주제가 제공되면 해당 주제와 연관된 레시피를 생성하고, 없으면 완전 랜덤하게 생성합니다.
     *
     * @param topic 선택적 주제 (null이면 완전 랜덤)
     */
    private String buildRecipePostPromptRandom(String topic) {
        StringBuilder prompt = new StringBuilder();

        // 주제가 null이거나 빈 문자열인 경우 로깅
        if (topic == null) {
            log.info("주제가 null이므로 완전 랜덤 레시피 생성");
        } else if (topic.trim().isEmpty()) {
            log.info("주제가 빈 문자열이므로 완전 랜덤 레시피 생성");
        } else {
            log.info("주제 '{}' 기반 레시피 생성", topic.trim());
            prompt.append(String.format("'%s' 주제와 연관된 ", topic.trim()));
        }

        prompt.append("다양한 요리 레시피를 추천해줘.\n\n")
                .append(buildRecipePostPromptCommon())
                .append("\n\n※ 알레르기 주의사항:\n")
                .append("- 일반적인 알레르기 유발 성분(우유, 계란, 대두, 밀, 땅콩, 견과류, 조개류, 생선 등)이 포함된 요리도 추천 가능\n")
                .append("- 사용자가 개별적으로 알레르기 정보를 확인하고 선택하도록 안내\n\n")
                .append("총 ").append(POST_RECIPE_COUNT).append("개의 다양한 보편적인 요리를 추천해줘.");

        String finalPrompt = prompt.toString();
        log.info("생성된 프롬프트 길이: {} 문자", finalPrompt.length());

        return finalPrompt;
    }

    /**
     * 키워드 기반 레시피 프롬프트 생성
     * 사용자가 제공한 키워드를 최우선으로 고려하여 관련 레시피만 생성
     *
     * @param keyword 사용자가 제공한 키워드
     */
    private String buildRecipeKeywordPrompt(String keyword) {
        StringBuilder prompt = new StringBuilder();

        if (keyword != null && !keyword.trim().isEmpty()) {
            prompt.append("🚨 최우선 조건: '").append(keyword.trim()).append("'과 관련된 레시피만 생성\n");
            prompt.append("⚠️ 해당 키워드와 무관한 요리는 절대 포함하지 마세요\n\n");
        }

        prompt.append("다양한 요리 레시피를 추천해줘.\n\n")
                .append(buildRecipePostPromptCommon())
                .append("\n\n※ 알레르기 주의사항:\n")
                .append("- 일반적인 알레르기 유발 성분(우유, 계란, 대두, 밀, 땅콩, 견과류, 조개류, 생선 등)이 포함된 요리도 추천 가능\n")
                .append("- 사용자가 개별적으로 알레르기 정보를 확인하고 선택하도록 안내\n\n")
                .append("총 ").append(POST_RECIPE_COUNT).append("개의 다양한 보편적인 요리를 추천해줘.");

        return prompt.toString();
    }

    // 기존 프롬프트 생성 메서드들은 우선순위 기반 통합 프롬프트로 대체됨

    /**
     * 배치 적합도 평가 프롬프트 생성
     */
    private String createBatchIngredientSuitabilityPrompt(List<Recipe> recipes,
                                                          List<String> availableIngredients,
                                                          List<String> allergyTypes) {
        // OpenAI 토큰 제한 고려 (대략 1토큰 = 4글자)
        int maxPromptLength = 12000; // 약 3000 토큰

        StringBuilder promptBuilder = new StringBuilder();

        // 우선순위 기반 통합 프롬프트 사용 (재료 중심)
        promptBuilder.append(buildPriorityBasedPrompt(availableIngredients, allergyTypes, null, new ArrayList<>()));
        promptBuilder.append("다음 레시피들의 적합도를 평가해줘:\n\n");

        int currentLength = promptBuilder.length();
        for (Recipe recipe : recipes) {
            String recipeText = "레시피: " + recipe.getTitle() + "\n" +
                    "재료: " + recipe.getIngredients() + "\n\n";

            if (currentLength + recipeText.length() > maxPromptLength) {
                log.warn("프롬프트 크기 제한 초과, {} 개 레시피만 처리",
                        recipes.indexOf(recipe));
                break;
            }

            promptBuilder.append(recipeText);
            currentLength += recipeText.length();
        }

        promptBuilder.append(buildPriorityBasedSuitabilityPromptCommon().replace("=== 🎯 적합도 평가 기준 (우선순위 순) ===", "평가 기준:")).append("\n\n")
                .append("반드시 다음 JSON 형태로만 응답해줘:\n")
                .append("{\n")
                .append("  \"scores\": {\n");

        // 실제 처리된 레시피만 JSON에 포함
        int processedCount = 0;
        for (Recipe recipe : recipes) {
            if (processedCount >= Math.min(recipes.size(),
                    (maxPromptLength - currentLength) / 50)) { // 대략적인 레시피당 길이
                break;
            }
            promptBuilder.append("    \"").append(recipe.getTitle()).append("\": 점수");
            if (processedCount < Math.min(recipes.size() - 1,
                    (maxPromptLength - currentLength) / 50 - 1)) {
                promptBuilder.append(",");
            }
            promptBuilder.append("\n");
            processedCount++;
        }

        promptBuilder.append("  }\n")
                .append("}\n\n")
                .append("다른 설명이나 텍스트 없이 JSON만 응답해주세요.");

        return promptBuilder.toString();
    }

    /**
     * 배치 응답에서 점수 파싱 (간소화)
     */
    private Map<String, Double> parseBatchScores(String response, List<Recipe> recipes) {
        Map<String, Double> scores = new HashMap<>();

        try {
            if (ENABLE_VERBOSE_LOG) log.info("AI 응답 내용: {}", response);

            // Gemini API 응답에서 JSON 부분 추출 (코드블록 제거)
            String jsonContent = extractJsonFromAIResponse(response);
            if (ENABLE_VERBOSE_LOG) log.info("추출된 JSON: {}", jsonContent);

            // AI 응답을 JSON으로 파싱
            JsonNode scoresJson = objectMapper.readTree(jsonContent);
            JsonNode scoresNode = scoresJson.path("scores");

            if (!scoresNode.isMissingNode()) {
                if (ENABLE_VERBOSE_LOG) log.info("점수 노드 발견: {}", scoresNode.toString());

                for (Recipe recipe : recipes) {
                    double score = scoresNode.path(recipe.getTitle()).asDouble(5.0);
                    scores.put(recipe.getTitle(), score);
                    if (ENABLE_VERBOSE_LOG) log.info("레시피 '{}' 점수: {}", recipe.getTitle(), score);
                }
            } else {
                log.warn("scores 노드를 찾을 수 없습니다.");
                // 모든 레시피에 기본 점수 부여
                for (Recipe recipe : recipes) {
                    scores.put(recipe.getTitle(), 5.0);
                }
            }
        } catch (Exception e) {
            log.warn("배치 점수 파싱 실패: {}", e.getMessage());
            // 파싱 실패 시 모든 레시피에 기본 점수 부여
            for (Recipe recipe : recipes) {
                scores.put(recipe.getTitle(), 5.0);
            }
        }

        if (ENABLE_VERBOSE_LOG) log.info("최종 파싱 결과: {}", scores);
        return scores;
    }

    /**
     * 사용자의 레시피 찜 여부를 일괄 조회합니다.
     * 기존 메서드 활용하여 최적화
     */
    private Map<Long, Boolean> getUserRecipeLikedMap(Long userId, List<Long> recipeIds) {
        try {
            // 기존 findByUserIdAndRecipeIdIn 메서드 사용 (이미 @EntityGraph로 최적화됨)
            List<UserRecipe> userRecipes = userRecipeRepository.findByUserIdAndRecipeIdIn(userId, recipeIds);
            Map<Long, Boolean> likedMap = new HashMap<>();

            // 모든 레시피에 대해 기본값 false 설정
            for (Long recipeId : recipeIds) {
                likedMap.put(recipeId, false);
            }

            // 찜한 레시피만 true로 업데이트
            for (UserRecipe userRecipe : userRecipes) {
                likedMap.put(userRecipe.getRecipe().getId(), Boolean.TRUE.equals(userRecipe.getLiked()));
            }

            return likedMap;
        } catch (Exception e) {
            log.warn("사용자 찜 여부 조회 실패: {} - {}", userId, e.getMessage());
            // 예외 발생 시에도 모든 레시피에 대해 false 반환
            Map<Long, Boolean> fallbackMap = new HashMap<>();
            for (Long recipeId : recipeIds) {
                fallbackMap.put(recipeId, false);
            }
            return fallbackMap;
        }
    }

    /**
     * 최신 OCR 건강 데이터 조회
     */
    private Map<String, String> getLatestOcrHealthData(Long userId) {
        try {
            // UserHealthDataRepository를 통해 최신 OCR 데이터 조회
            List<UserHealthData> userHealthDataList = userHealthDataRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (!userHealthDataList.isEmpty()) {
                return userHealthDataList.get(0).getExtractedData();
            }
            return new HashMap<>();
        } catch (Exception e) {
            log.warn("OCR 건강 데이터 조회 실패: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * OCR 건강 데이터를 기반으로 건강 정보 문자열 생성
     */
    private String buildOcrHealthInfo(Map<String, String> ocrHealthData) {
        if (ocrHealthData == null || ocrHealthData.isEmpty()) {
            return "건강 정보 없음";
        }

        StringBuilder healthInfo = new StringBuilder("현재 건강 상태:\n");
        ocrHealthData.forEach((key, value) -> {
            if (value != null && !value.trim().isEmpty()) {
                healthInfo.append("- ").append(key).append(": ").append(value).append("\n");
            }
        });

        return healthInfo.toString();
    }

    /**
     * 사용자 HealthGoal 목록 조회
     */
    private List<String> getUserHealthGoals(Long userId) {
        try {
            log.info("사용자 {}의 HealthGoal 조회 시작", userId);

            // HealthGoalService를 통해 사용자의 건강 목표 조회
            var healthGoalResponse = healthGoalService.getHealthGoalList(userId);

            if (healthGoalResponse != null && healthGoalResponse.getHealthGoalList() != null) {
                List<String> healthGoals = healthGoalResponse.getHealthGoalList().stream()
                        .filter(goal -> goal != null && goal.getHealthGoalType() != null)
                        .map(goal -> goal.getHealthGoalType().name())
                        .collect(Collectors.toList());

                log.info("사용자 {}의 HealthGoal 조회 성공: {}", userId, healthGoals);
                return healthGoals;
            } else {
                log.warn("사용자 {}의 HealthGoal 응답이 null이거나 빈 목록", userId);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("사용자 {}의 HealthGoal 조회 실패: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * HealthGoal 기반 적합도 평가
     */
    private List<RecipeWithScore> evaluateRecipeSuitabilityByHealthGoal(List<Recipe> recipes,
                                                                        List<String> availableIngredients,
                                                                        List<String> allergyTypes,
                                                                        List<String> healthGoals) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        log.info("=== HealthGoal 기반 적합도 평가 시작 ===");
        log.info("사용자 보유 재료: {}", String.join(", ", availableIngredients));
        log.info("사용자 알레르기 정보: {}", allergyTypes.isEmpty() ? "없음" : String.join(", ", allergyTypes));
        log.info("사용자 건강 목표: {}", String.join(", ", healthGoals));
        log.info("전체 레시피 수: {}", recipes.size());

        try {
            // 배치 처리
            recipesWithScores.addAll(processBatchByHealthGoal(recipes, availableIngredients, allergyTypes, healthGoals));
        } catch (Exception e) {
            log.warn("배치 처리 실패, 개별 처리로 전환: {}", e.getMessage());
            recipesWithScores.addAll(processIndividualByHealthGoal(recipes, availableIngredients, allergyTypes, healthGoals));
        }

        log.info("=== HealthGoal 기반 적합도 평가 완료 ===");
        return recipesWithScores;
    }

    /**
     * 통합 적합도 평가 (재료 + OCR + HealthGoal)
     */
    private List<RecipeWithScore> evaluateRecipeSuitabilityByCombined(List<Recipe> recipes,
                                                                      List<String> availableIngredients,
                                                                      List<String> allergyTypes,
                                                                      Map<String, String> ocrHealthData,
                                                                      List<String> healthGoals) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        log.info("=== 통합 적합도 평가 시작 ===");
        log.info("사용자 보유 재료: {}", String.join(", ", availableIngredients));
        log.info("사용자 알레르기 정보: {}", allergyTypes.isEmpty() ? "없음" : String.join(", ", allergyTypes));
        log.info("OCR 건강 데이터: {}", ocrHealthData != null ? ocrHealthData.size() + "개 항목" : "없음");
        log.info("사용자 건강 목표: {}", String.join(", ", healthGoals));
        log.info("전체 레시피 수: {}", recipes.size());

        try {
            // 배치 처리
            recipesWithScores.addAll(processBatchByCombined(recipes, availableIngredients, allergyTypes, ocrHealthData, healthGoals));
        } catch (Exception e) {
            log.warn("배치 처리 실패, 개별 처리로 전환: {}", e.getMessage());
            recipesWithScores.addAll(processIndividualByCombined(recipes, availableIngredients, allergyTypes, ocrHealthData, healthGoals));
        }

        log.info("=== 통합 적합도 평가 완료 ===");
        return recipesWithScores;
    }

    /**
     * HealthGoal 기반 배치 처리 메서드
     */
    private List<RecipeWithScore> processBatchByHealthGoal(List<Recipe> recipes,
                                                           List<String> availableIngredients,
                                                           List<String> allergyTypes,
                                                           List<String> healthGoals) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        log.info("HealthGoal 기반 배치 처리 시작 - 사용자 재료: {}, 알레르기: {}, 건강목표: {}",
                String.join(", ", availableIngredients),
                allergyTypes.isEmpty() ? "없음" : String.join(", ", allergyTypes),
                String.join(", ", healthGoals));

        try {
            // 배치 처리: 모든 레시피를 한 번에 AI에게 전달
            String batchPrompt = createBatchHealthGoalSuitabilityPrompt(recipes, availableIngredients, allergyTypes, healthGoals);
            log.info("HealthGoal 기반 배치 프롬프트 생성 완료");

            String batchResponse = callAI(batchPrompt);
            log.info("AI 배치 응답: {}", batchResponse);

            // AI 응답에서 각 레시피의 점수 파싱
            Map<String, Double> scores = parseBatchHealthGoalScores(batchResponse, recipes);
            log.info("파싱된 점수: {}", scores);

            for (Recipe recipe : recipes) {
                double score = scores.getOrDefault(recipe.getTitle(), 5.0);
                log.info("레시피 '{}' 적합도 점수: {}", recipe.getTitle(), score);

                if (score > 0) {
                    recipesWithScores.add(new RecipeWithScore(recipe, score));
                } else {
                    log.info("레시피 {} 제외됨 (AI가 알레르기 충돌로 판단)", recipe.getTitle());
                }
            }

        } catch (Exception e) {
            log.warn("HealthGoal 기반 배치 적합도 평가 실패: {}", e.getMessage());
            throw e; // 상위에서 개별 처리로 전환하도록 예외 재발생
        }

        return recipesWithScores;
    }

    /**
     * HealthGoal 기반 개별 처리 메서드
     */
    private List<RecipeWithScore> processIndividualByHealthGoal(List<Recipe> recipes,
                                                                List<String> availableIngredients,
                                                                List<String> allergyTypes,
                                                                List<String> healthGoals) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        log.info("HealthGoal 기반 개별 처리 시작 - 사용자 재료: {}, 알레르기: {}, 건강목표: {}",
                String.join(", ", availableIngredients),
                allergyTypes.isEmpty() ? "없음" : String.join(", ", allergyTypes),
                String.join(", ", healthGoals));

        for (Recipe recipe : recipes) {
            try {
                log.info("레시피 '{}' 재료: {}", recipe.getTitle(), recipe.getIngredients());

                String prompt = createHealthGoalSuitabilityPrompt(recipe, availableIngredients, allergyTypes, healthGoals);
                double score = callAIForSuitabilityScore(prompt);

                log.info("레시피 '{}' 적합도 점수: {}", recipe.getTitle(), score);

                if (score > 0) {
                    recipesWithScores.add(new RecipeWithScore(recipe, score));
                } else {
                    log.info("레시피 {} 제외됨 (AI가 알레르기 충돌로 판단)", recipe.getTitle());
                }
            } catch (Exception e) {
                log.warn("레시피 {} 적합도 평가 실패: {}", recipe.getTitle(), e.getMessage());
                recipesWithScores.add(new RecipeWithScore(recipe, 5.0));
            }
        }

        return recipesWithScores;
    }

    /**
     * HealthGoal 기반 적합도 평가 프롬프트 생성 (우선순위 기반으로 통합)
     */
    private String createHealthGoalSuitabilityPrompt(Recipe recipe,
                                                     List<String> availableIngredients,
                                                     List<String> allergyTypes,
                                                     List<String> healthGoals) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("레시피 정보:\n")
                .append("- 제목: ").append(recipe.getTitle()).append("\n")
                .append("- 재료: ").append(recipe.getIngredients()).append("\n")
                .append("- 설명: ").append(recipe.getDescription()).append("\n\n");

        // 우선순위 기반 통합 프롬프트 사용
        promptBuilder.append(buildPriorityBasedPrompt(availableIngredients, allergyTypes, null, healthGoals));
        promptBuilder.append("\n").append(buildPriorityBasedSuitabilityPromptCommon());

        return promptBuilder.toString();
    }

    /**
     * 통합 적합도 평가 프롬프트 생성 (우선순위 기반으로 통합)
     */
    private String createCombinedSuitabilityPrompt(Recipe recipe,
                                                   List<String> availableIngredients,
                                                   List<String> allergyTypes,
                                                   Map<String, String> ocrHealthData,
                                                   List<String> healthGoals) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("레시피 정보:\n")
                .append("- 제목: ").append(recipe.getTitle()).append("\n")
                .append("- 재료: ").append(recipe.getIngredients()).append("\n")
                .append("- 설명: ").append(recipe.getDescription()).append("\n\n");

        // 우선순위 기반 통합 프롬프트 사용
        promptBuilder.append(buildPriorityBasedPrompt(availableIngredients, allergyTypes, ocrHealthData, healthGoals));
        promptBuilder.append("\n").append(buildPriorityBasedSuitabilityPromptCommon());

        return promptBuilder.toString();
    }

    /**
     * HealthGoal 기반 적합도 평가 공통 프롬프트 생성 (우선순위 기반으로 통합)
     */
    private String buildHealthGoalSuitabilityPromptCommon() {
        return buildPriorityBasedSuitabilityPromptCommon();
    }

    /**
     * 통합 적합도 평가 공통 프롬프트 생성
     */
    private String buildCombinedSuitabilityPromptCommon() {
        return buildPriorityBasedSuitabilityPromptCommon();
    }

    /**
     * Recipe를 RecipeSummaryDTO로 변환
     */
    private UserRecipeResponse.RecipeSummaryDTO toRecipeSummaryDTO(Recipe recipe) {
        return UserRecipeResponse.RecipeSummaryDTO.builder()
                .recipeId(recipe.getId())
                .name(recipe.getTitle())
                .imageUrl(recipe.getRecipeImage())
                .liked(false) // 기본값은 false, 별도로 설정해야 함
                .build();
    }

    /**
     * 건강 정보 기반 배치 프롬프트 생성
     */
    private String createBatchHealthSuitabilityPrompt(List<Recipe> recipes,
                                                      List<String> availableIngredients,
                                                      List<String> allergyTypes,
                                                      String healthInfo) {
        // OpenAI 토큰 제한 고려 (대략 1토큰 = 4글자)
        int maxPromptLength = 12000; // 약 3000 토큰

        StringBuilder promptBuilder = new StringBuilder();

        // 우선순위 기반 통합 프롬프트 사용 (OCR 건강 정보 포함)
        Map<String, String> ocrHealthData = new HashMap<>();
        if (healthInfo != null && !healthInfo.equals("건강 정보 없음")) {
            // healthInfo 문자열을 파싱하여 Map으로 변환
            String[] lines = healthInfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("- ") && line.contains(":")) {
                    String[] parts = line.substring(2).split(":", 2);
                    if (parts.length == 2) {
                        ocrHealthData.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }
        promptBuilder.append(buildPriorityBasedPrompt(availableIngredients, allergyTypes, ocrHealthData, new ArrayList<>()));
        promptBuilder.append("다음 레시피들의 건강 적합도를 평가해줘:\n\n");

        int currentLength = promptBuilder.length();
        for (Recipe recipe : recipes) {
            String recipeText = "레시피: " + recipe.getTitle() + "\n" +
                    "재료: " + recipe.getIngredients() + "\n\n";

            if (currentLength + recipeText.length() > maxPromptLength) {
                log.warn("프롬프트 크기 제한 초과, {} 개 레시피만 처리",
                        recipes.indexOf(recipe));
                break;
            }

            promptBuilder.append(recipeText);
            currentLength += recipeText.length();
        }

        promptBuilder.append(buildPriorityBasedSuitabilityPromptCommon().replace("=== 적합도 평가 기준 (우선순위 순) ===", "평가 기준:")).append("\n\n")
                .append("반드시 다음 JSON 형태로만 응답해줘:\n")
                .append("{\n")
                .append("  \"scores\": {\n");

        // 실제 처리된 레시피만 JSON에 포함
        int processedCount = 0;
        for (Recipe recipe : recipes) {
            if (processedCount >= Math.min(recipes.size(),
                    (maxPromptLength - currentLength) / 50)) { // 대략적인 레시피당 길이
                break;
            }
            promptBuilder.append("    \"").append(recipe.getTitle()).append("\": 점수");
            if (processedCount < Math.min(recipes.size() - 1,
                    (maxPromptLength - currentLength) / 50 - 1)) {
                promptBuilder.append(",");
            }
            promptBuilder.append("\n");
            processedCount++;
        }

        promptBuilder.append("  }\n")
                .append("}\n\n")
                .append("다른 설명이나 텍스트 없이 JSON만 응답해주세요.");

        return promptBuilder.toString();
    }

    /**
     * 건강 정보 기반 배치 응답에서 점수 파싱
     */
    private Map<String, Double> parseBatchHealthScores(String response, List<Recipe> recipes) {
        Map<String, Double> scores = new HashMap<>();

        try {
            if (ENABLE_VERBOSE_LOG) log.info("AI 응답 내용: {}", response);

            // Gemini API 응답에서 JSON 부분 추출 (코드블록 제거)
            String jsonContent = extractJsonFromAIResponse(response);
            if (ENABLE_VERBOSE_LOG) log.info("추출된 JSON: {}", jsonContent);

            // AI 응답을 JSON으로 파싱
            JsonNode scoresJson = objectMapper.readTree(jsonContent);
            JsonNode scoresNode = scoresJson.path("scores");

            if (!scoresNode.isMissingNode()) {
                log.info("점수 노드 발견: {}", scoresNode.toString());

                for (Recipe recipe : recipes) {
                    double score = scoresNode.path(recipe.getTitle()).asDouble(5.0);
                    scores.put(recipe.getTitle(), score);
                    if (ENABLE_VERBOSE_LOG) log.info("레시피 '{}' 건강 적합도 점수: {}", recipe.getTitle(), score);
                }
            } else {
                log.warn("scores 노드를 찾을 수 없습니다.");
                // 모든 레시피에 기본 점수 부여
                for (Recipe recipe : recipes) {
                    scores.put(recipe.getTitle(), 5.0);
                }
            }
        } catch (Exception e) {
            log.warn("건강 정보 기반 배치 점수 파싱 실패: {}", e.getMessage());
            // 파싱 실패 시 모든 레시피에 기본 점수 부여
            for (Recipe recipe : recipes) {
                scores.put(recipe.getTitle(), 5.0);
            }
        }

        if (ENABLE_VERBOSE_LOG) log.info("최종 건강 정보 기반 파싱 결과: {}", scores);
        return scores;
    }

    /**
     * 통합 정보 기반 배치 처리
     */
    private List<RecipeWithScore> processBatchByCombined(List<Recipe> recipes,
                                                         List<String> availableIngredients,
                                                         List<String> allergyTypes,
                                                         Map<String, String> ocrHealthData,
                                                         List<String> healthGoals) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        if (ENABLE_VERBOSE_LOG) {
            log.info("통합 정보 기반 배치 처리 시작 - 사용자 재료: {}, 알레르기: {}, 건강목표: {}",
                    String.join(", ", availableIngredients),
                    allergyTypes.isEmpty() ? "없음" : String.join(", ", allergyTypes),
                    healthGoals.isEmpty() ? "없음" : String.join(", ", healthGoals));
        }

        try {
            // 배치 처리: 모든 레시피를 한 번에 AI에게 전달
            String batchPrompt = createBatchCombinedSuitabilityPrompt(recipes, availableIngredients, allergyTypes, ocrHealthData, healthGoals);
            if (ENABLE_VERBOSE_LOG) log.info("통합 정보 기반 배치 프롬프트 생성 완료");

            String batchResponse = callAI(batchPrompt);
            if (ENABLE_VERBOSE_LOG) log.info("AI 배치 응답: {}", batchResponse);

            // AI 응답에서 각 레시피의 점수 파싱
            Map<String, Double> scores = parseBatchCombinedScores(batchResponse, recipes);
            log.info("파싱된 점수: {}", scores);

            for (Recipe recipe : recipes) {
                double score = scores.getOrDefault(recipe.getTitle(), 5.0);
                if (score > 0) {
                    recipesWithScores.add(new RecipeWithScore(recipe, score));
                    if (ENABLE_VERBOSE_LOG) log.info("레시피 '{}' 통합 적합도 점수: {}", recipe.getTitle(), score);
                } else {
                    if (ENABLE_VERBOSE_LOG) log.info("레시피 {} 제외됨 (AI가 부적합으로 판단)", recipe.getTitle());
                }
            }

            if (ENABLE_VERBOSE_LOG) log.info("통합 정보 기반 배치 처리 완료 - {} 개 레시피 처리됨", recipesWithScores.size());

        } catch (Exception e) {
            log.warn("통합 정보 기반 배치 처리 실패: {}", e.getMessage());
            // 배치 처리 실패 시 개별 처리로 전환
            recipesWithScores.addAll(processIndividualByCombined(recipes, availableIngredients, allergyTypes, ocrHealthData, healthGoals));
        }

        return recipesWithScores;
    }

    /**
     * 통합 정보 기반 개별 처리
     */
    private List<RecipeWithScore> processIndividualByCombined(List<Recipe> recipes,
                                                              List<String> availableIngredients,
                                                              List<String> allergyTypes,
                                                              Map<String, String> ocrHealthData,
                                                              List<String> healthGoals) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();

        if (ENABLE_VERBOSE_LOG) log.info("통합 정보 기반 개별 처리 시작 - {} 개 레시피", recipes.size());

        for (Recipe recipe : recipes) {
            try {
                String prompt = createCombinedSuitabilityPrompt(recipe, availableIngredients, allergyTypes, ocrHealthData, healthGoals);
                double score = callAIForSuitabilityScore(prompt);

                if (ENABLE_VERBOSE_LOG) log.info("레시피 '{}' 통합 적합도 점수: {}", recipe.getTitle(), score);

                if (score > 0) {
                    recipesWithScores.add(new RecipeWithScore(recipe, score));
                } else {
                    if (ENABLE_VERBOSE_LOG) log.info("레시피 {} 제외됨 (AI가 부적합으로 판단)", recipe.getTitle());
                }
            } catch (Exception e) {
                log.warn("레시피 {} 통합 적합도 평가 실패: {}", recipe.getTitle(), e.getMessage());
                recipesWithScores.add(new RecipeWithScore(recipe, 5.0));
            }
        }

        return recipesWithScores;
    }

    /**
     * 통합 정보 기반 배치 프롬프트 생성
     */
    private String createBatchCombinedSuitabilityPrompt(List<Recipe> recipes,
                                                        List<String> availableIngredients,
                                                        List<String> allergyTypes,
                                                        Map<String, String> ocrHealthData,
                                                        List<String> healthGoals) {
        // OpenAI 토큰 제한 고려 (대략 1토큰 = 4글자)
        int maxPromptLength = 12000; // 약 3000 토큰

        StringBuilder promptBuilder = new StringBuilder();

        // 우선순위 기반 통합 프롬프트 사용
        promptBuilder.append(buildPriorityBasedPrompt(availableIngredients, allergyTypes, ocrHealthData, healthGoals));
        promptBuilder.append("다음 레시피들의 통합 적합도를 평가해줘:\n\n");

        int currentLength = promptBuilder.length();
        for (Recipe recipe : recipes) {
            String recipeText = "레시피: " + recipe.getTitle() + "\n" +
                    "재료: " + recipe.getIngredients() + "\n\n";

            if (currentLength + recipeText.length() > maxPromptLength) {
                log.warn("프롬프트 크기 제한 초과, {} 개 레시피만 처리",
                        recipes.indexOf(recipe));
                break;
            }

            promptBuilder.append(recipeText);
            currentLength += recipeText.length();
        }

        promptBuilder.append(buildPriorityBasedSuitabilityPromptCommon().replace("=== 🎯 적합도 평가 기준 (우선순위 순) ===", "평가 기준:")).append("\n\n")
                .append("반드시 다음 JSON 형태로만 응답해줘:\n")
                .append("{\n")
                .append("  \"scores\": {\n");

        // 실제 처리된 레시피만 JSON에 포함
        int processedCount = 0;
        for (Recipe recipe : recipes) {
            if (processedCount >= Math.min(recipes.size(),
                    (maxPromptLength - currentLength) / 50)) { // 대략적인 레시피당 길이
                break;
            }
            promptBuilder.append("    \"").append(recipe.getTitle()).append("\": 점수");
            if (processedCount < Math.min(recipes.size() - 1,
                    (maxPromptLength - currentLength) / 50 - 1)) {
                promptBuilder.append(",");
            }
            promptBuilder.append("\n");
            processedCount++;
        }

        promptBuilder.append("  }\n")
                .append("}\n\n")
                .append("다른 설명이나 텍스트 없이 JSON만 응답해주세요.");

        return promptBuilder.toString();
    }

    /**
     * 통합 정보 기반 배치 응답에서 점수 파싱
     */
    private Map<String, Double> parseBatchCombinedScores(String response, List<Recipe> recipes) {
        Map<String, Double> scores = new HashMap<>();

        try {
            if (ENABLE_VERBOSE_LOG) log.info("AI 응답 내용: {}", response);

            // Gemini API 응답에서 JSON 부분 추출 (코드블록 제거)
            String jsonContent = extractJsonFromAIResponse(response);
            if (ENABLE_VERBOSE_LOG) log.info("추출된 JSON: {}", jsonContent);

            // AI 응답을 JSON으로 파싱
            JsonNode scoresJson = objectMapper.readTree(jsonContent);
            JsonNode scoresNode = scoresJson.path("scores");

            if (!scoresNode.isMissingNode()) {
                if (ENABLE_VERBOSE_LOG) log.info("점수 노드 발견: {}", scoresNode.toString());

                for (Recipe recipe : recipes) {
                    double score = scoresNode.path(recipe.getTitle()).asDouble(5.0);
                    scores.put(recipe.getTitle(), score);
                    if (ENABLE_VERBOSE_LOG) log.info("레시피 '{}' 통합 적합도 점수: {}", recipe.getTitle(), score);
                }
            } else {
                log.warn("scores 노드를 찾을 수 없습니다.");
                // 모든 레시피에 기본 점수 부여
                for (Recipe recipe : recipes) {
                    scores.put(recipe.getTitle(), 5.0);
                }
            }
        } catch (Exception e) {
            log.warn("통합 정보 기반 배치 점수 파싱 실패: {}", e.getMessage());
            // 파싱 실패 시 모든 레시피에 기본 점수 부여
            for (Recipe recipe : recipes) {
                scores.put(recipe.getTitle(), 5.0);
            }
        }

        if (ENABLE_VERBOSE_LOG) log.info("최종 통합 정보 기반 파싱 결과: {}", scores);
        return scores;
    }

    /**
     * HealthGoal 기반 배치 프롬프트 생성
     */
    private String createBatchHealthGoalSuitabilityPrompt(List<Recipe> recipes,
                                                          List<String> availableIngredients,
                                                          List<String> allergyTypes,
                                                          List<String> healthGoals) {
        // OpenAI 토큰 제한 고려 (대략 1토큰 = 4글자)
        int maxPromptLength = 12000; // 약 3000 토큰

        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("사용자가 가지고 있는 재료: ").append(String.join(", ", availableIngredients)).append("\n")
                .append(buildAllergyPrompt(allergyTypes))
                .append("사용자 건강 목표: ").append(String.join(", ", healthGoals)).append("\n")
                .append("다음 레시피들의 건강 목표 적합도를 평가해줘:\n\n");

        int currentLength = promptBuilder.length();
        for (Recipe recipe : recipes) {
            String recipeText = "레시피: " + recipe.getTitle() + "\n" +
                    "재료: " + recipe.getIngredients() + "\n\n";

            if (currentLength + recipeText.length() > maxPromptLength) {
                log.warn("프롬프트 크기 제한 초과, {} 개 레시피만 처리",
                        recipes.indexOf(recipe));
                break;
            }

            promptBuilder.append(recipeText);
            currentLength += recipeText.length();
        }

        promptBuilder.append(buildHealthGoalSuitabilityPromptCommon().replace("위 정보를 바탕으로", "평가 기준:")).append("\n\n")
                .append("반드시 다음 JSON 형태로만 응답해줘:\n")
                .append("{\n")
                .append("  \"scores\": {\n");

        // 실제 처리된 레시피만 JSON에 포함
        int processedCount = 0;
        for (Recipe recipe : recipes) {
            if (processedCount >= Math.min(recipes.size(),
                    (maxPromptLength - currentLength) / 50)) { // 대략적인 레시피당 길이
                break;
            }
            promptBuilder.append("    \"").append(recipe.getTitle()).append("\": 점수");
            if (processedCount < Math.min(recipes.size() - 1,
                    (maxPromptLength - currentLength) / 50 - 1)) {
                promptBuilder.append(",");
            }
            promptBuilder.append("\n");
            processedCount++;
        }

        promptBuilder.append("  }\n")
                .append("}\n\n")
                .append("다른 설명이나 텍스트 없이 JSON만 응답해주세요.");

        return promptBuilder.toString();
    }

    /**
     * HealthGoal 기반 배치 응답에서 점수 파싱
     */
    private Map<String, Double> parseBatchHealthGoalScores(String response, List<Recipe> recipes) {
        Map<String, Double> scores = new HashMap<>();

        try {
            if (ENABLE_VERBOSE_LOG) log.info("AI 응답 내용: {}", response);

            // Gemini API 응답에서 JSON 부분 추출 (코드블록 제거)
            String jsonContent = extractJsonFromAIResponse(response);
            if (ENABLE_VERBOSE_LOG) log.info("추출된 JSON: {}", jsonContent);

            // AI 응답을 JSON으로 파싱
            JsonNode scoresJson = objectMapper.readTree(jsonContent);
            JsonNode scoresNode = scoresJson.path("scores");

            if (!scoresNode.isMissingNode()) {
                if (ENABLE_VERBOSE_LOG) log.info("점수 노드 발견: {}", scoresNode.toString());

                for (Recipe recipe : recipes) {
                    double score = scoresNode.path(recipe.getTitle()).asDouble(5.0);
                    scores.put(recipe.getTitle(), score);
                    if (ENABLE_VERBOSE_LOG) log.info("레시피 '{}' 건강 목표 적합도 점수: {}", recipe.getTitle(), score);
                }
            } else {
                log.warn("scores 노드를 찾을 수 없습니다.");
                // 모든 레시피에 기본 점수 부여
                for (Recipe recipe : recipes) {
                    scores.put(recipe.getTitle(), 5.0);
                }
            }
        } catch (Exception e) {
            log.warn("HealthGoal 기반 배치 점수 파싱 실패: {}", e.getMessage());
            // 파싱 실패 시 모든 레시피에 기본 점수 부여
            for (Recipe recipe : recipes) {
                scores.put(recipe.getTitle(), 5.0);
            }
        }

        if (ENABLE_VERBOSE_LOG) log.info("최종 HealthGoal 기반 파싱 결과: {}", scores);
        return scores;
    }

    private List<Recipe> getRecipesByCategories(String categories) {
        try {
            String[] categoryArray = categories.split(",");
            List<RecipeCategory> recipeCategories = new ArrayList<>();

            for (String category : categoryArray) {
                try {
                    RecipeCategory recipeCategory = RecipeCategory.valueOf(category.trim().toUpperCase());
                    recipeCategories.add(recipeCategory);
                } catch (IllegalArgumentException e) {
                    log.warn("유효하지 않은 카테고리: {}", category.trim());
                }
            }

            if (recipeCategories.isEmpty()) {
                log.warn("유효한 카테고리가 없습니다: {}", categories);
                return new ArrayList<>();
            }

            List<Recipe> recipes = recipeRepository.findByCategoriesIn(recipeCategories);
            return recipes.stream()
                    .limit(MAX_RECOMMENDATIONS)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("카테고리 파싱 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public List<RecipeResponse.DetailRes> createAndSaveRandomRecipes(Long userId, String topic) {
        log.info("AI 랜덤 레시피 생성 및 저장 시작 - userId: {}, topic: {}", userId, topic);

        try {
            // 사용자 정보 검증
            User user = getUser(userId);
            log.info("사용자 검증 완료: userId={}, user={}", userId, user != null ? user.getId() : "null");

            // 주제 기반 또는 완전 랜덤 프롬프트 생성
            String prompt = buildRecipePostPromptRandom(topic);
            log.info("랜덤 레시피 생성 프롬프트 생성 완료 - 주제: {}, 프롬프트 길이: {}", topic, prompt.length());

            // AI 호출하여 레시피 생성 및 저장
            List<Recipe> recipes = callAIAndSaveRecipes(prompt);
            log.info("AI 랜덤 레시피 생성 완료 - 생성된 레시피 수: {}, 주제: {}", recipes.size(), topic);

            // DetailRes로 변환하여 반환
            return recipes.stream()
                    .map(RecipeConverter::toDetailRes)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("AI 랜덤 레시피 생성 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
        }
    }

    /**
     * AI를 사용하여 랜덤 레시피를 생성하고 저장합니다. (기존 호환성 유지)
     * 주제 없이 완전 랜덤하게 레시피를 생성합니다.
     */
    public List<RecipeResponse.DetailRes> createAndSaveRandomRecipes(Long userId) {
        return createAndSaveRandomRecipes(userId, null);
    }

    /**
     * AI를 사용하여 키워드 기반 랜덤 레시피를 생성하고 저장합니다.
     * 키워드가 제공되면 해당 키워드와 연관된 레시피를 생성하고, 없으면 완전 랜덤하게 생성합니다.
     */
    @Override
    @Transactional
    public List<RecipeResponse.DetailRes> createAndSaveRandomRecipesByKeyword(Long userId, String keyword) {
        log.info("AI 키워드 기반 랜덤 레시피 생성 및 저장 시작 - userId: {}, keyword: {}", userId, keyword);

        try {
            // 키워드 기반 또는 완전 랜덤 프롬프트 생성
            String prompt = buildRecipeKeywordPrompt(keyword);
            log.info("키워드 기반 랜덤 레시피 생성 프롬프트 생성 완료 - 키워드: {}", keyword);

            // AI 호출하여 레시피 생성 및 저장
            List<Recipe> recipes = callAIAndSaveRecipes(prompt);
            log.info("AI 키워드 기반 랜덤 레시피 생성 완료 - 생성된 레시피 수: {}, 키워드: {}", recipes.size(), keyword);

            // DetailRes로 변환하여 반환
            return recipes.stream()
                    .map(RecipeConverter::toDetailRes)
                    .collect(Collectors.toList());
        } catch (BusinessException e) {
            log.error("AI 키워드 기반 랜덤 레시피 생성 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("AI 키워드 기반 랜덤 레시피 생성 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public List<RecipeResponse.DetailRes> createAndSaveRecipesByIngredient(Long userId) {
        log.info("AI 재료 기반 레시피 생성 및 저장 시작 - userId: {}", userId);

        try {
            // 사용자 정보 조회
            User user = getUser(userId);
            List<String> availableIngredients = getUserIngredients(userId);
            List<String> allergyTypes = getUserAllergies(userId);

            // 재료 기반 프롬프트 생성
            String prompt = buildRecipePostPromptIngredient(availableIngredients);
            log.info("재료 기반 레시피 생성 프롬프트 생성 완료");

            // AI 호출하여 레시피 생성 및 저장
            List<Recipe> recipes = callAIAndSaveRecipes(prompt);
            log.info("AI 재료 기반 레시피 생성 완료 - 생성된 레시피 수: {}", recipes.size());

            // DetailRes로 변환하여 반환
            return recipes.stream()
                    .map(RecipeConverter::toDetailRes)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("AI 재료 기반 레시피 생성 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
        }
    }

    /**
     * 레시피 제목을 Redis ZSet에 추가 (search domain과 동일한 방식)
     * ZSet은 TTL을 직접 지원하지 않으므로 별도 TTL 설정 필요
     */
    private void cacheRecipeTitle(String title) {
        try {
            // ZSet에 제목 추가 (score는 시간 기반으로 부여하면 정리에 유리합니다)
            redisTemplate.opsForZSet().add(RECIPE_TITLES_KEY, title, System.currentTimeMillis());
            log.info("Redis ZSet에 레시피 제목 추가: {}", title);
        } catch (Exception e) {
            log.warn("Redis ZSet 레시피 제목 추가 실패: {}", e.getMessage());
        }
    }

    /**
     * Redis ZSet에서 오래된 레시피 제목들을 정리
     * 매일 새벽 2시에 실행 (search domain과 동일한 패턴)
     * Score가 시간 기반이므로 시간 순으로 정리 가능
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredRecipeTitles() {
        try {
            long now = System.currentTimeMillis();
            long cutoff = now - RECIPE_CACHE_TTL.toMillis(); // 30일 이전 데이터 제거
            Long removed = redisTemplate.opsForZSet()
                    .removeRangeByScore(RECIPE_TITLES_KEY, 0, cutoff);
            log.info("Redis ZSet 만료 정리 완료 - 제거:{}개, cutoff:{}", removed, cutoff);
        } catch (Exception e) {
            log.error("Redis ZSet 정리 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 우선순위 기반 통합 프롬프트 생성 (재사용성 고려)
     * 우선순위: 알러지(최우선, 강제 배제) > 건강 목표 > 건강 정보(OCR) > 재료
     */
    private String buildPriorityBasedPrompt(List<String> availableIngredients,
                                            List<String> allergyTypes,
                                            Map<String, String> ocrHealthData,
                                            List<String> healthGoals) {
        StringBuilder promptBuilder = new StringBuilder();

        // 1. 알러지 정보 (최우선, 강제 배제)
        promptBuilder.append("===최우선 고려사항 알러지 정보보 ===\n");
        promptBuilder.append(buildAllergyPrompt(allergyTypes));
        promptBuilder.append("위 알러지 성분이 포함된 요리는 절대 추천하지 마세요. (0점 처리)\n\n");

        // 2. 건강 목표 (두 번째 우선순위)
        if (healthGoals != null && !healthGoals.isEmpty()) {
            promptBuilder.append("=== 건강 목표 정보 ===\n");
            promptBuilder.append("사용자 건강 목표: ").append(String.join(", ", healthGoals)).append("\n");
            promptBuilder.append("건강 목표 달성에 도움이 되는 요리를 우선적으로 고려합니다.\n\n");
        }

        // 3. 건강 정보 (OCR, 세 번째 우선순위)
        if (ocrHealthData != null && !ocrHealthData.isEmpty()) {
            promptBuilder.append("=== 현재 건강 상태 정보 ===\n");
            promptBuilder.append(buildOcrHealthInfo(ocrHealthData));
            promptBuilder.append("현재 건강 상태에 적합한 요리를 고려합니다.\n\n");
        }

        // 4. 재료 정보 (마지막 우선순위)
        if (availableIngredients != null && !availableIngredients.isEmpty()) {
            promptBuilder.append("=== 보유 재료 정보 ===\n");
            List<String> uniqueIngredients = new ArrayList<>(new LinkedHashSet<>(availableIngredients));
            promptBuilder.append("사용자 보유 재료: ").append(String.join(", ", uniqueIngredients)).append("\n");
            promptBuilder.append("보유 재료를 활용할 수 있는 요리를 우선적으로 고려합니다.\n\n");
        }

        return promptBuilder.toString();
    }

    /**
     * 우선순위 기반 적합도 평가 공통 프롬프트 생성
     */
    private String buildPriorityBasedSuitabilityPromptCommon() {
        return "=== 적합도 평가 기준 (우선순위 순) ===\n" +
                "1. 알러지 성분 포함 여부 (최우선, 포함시 0점)\n" +
                "2. 건강 목표 달성 도움 정도\n" +
                "3. 현재 건강 상태 적합성\n" +
                "4. 보유 재료 활용도\n\n" +
                "점수 기준:\n" +
                "- 9-10점: 모든 조건을 완벽하게 만족 (건강 목표 최적, 건강 상태 적합, 재료 완벽)\n" +
                "- 7-8점: 대부분의 조건을 만족 (건강 목표 적합, 건강 상태 적합, 재료 충분)\n" +
                "- 5-6점: 주요 조건을 만족 (건강 목표 보통, 건강 상태 보통, 재료 가능)\n" +
                "- 3-4점: 일부 조건만 만족 (건강 목표 부적합, 건강 상태 부적합, 재료 부족)\n" +
                "- 1-2점: 대부분의 조건을 만족하지 못함\n" +
                "- 0점: 알러지 성분 포함 (절대 추천 불가)\n\n" +
                "적합도 점수만 숫자로 응답해주세요 (예: 8.5)";
    }

    /**
     * HTTP/HTTPS URL 유효성 검증
     *
     * @param url 검증할 URL 문자열
     * @return 유효한 HTTP/HTTPS URL이면 true, 그렇지 않으면 false
     */
    private boolean isValidHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            java.net.URI uri = new java.net.URI(url.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }
}

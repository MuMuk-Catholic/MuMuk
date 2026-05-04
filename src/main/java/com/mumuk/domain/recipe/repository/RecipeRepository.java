package com.mumuk.domain.recipe.repository;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.entity.RecipeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // 여러 카테고리 중 하나라도 포함된 레시피의 이름 반환
    @Query("SELECT DISTINCT r.title FROM Recipe r JOIN r.categories c WHERE c IN :categories")
    List<String> findNamesByCategories(@Param("categories") List<RecipeCategory> categories);

    // 여러 카테고리 중 하나라도 포함된 레시피 반환
    @Query("SELECT DISTINCT r FROM Recipe r JOIN r.categories c WHERE c IN :categories")
    List<Recipe> findByCategoriesIn(@Param("categories") List<RecipeCategory> categories);

    // 입력한 레시피 id를 제외하고, 같은 카테고리 중 하나라도 포함된 레시피 6개를 랜덤하게 조회

    @Query(
            value = """
                SELECT * FROM recipe
                WHERE id IN (
                    SELECT DISTINCT r.id FROM recipe r
                    INNER JOIN recipe_category_map rcm ON r.id = rcm.recipe_id
                    WHERE rcm.category IN (:categories) AND r.id != :recipeId
                )
                ORDER BY RANDOM()
                LIMIT 6
                """,
            nativeQuery = true
    )
    List<Recipe> findRandomRecipesByCategories(@Param("categories") List<String> categories, @Param("recipeId") Long recipeId);

    // 칼로리 범위로 검색
    List<Recipe> findByCaloriesLessThanEqual(Long maxCalories);

    // 조리시간 범위로 검색
    List<Recipe> findByCookingTimeLessThanEqual(Long maxCookingTime);

    // 복합 조건 검색
    @Query("SELECT r FROM Recipe r WHERE " +
            "(:keyword IS NULL OR r.title LIKE %:keyword%) AND " +
            "(:categories IS NULL OR EXISTS (SELECT 1 FROM r.categories c WHERE c IN :categories)) AND " +
            "(:maxCalories IS NULL OR r.calories <= :maxCalories) AND " +
            "(:maxCookingTime IS NULL OR r.cookingTime <= :maxCookingTime)")
    List<Recipe> searchRecipes(@Param("keyword") String keyword,
                               @Param("categories") List<RecipeCategory> categories,
                               @Param("maxCalories") Long maxCalories,
                               @Param("maxCookingTime") Long maxCookingTime);

    // 중복 레시피 검증
    boolean existsByTitleAndIngredients(String title, String ingredients);

    // 제목만으로 중복 체크
    boolean existsByTitle(String title);

    // 제목과 재료로 중복 체크
    @Query("SELECT r FROM Recipe r WHERE r.title = :title AND r.ingredients = :ingredients")
    List<Recipe> findByTitleAndIngredients(@Param("title") String title, @Param("ingredients") String ingredients);

    // 제목으로 검색 (대소문자 무시)
    @Query("SELECT r FROM Recipe r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Recipe> findByTitleContainingIgnoreCase(@Param("title") String title);

    // 효율적인 랜덤 샘플링 (DB 레벨에서 한 번의 쿼리로 처리)
    @Query(value = "SELECT * FROM recipe ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Recipe> findRandomRecipes(@Param("limit") int limit);

    // PK 범위를 이용한 랜덤 샘플링 (대용량 테이블용 대안)
    @Query(value = """
        SELECT * FROM recipe 
        WHERE id >= (
            SELECT CAST(FLOOR(RANDOM() * (SELECT MAX(id) FROM recipe)) + 1 AS BIGINT)
        )
        ORDER BY id 
        LIMIT :limit
        """, nativeQuery = true)
    List<Recipe> findRandomRecipesByPkRange(@Param("limit") int limit);

    // 카테고리 조건을 만족하는 레시피를 랜덤으로 제한 개수만큼 조회
    @Query(value = """
        SELECT r.*
        FROM recipe r
        INNER JOIN recipe_category_map rcm ON r.id = rcm.recipe_id
        WHERE rcm.category IN (:categories)
        GROUP BY r.id
        ORDER BY RANDOM()
        LIMIT :limit
        """, nativeQuery = true)
    List<Recipe> findRandomRecipesByCategoriesLimited(@Param("categories") List<String> categories,
                                                      @Param("limit") int limit);
}

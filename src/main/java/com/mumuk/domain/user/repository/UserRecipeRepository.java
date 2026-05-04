package com.mumuk.domain.user.repository;

import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.user.entity.UserRecipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRecipeRepository extends JpaRepository<UserRecipe, Long> {
    Optional<UserRecipe> findByUserIdAndRecipeId(Long userId, Long recipeId);
    @EntityGraph(attributePaths = {"recipe"})
    List<UserRecipe> findByUserIdAndRecipeIdIn(Long userId, List<Long> recipeIds);

    //사용자가 찜한 레시피 목록을 조회
    Page<UserRecipe> findByUser_IdAndLikedIsTrue(Long userId, Pageable pageable);
    //사용자가 최근에 본 레시피 목록을 조회
    //List<UserRecipe> findByUser_IdAndViewedIsTrueOrderByViewedAtDesc(Long userId, Pageable pageable);

}

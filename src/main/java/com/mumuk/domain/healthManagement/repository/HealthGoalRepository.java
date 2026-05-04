package com.mumuk.domain.healthManagement.repository;

import com.mumuk.domain.healthManagement.entity.HealthGoal;
import com.mumuk.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthGoalRepository extends JpaRepository<HealthGoal, Long> {
    List<HealthGoal> findByUser(User user);

    void deleteByUser(User user);
}

package com.mumuk.domain.ocr.repository;

import com.mumuk.domain.ocr.entity.UserHealthData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserHealthDataRepository extends JpaRepository<UserHealthData, Long> {

    /**
     * 사용자 ID로 최신 OCR 데이터 조회 (생성일 기준 내림차순)
     */
    @Query("SELECT uhd FROM UserHealthData uhd WHERE uhd.userId = :userId ORDER BY uhd.createdAt DESC")
    List<UserHealthData> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}

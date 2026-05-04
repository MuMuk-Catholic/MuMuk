package com.mumuk.domain.recipe.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecommendRequest {
    // 기본 신체 정보
    private String gender; // 성별
    private Double height; // 키 (cm)
    private Double weight; // 체중 (kg)

    // InBody 체성분 데이터
    private Double skeletalMuscle; // 골격근량 (kg)
    private Double bodyFatMass; // 체지방량 (kg)
    private Double bodyFatPercentage; // 체지방률 (%)
    private Double bmi; // BMI
    private Double bmr; // 기초대사량 (kcal)

    // 알레르기 정보
    private List<String> allergies; // 알레르기 목록

    // 건강 목표
    private List<String> healthGoals; // 건강 목표 (체중감량, 근육량 증가, 당 줄이기, 혈압관리, 콜레스테롤 관리, 소화 및 장 건강 등)
}
package com.mumuk.domain.recipe.entity;

public enum RecipeCategory {
    BODY_WEIGHT_MANAGEMENT("체형/체중 관리식단"),
    HEALTH_MANAGEMENT("건강 관리식단"),
    WEIGHT_LOSS("체중 감량"),
    MUSCLE_GAIN("근육 증가"),
    SUGAR_REDUCTION("당 줄이기"),
    BLOOD_PRESSURE("혈압관리"),
    CHOLESTEROL("콜레스테롤 관리"),
    DIGESTION("소화 건강"),
    OTHER("기타");

    private final String name;

    RecipeCategory(String name) {
        this.name = name;
    }

    public String getName() { return name; }
}
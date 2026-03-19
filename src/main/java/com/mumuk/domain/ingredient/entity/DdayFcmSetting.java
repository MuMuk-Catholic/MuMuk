package com.mumuk.domain.ingredient.entity;

public enum DdayFcmSetting {
    NONE(0),
    D3(3),
    D7(7),
    D10(10),
    D31(31);

    private final int daysBefore;

    DdayFcmSetting(int daysBefore) {
        this.daysBefore = daysBefore;
    }

    public int getDaysBefore() {
        return daysBefore;
    }
}
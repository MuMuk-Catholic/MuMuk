package com.mumuk.domain.ingredient.entity;

import com.mumuk.domain.user.entity.User;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "ingredient_notification")
public class IngredientNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "재료별 알림 설정", nullable = false)
    @Enumerated(EnumType.STRING)
    private DdayFcmSetting ddayFcmSetting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    public IngredientNotification() {

    }

    public IngredientNotification(Ingredient ingredient, DdayFcmSetting setting) {
        this.ingredient = ingredient;
        this.ddayFcmSetting = setting;
    }

    // Getter
    public Long getId() {return id;}

    public DdayFcmSetting getDdayFcmSetting() {return ddayFcmSetting;}

    public Ingredient getIngredient() {return ingredient;}

    // Setter
    public void setId(Long id) {
        this.id = id;}

    public void setDdayFcmSetting(DdayFcmSetting ddayFcmSetting) {
        this.ddayFcmSetting = ddayFcmSetting;}

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;}

}

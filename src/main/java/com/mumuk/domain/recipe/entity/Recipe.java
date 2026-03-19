package com.mumuk.domain.recipe.entity;

import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipe", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"title"})
})
public class Recipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "recipe_image", nullable = false, length = 500)
    private String recipeImage = "default-recipe-image.jpg";

    @Column(name = "description", nullable = false, length = 100)
    private String description;

    @Column(name = "cooking_time", nullable = false)
    private Long cookingTime;

    @Column(name = "protein", nullable = false)
    private Long protein;

    @Column(name = "carbohydrate", nullable = false)
    private Long carbohydrate;

    @Column(name = "fat", nullable = false)
    private Long fat;

    @Column(name = "calories", nullable = false)
    private Long calories;

    @ElementCollection(targetClass = RecipeCategory.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "recipe_category_map", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "category")
    private List<RecipeCategory> categories = new ArrayList<>();

    @Column(name = "ingredients", nullable = false, length = 200)
    private String ingredients;

    // Getter
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getRecipeImage() {
        return recipeImage;
    }

    public String getDescription() {
        return description;
    }

    public Long getCookingTime() {
        return cookingTime;
    }

    public Long getCalories() {
        return calories;
    }

    public Long getProtein() {
        return protein;
    }

    public Long getCarbohydrate() {
        return carbohydrate;
    }

    public Long getFat() {
        return fat;
    }

    public List<RecipeCategory> getCategories() {
        return categories;
    }

    public String getIngredients() {
        return ingredients;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setRecipeImage(String recipeImage) {
        this.recipeImage = recipeImage;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCookingTime(Long cookingTime) {
        this.cookingTime = cookingTime;
    }

    public void setCalories(Long calories) {
        this.calories = calories;
    }

    public void setProtein(Long protein) {
        this.protein = protein;
    }

    public void setCarbohydrate(Long carbohydrate) {
        this.carbohydrate = carbohydrate;
    }

    public void setFat(Long fat) {
        this.fat = fat;
    }

    public void setCategories(List<RecipeCategory> categories) {
        this.categories = categories;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }
}

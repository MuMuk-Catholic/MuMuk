package com.mumuk.domain.ingredient.service;

import com.mumuk.domain.ingredient.dto.request.IngredientRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;

import java.util.List;


public interface IngredientService {
    void registerIngredient(IngredientRequest.RegisterReq req, Long userId);
    List<IngredientResponse.RetrieveRes> getAllIngredient(Long userId);
    void updateIngredientExpireDate(Long ingredientId, IngredientRequest.UpdateExpireDateReq req, Long userId);
    void updateIngredientDdaySetting(Long ingredientId, IngredientRequest.UpdateDdaySettingReq req, Long userId);
    void updateIngredientQuantity(Long ingredientId, IngredientRequest.UpdateQuantityReq req, Long userId);
    void deleteIngredient(Long ingredientId, Long userId);
    List<IngredientResponse.ExpireDateManegeRes> getCloseExpireDateIngredients(Long userId);

}

package com.mumuk.domain.ingredient.converter;

import com.mumuk.domain.ingredient.dto.request.IngredientRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;
import com.mumuk.domain.ingredient.entity.DdayFcmSetting;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.ingredient.entity.IngredientNotification;
import com.mumuk.domain.user.entity.User;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class IngredientConverter {

    public Ingredient toRegister(IngredientRequest.RegisterReq req, User user) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(req.getName());
        ingredient.setExpireDate(req.getExpireDate());
        ingredient.setUser(user);
        ingredient.setQuantity(1); //재료 개수 기본값 1
        ingredient.setDaySettings(
                List.of(new IngredientNotification(ingredient, DdayFcmSetting.NONE))
        );//기본값 NONE 리스트 설정.업데이트시 새로운 리스트로 Set 하므로 불변리스트로 코드 간략화
        return ingredient;

    }

    public IngredientResponse.RetrieveRes toRetrieve(Ingredient ingredient) {
        return new IngredientResponse.RetrieveRes(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getExpireDate(),
                ingredient.getCreatedAt(),
                ingredient.getQuantity());

    }

    public IngredientResponse.ExpireDateManegeRes toExpireDate(Ingredient ingredient) {
        LocalDate today = LocalDate.now();
        long daysLeft = ChronoUnit.DAYS.between(today, ingredient.getExpireDate());
        String dDay = "D-" + daysLeft;
        return new IngredientResponse.ExpireDateManegeRes(
                ingredient.getName(),
                ingredient.getExpireDate(),
                dDay);

    }
}

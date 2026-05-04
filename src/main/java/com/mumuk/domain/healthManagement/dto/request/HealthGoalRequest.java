package com.mumuk.domain.healthManagement.dto.request;

import com.mumuk.domain.healthManagement.entity.HealthGoalType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class HealthGoalRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SetHealthGoalReq {
        private List<HealthGoalType> healthGoalTypeList;
    }
}

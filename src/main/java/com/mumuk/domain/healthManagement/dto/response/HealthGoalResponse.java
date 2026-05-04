package com.mumuk.domain.healthManagement.dto.response;

import com.mumuk.domain.healthManagement.entity.HealthGoalType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public class HealthGoalResponse {

    @Getter
    @AllArgsConstructor
    public static class HealthGoalListRes{

        private List<HealthGoalRes> healthGoalList;
        @Getter
        @AllArgsConstructor
        public static class HealthGoalRes{
            private HealthGoalType healthGoalType;
        }
    }
}

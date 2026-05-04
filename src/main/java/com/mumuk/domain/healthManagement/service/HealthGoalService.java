package com.mumuk.domain.healthManagement.service;

import com.mumuk.domain.healthManagement.dto.request.HealthGoalRequest;
import com.mumuk.domain.healthManagement.dto.response.HealthGoalResponse;

public interface HealthGoalService {
    HealthGoalResponse.HealthGoalListRes getHealthGoalList(Long userId);
    HealthGoalResponse.HealthGoalListRes setHealthGoalList(Long userId, HealthGoalRequest.SetHealthGoalReq request);
}
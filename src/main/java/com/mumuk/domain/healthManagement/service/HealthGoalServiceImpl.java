package com.mumuk.domain.healthManagement.service;

import com.mumuk.domain.healthManagement.dto.request.HealthGoalRequest;
import com.mumuk.domain.healthManagement.dto.response.HealthGoalResponse;
import com.mumuk.domain.healthManagement.entity.HealthGoal;
import com.mumuk.domain.healthManagement.entity.HealthGoalType;
import com.mumuk.domain.healthManagement.repository.HealthGoalRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.security.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HealthGoalServiceImpl implements HealthGoalService {
    private final HealthGoalRepository healthGoalRepository;
    private final UserRepository userRepository;

    public HealthGoalServiceImpl(HealthGoalRepository healthGoalRepository, UserRepository userRepository) {
        this.healthGoalRepository = healthGoalRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public HealthGoalResponse.HealthGoalListRes getHealthGoalList(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        List<HealthGoal> healthGoalList = healthGoalRepository.findByUser(user);

        List<HealthGoalResponse.HealthGoalListRes.HealthGoalRes> healthGoalResList = healthGoalList.stream()
                .map( healthGoal -> new HealthGoalResponse.HealthGoalListRes.HealthGoalRes(
                        healthGoal.getGoalName()
                )).toList();

        return new HealthGoalResponse.HealthGoalListRes(healthGoalResList);
    }

    @Override
    @Transactional
    public HealthGoalResponse.HealthGoalListRes setHealthGoalList(Long userId, HealthGoalRequest.SetHealthGoalReq request) {
        User user= userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        List<HealthGoalType> healthGoalTypeList = request.getHealthGoalTypeList();

        //건강목표 없음과 다른 목표 동시에 선택 불가
        if (healthGoalTypeList.contains(HealthGoalType.NONE) && healthGoalTypeList.size()>1) {
            throw new BusinessException(ErrorCode.HEALTHGOAL_NONE_WITH_OTHERS);
        }

        // 건강목표 정보 삭제
        healthGoalRepository.deleteByUser(user);
        healthGoalRepository.flush();

        // 건강목표 객체 생성
        List<HealthGoal> healthGoalList = healthGoalTypeList.stream()
                .map(healthGoalType -> new HealthGoal(healthGoalType,user))
                .toList();

        // 건강목표 객체 저장
        healthGoalRepository.saveAll(healthGoalList);
        healthGoalRepository.flush();

        // 반환할 값 생성
        List<HealthGoalResponse.HealthGoalListRes.HealthGoalRes> setHealthGoalList=healthGoalTypeList.stream()
                .map(healthGoalType -> new HealthGoalResponse.HealthGoalListRes.HealthGoalRes(healthGoalType))
                .toList();
        return new HealthGoalResponse.HealthGoalListRes(setHealthGoalList);

    }
}

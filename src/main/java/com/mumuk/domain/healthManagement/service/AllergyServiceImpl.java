package com.mumuk.domain.healthManagement.service;

import com.mumuk.domain.healthManagement.dto.request.AllergyRequest;
import com.mumuk.domain.healthManagement.dto.response.AllergyResponse;
import com.mumuk.domain.healthManagement.entity.Allergy;
import com.mumuk.domain.healthManagement.entity.AllergyType;
import com.mumuk.domain.healthManagement.repository.AllergyRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.security.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class AllergyServiceImpl implements AllergyService {

    private final UserRepository userRepository;
    private final AllergyRepository allergyRepository;

    public AllergyServiceImpl(UserRepository userRepository, AllergyRepository allergyRepository) {
        this.userRepository = userRepository;
        this.allergyRepository = allergyRepository;
    }

    @Override
    @Transactional
    public AllergyResponse.AllergyListRes getAllergyList(Long userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(()->new AuthException(ErrorCode.USER_NOT_FOUND));

        List<Allergy> allergyList=allergyRepository.findByUser(user);

        List<AllergyResponse.AllergyListRes.AllergyTypeRes> allergyListRes=allergyList.stream()
                .map(allergy->new AllergyResponse.AllergyListRes.AllergyTypeRes(
                        allergy.getAllergyType()
                )).toList();
        return new AllergyResponse.AllergyListRes(allergyListRes);

    }

    @Override
    @Transactional
    public AllergyResponse.AllergyListRes setAllergyList(Long userId, AllergyRequest.SetAllergyReq request) {

        User user=userRepository.findById(userId)
                .orElseThrow(()->new AuthException(ErrorCode.USER_NOT_FOUND));

        List<AllergyType> allergyTypeList =request.getAllergyTypeList();

        if (allergyTypeList.contains(AllergyType.NONE) && allergyTypeList.size()>1){
            throw new BusinessException(ErrorCode.ALLERGY_NONE_WITH_OTHERS);
        }

        // 사용자의 기존 알러지 정보 삭제
        allergyRepository.deleteByUser(user);
        allergyRepository.flush();

        // 입력받은 알러지 정보 목록을 모두 알러지 객체 목록으로 전환
        List<Allergy> allergyList=allergyTypeList.stream()
                .map(allergyType -> new Allergy(allergyType,user))
                .toList();

        // 알러지 정보 저장
        allergyRepository.saveAll(allergyList);
        allergyRepository.flush();

        List<AllergyResponse.AllergyListRes.AllergyTypeRes> setAllergyList=allergyTypeList.stream()
                .map(allergyType -> new AllergyResponse.AllergyListRes.AllergyTypeRes(allergyType))
                .toList();
        return new AllergyResponse.AllergyListRes(setAllergyList);
    }
}
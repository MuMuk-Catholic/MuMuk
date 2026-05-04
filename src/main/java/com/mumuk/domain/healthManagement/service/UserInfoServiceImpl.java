package com.mumuk.domain.healthManagement.service;

import com.mumuk.domain.healthManagement.dto.request.UserInfoRequest;
import com.mumuk.domain.healthManagement.dto.response.UserInfoResponse;
import com.mumuk.domain.healthManagement.entity.Gender;
import com.mumuk.domain.healthManagement.entity.UserInfo;
import com.mumuk.domain.healthManagement.repository.UserInfoRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;

    public UserInfoServiceImpl (UserRepository userRepository, UserInfoRepository userInfoRepository){
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
    }
    @Override
    @Transactional
    public UserInfoResponse.UserInfoRes setUserInfo(Long userId, UserInfoRequest.UserInfoReq request) {
        User user=userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        //userInfo가 존재하는지 확인하고 수정하기
        UserInfo userInfo =userInfoRepository.findByUserId(userId)
                .orElseGet(()->{
                    UserInfo newUserinfo =new UserInfo();
                    newUserinfo.setUser(user);
                    return newUserinfo;
                });

        // 예외처리를 위해 둔 코드임. 만약 userInfo의 값이 null이라면, 성별의 기본값인 NONE으로 변경함
        if (userInfo.getGender()==null) {
            userInfo.setGender(Gender.NONE);
        }

        // 사용자 요청에서 gender 입력이 없었다면, gender의 기본값인 NONE 유지, 아닐 경우 요청에 따라 성별 변경
        if (request.getGender()!=null) {
            userInfo.setGender(request.getGender());
        }
        // 사용자 요청에 따라 userInfo 값 변경
        userInfo.setHeight(request.getHeight());
        userInfo.setWeight(request.getWeight());

        userInfoRepository.save(userInfo);

        UserInfoResponse.UserInfoRes userInfoRes =new UserInfoResponse.UserInfoRes(userInfo.getGender(),userInfo.getHeight(),userInfo.getWeight());
        return userInfoRes;
    }

    @Override
    @Transactional
    public UserInfoResponse.UserInfoRes getUserInfo(Long userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserInfo userInfo=userInfoRepository.findByUserId(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USERINFO_NOT_FOUND));

        return new UserInfoResponse.UserInfoRes(userInfo.getGender(), userInfo.getHeight(), userInfo.getWeight());
    }
}

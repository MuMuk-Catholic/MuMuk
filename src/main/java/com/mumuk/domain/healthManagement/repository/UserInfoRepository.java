package com.mumuk.domain.healthManagement.repository;

import com.mumuk.domain.healthManagement.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo,Long> {

    Optional<UserInfo> findByUserId(Long userId);

}

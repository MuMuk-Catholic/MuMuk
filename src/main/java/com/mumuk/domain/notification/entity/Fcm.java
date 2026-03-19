package com.mumuk.domain.notification.entity;

import com.mumuk.domain.user.entity.User;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;

@Entity
@Table(name = "fcm")
public class Fcm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String fcmToken;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Fcm() {}

    // Getter
    public Long getId() {
        return id;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public User getUser() {
        return user;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Token 업데이트 메서드
    public String updateFcmToken(String updateToken) {
        return this.fcmToken = updateToken;
    }
}

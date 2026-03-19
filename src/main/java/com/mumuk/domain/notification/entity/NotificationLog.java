package com.mumuk.domain.notification.entity;

import com.mumuk.domain.user.entity.User;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "notificationlog")
public class NotificationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "fcm_message_id")
    private String fcmMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;
    //읽은 시각은 추가하지 않음.

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public NotificationLog(String title, String body, MessageStatus status, User user) {
        this.title = title;
        this.body = body;
        this.status = status;
        this.user = user;
    }

    // Getter
    public NotificationLog() {}

    public Long getId() {
        return id;}

    public String getTitle() {
        return title;}

    public String getBody() {
        return body;}

    public String getFcmMessageId() {
        return fcmMessageId;}

    public MessageStatus getStatus() {
        return status;}

    public User getUser() {
        return user;}

    // Setter
    public void setId(Long id) {
        this.id = id;}

    public void setTitle(String title) {
        this.title = title;}

    public void setBody(String body) {
        this.body = body;
    }
    public void setFcmMessageId(String fcmMessageId) {
        this.fcmMessageId = fcmMessageId;
    }
    public void setStatus(MessageStatus status) {
        this.status = status;
    }
    public void setUser(User user) {
        this.user = user;
    }
}

package com.mumuk.domain.ocr.entity;


import com.mumuk.global.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.util.Map;



@Entity
@Table(name = "user_health_data")
public class UserHealthData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;


    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> extractedData;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Map<String, String> getExtractedData() {
        return extractedData;
    }

    public UserHealthData(Long userId, Map<String, String> extractedData) {
        this.userId = userId;
        this.extractedData = extractedData;
    }

    public UserHealthData() {
    }
}

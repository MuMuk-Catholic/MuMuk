package com.mumuk.domain.healthManagement.dto.request;

import com.mumuk.domain.healthManagement.entity.AllergyType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class AllergyRequest {
    @Getter
    @Setter
    @NoArgsConstructor
    public static class SetAllergyReq {
        private List<AllergyType> allergyTypeList;
    }
}

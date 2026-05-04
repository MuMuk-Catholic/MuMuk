package com.mumuk.domain.healthManagement.dto.response;

import com.mumuk.domain.healthManagement.entity.AllergyType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public class AllergyResponse {

    @Getter
    @AllArgsConstructor
    public static class AllergyListRes{
        private List<AllergyTypeRes> allergyOptions;
        @Getter
        @AllArgsConstructor
        public static class AllergyTypeRes{
            private AllergyType allergyType;
            // 매개변수 하나만 사용하기는 하나 추후 확장 대비 이렇게 구현하기로 함
        }
    }

}

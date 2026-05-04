package com.mumuk.domain.search.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

public class SearchRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode //equals 와 hashcode 함수 자동으로 구현해줌!!

    public static class SavedRecentSearchReq {
        @NotNull
        private String title;
        @NotNull
        private String createdAt; // 이거 localdatetime으로 설정하면 직렬화 할 때 복잡해짐
    }



}
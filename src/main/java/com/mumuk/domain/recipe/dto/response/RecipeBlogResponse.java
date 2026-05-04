package com.mumuk.domain.recipe.dto.response;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RecipeBlogResponse {

    private List<Blog> blogs;

    @Getter
    @AllArgsConstructor
    public static class Blog{
        private String title;
        private String description;
        private String link;
        private String blogImageUrl;
    }
}

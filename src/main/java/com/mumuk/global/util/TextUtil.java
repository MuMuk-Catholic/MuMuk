package com.mumuk.global.util;

public class TextUtil {

    public static String stripTags(String html) {
        return html == null ? null : html.replaceAll("<[^>]*>", "");
    }

    public static String smartTruncate(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) return input;

        // 공백 기준으로 자르면서 최대 글자 수 초과 전까지만 유지
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            if (sb.length() + word.length() + 1 > maxLength) break;
            if (sb.length() > 0) sb.append(" ");
            sb.append(word);
        }

        String result = sb.toString();
        return result.length() < input.length() ? result + "..." : result;
    }
}

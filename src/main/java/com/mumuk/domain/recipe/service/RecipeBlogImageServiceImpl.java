package com.mumuk.domain.recipe.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;


@Slf4j
@Service
public class RecipeBlogImageServiceImpl implements RecipeBlogImageService {

    // 검색 설정
    private static final int SEARCH_TIMEOUT = 5000;
    private static final int MAX_HTML_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_BLOG_HTML_SIZE = 512 * 1024; // 512KB
    private static final int MAX_IMAGES_TO_PROCESS = 10;
    private static final int MAX_FALLBACK_IMAGES = 15;
    private static final int MAX_BLOG_POSTS = 5;
    private static final int MAX_BLOG_IMAGES = 10;
    private static final long BLOG_REQUEST_DELAY_MS = 150L; // 연속 요청 완화용 지연

    // URL 검증 상수/패턴 (성능/유지보수 개선)
    private static final Set<String> BLOCKED_KEYWORDS = Set.of(
            "favicon", "icon", "logo", "thumb", "small", "mini", "tiny"
    );
    private static final Set<String> SMALL_SIZE_PATTERNS = Set.of(
            "30x30", "32x32", "48x48", "54x54", "64x64", "128x128", "256x256"
    );
    private static final Set<String> NAVER_SMALL_TYPES = Set.of(
            "type=fff", "type=f30", "type=f48", "type=f54", "type=f64", "type=f128"
    );
    private static final Pattern SIZE_PATTERN = Pattern.compile("\\d+x\\d+");




    @Override
    public String searchRecipeImage(String recipeName) {
        log.info("레시피 이미지 검색 시작: {}", recipeName);

        try {
            // 1차: '사진' 키워드 추가로 먼저 시도 (사진 중심 검색)
            log.info("1차 검색: '사진' 키워드 추가 - {}", recipeName + " 사진");
            String imageUrl = searchNaverImage(recipeName + " 사진");
            if (imageUrl != null) {
                log.info("1차 검색 성공: {} -> {}", recipeName, imageUrl);
                return imageUrl;
            }

            // 2차: '레시피' 키워드 추가로 시도 (레시피 중심 검색)
            log.info("2차 검색: '레시피' 키워드 추가 - {}", recipeName + " 레시피");
            imageUrl = searchNaverImage(recipeName + " 레시피");
            if (imageUrl != null) {
                log.info("2차 검색 성공: {} -> {}", recipeName, imageUrl);
                return imageUrl;
            }

            // 3차: 원본 레시피명으로 직접 검색
            log.info("3차 검색: 원본 검색어 - {}", recipeName);
            imageUrl = searchNaverImage(recipeName);
            if (imageUrl != null) {
                log.info("3차 검색 성공: {} -> {}", recipeName, imageUrl);
                return imageUrl;
            }

            // 4차: 점진적 단어 제거 + 키워드 조합 검색
            log.info("4차 검색: 점진적 단어 제거 + 키워드 조합 - {}", recipeName);
            imageUrl = searchWithProgressiveWordRemoval(recipeName);
            if (imageUrl != null) {
                log.info("4차 검색 성공: {} -> {}", recipeName, imageUrl);
                return imageUrl;
            }

            // 5차: 영어 검색어로 재시도 (한글+영어 조합인 경우)
            if (containsMixedLanguage(recipeName)) {
                log.info("5차 검색: 영어 단어 추출 - {}", recipeName);
                String englishOnly = extractEnglishWords(recipeName);
                if (!englishOnly.isEmpty()) {
                    imageUrl = searchNaverImage(englishOnly);
                    if (imageUrl != null) {
                        log.info("5차 검색 성공: {} -> {}", englishOnly, imageUrl);
                        return imageUrl;
                    }
                }
            }

            log.warn("모든 검색 방법으로도 이미지를 찾을 수 없음: {}", recipeName);
            return null;

        } catch (Exception e) {
            log.error("레시피 이미지 검색 실패: {} - {}", recipeName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 네이버 이미지 검색 (최적화된 방식)
     */
    private String searchNaverImage(String recipeName) {
        try {
            String encodedQuery = URLEncoder.encode(recipeName, StandardCharsets.UTF_8);
            String searchUrl = "https://search.naver.com/search.naver?where=image&query=" + encodedQuery;

            log.info("네이버 이미지 검색 URL: {}", searchUrl);

            // 최적화된 연결 설정
            Document doc = Jsoup.connect(searchUrl)
                    .timeout(SEARCH_TIMEOUT)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .maxBodySize(MAX_HTML_SIZE)
                    .get();

            int htmlSize = doc.html().length();
            log.info("HTML 문서 크기: {} bytes ({} KB)", htmlSize, htmlSize / 1024);

            // HTML 크기가 너무 크면 경고
            if (htmlSize > 500 * 1024) { // 500KB 이상
                log.warn("HTML 문서가 너무 큽니다: {} KB (성능 저하 가능성)", htmlSize / 1024);
            }

            // 1단계: 원본 이미지 URL 추출 시도 (search.pstatic.net의 src 파라미터에서)
            log.info("1단계: 원본 이미지 URL 추출 시도");
            Elements imageResults = doc.select("img[src*='search.pstatic.net']");
            log.info("search.pstatic.net 이미지 수: {}", imageResults.size());

            // 처음 10개만 처리 (성능 최적화)
            int maxImages = Math.min(MAX_IMAGES_TO_PROCESS, imageResults.size());
            for (int i = 0; i < maxImages; i++) {
                Element image = imageResults.get(i);
                String src = image.attr("src");
                if (src != null && !src.isEmpty()) {
                    log.debug("search.pstatic.net 이미지 {}: {}", i + 1, src);

                    // src 파라미터에서 원본 URL 추출
                    String originalUrl = extractOriginalUrlFromPstatic(src);
                    if (originalUrl != null) {
                        log.info("원본 URL 추출 성공: {} -> {}", src, originalUrl);
                        if (isValidImageUrl(originalUrl)) {
                            log.info("원본 이미지 검증 통과: {} -> {}", recipeName, originalUrl);
                            return originalUrl;
                        } else {
                            log.debug("원본 이미지 검증 실패: {}", originalUrl);
                        }
                    }
                }
            }

            // 2단계: 네이버 블로그 직접 크롤링 시도
            log.info("2단계: 네이버 블로그 직접 크롤링 시도");
            String blogImageUrl = searchNaverBlogDirectly(recipeName);
            if (blogImageUrl != null) {
                log.info("블로그 직접 크롤링 성공: {} -> {}", recipeName, blogImageUrl);
                return blogImageUrl;
            }

            // 3단계: 기존 방식으로 검색 (fallback) - 제한된 수만 처리
            log.info("3단계: fallback 이미지 검색 시도");
            Elements fallbackImages = doc.select("img[data-lazy-src], img[data-source]");
            log.info("fallback 이미지 수: {}", fallbackImages.size());

            // 처음 15개만 처리 (성능 최적화)
            maxImages = Math.min(MAX_FALLBACK_IMAGES, fallbackImages.size());
            for (int i = 0; i < maxImages; i++) {
                Element image = fallbackImages.get(i);
                String src = image.attr("data-lazy-src");
                if (src == null || src.isEmpty()) {
                    src = image.attr("data-source");
                }
                if (src == null || src.isEmpty()) {
                    src = image.attr("src");
                }

                if (src != null && !src.isEmpty() && !src.contains("search.pstatic.net")) {
                    log.debug("fallback 이미지 {}: {}", i + 1, src);
                    if (isValidImageUrl(src)) {
                        log.info("fallback 이미지 검증 통과: {} -> {}", recipeName, src);
                        return src;
                    }
                }
            }

            log.warn("모든 검색 방법으로도 이미지를 찾을 수 없음: {}", recipeName);
            return null;

        } catch (Exception e) {
            log.error("네이버 이미지 검색 실패: {} - {}", recipeName, e.getMessage(), e);
            return null;
        }
    }







    /**
     * 이미지 URL 유효성 검사 (54x54 강력 차단)
     */
    private boolean isValidImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return false;
        // HTTP/HTTPS 프로토콜 확인
        if (!imageUrl.startsWith("http")) return false;

        String lowerUrl = imageUrl.toLowerCase();

        // 이미지 확장자 확인 (필수)
        if (!(lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") ||
                lowerUrl.contains(".png") || lowerUrl.contains(".webp") ||
                lowerUrl.contains(".gif"))) {
            log.info("이미지 확장자가 없어서 차단: {}", imageUrl);
            return false;
        }

        // 기본 차단 키워드
        for (String keyword : BLOCKED_KEYWORDS) {
            if (lowerUrl.contains(keyword)) {
                log.info("차단 키워드로 차단: {} (키워드: {})", imageUrl, keyword);
                return false;
            }
        }

        // 54x54 관련 URL 강력 차단 (특수케이스 우선 차단)
        if (lowerUrl.contains("54x54") || lowerUrl.contains("type=f54") ||
                lowerUrl.contains("_54") || lowerUrl.contains("54_")) {
            log.info("54x54 관련 URL 차단: {}", imageUrl);
            return false;
        }

        // 작은 이미지 크기 패턴 차단
        for (String pattern : SMALL_SIZE_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                log.info("작은 이미지 크기 패턴 차단: {} (패턴: {})", imageUrl, pattern);
                return false;
            }
        }

        // 네이버 이미지 검색 추가 검증
        if (lowerUrl.contains("search.pstatic.net")) {
            if (imageUrl.length() < 150) {
                log.info("URL이 너무 짧아서 차단: {} (길이: {})", imageUrl, imageUrl.length());
                return false;
            }
            for (String typeToken : NAVER_SMALL_TYPES) {
                if (lowerUrl.contains(typeToken)) {
                    log.info("작은 이미지 타입으로 차단: {} (토큰: {})", imageUrl, typeToken);
                    return false;
                }
            }
            // 숫자x숫자 패턴 차단 (미리 컴파일된 패턴 사용)
            if (SIZE_PATTERN.matcher(lowerUrl).find()) {
                log.info("숫자x숫자 패턴 차단: {}", imageUrl);
                return false;
            }
        }

        log.info("이미지 URL 검증 통과: {}", imageUrl);
        return true;
    }

    /**
     * search.pstatic.net URL에서 원본 이미지 URL 추출
     */
    private String extractOriginalUrlFromPstatic(String pstaticUrl) {
        try {
            if (pstaticUrl == null || !pstaticUrl.contains("search.pstatic.net")) {
                return null;
            }

            // URL 파라미터에서 src 값 추출
            if (pstaticUrl.contains("src=")) {
                String[] parts = pstaticUrl.split("src=");
                if (parts.length > 1) {
                    String srcPart = parts[1];
                    // &type=... 부분 제거
                    if (srcPart.contains("&")) {
                        srcPart = srcPart.split("&")[0];
                    }

                    // URL 디코딩
                    try {
                        String decodedUrl = java.net.URLDecoder.decode(srcPart, StandardCharsets.UTF_8.name());
                        log.info("원본 URL 추출: {} -> {}", pstaticUrl, decodedUrl);
                        return decodedUrl;
                    } catch (Exception e) {
                        log.warn("URL 디코딩 실패: {}", srcPart);
                        return srcPart;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.error("원본 URL 추출 실패: {} - {}", pstaticUrl, e.getMessage());
            return null;
        }
    }

    /**
     * 네이버 블로그 직접 크롤링으로 이미지 검색 (최적화)
     */
    private String searchNaverBlogDirectly(String recipeName) {
        try {
            String encodedQuery = URLEncoder.encode(recipeName + " 레시피", StandardCharsets.UTF_8);
            String blogSearchUrl = "https://search.naver.com/search.naver?where=blog&query=" + encodedQuery;

            log.info("네이버 블로그 검색 URL: {}", blogSearchUrl);

            // 블로그 검색 결과 크기 제한
            Document doc = Jsoup.connect(blogSearchUrl)
                    .timeout(SEARCH_TIMEOUT)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .maxBodySize(MAX_BLOG_HTML_SIZE)
                    .get();

            int htmlSize = doc.html().length();
            log.info("블로그 검색 HTML 크기: {} KB", htmlSize / 1024);

            // 블로그 포스트 링크 찾기 (처음 5개만)
            Elements blogLinks = doc.select("a[href*='blog.naver.com']");
            log.info("블로그 링크 발견: {}개 (처음 5개만 처리)", blogLinks.size());

            // 처음 5개 블로그 포스트에서 이미지 찾기 (성능 최적화)
            int maxBlogs = Math.min(MAX_BLOG_POSTS, blogLinks.size());
            for (int i = 0; i < maxBlogs; i++) {
                Element blogLink = blogLinks.get(i);
                String blogUrl = blogLink.attr("href");

                if (blogUrl != null && !blogUrl.isEmpty()) {
                    log.info("블로그 포스트 크롤링 시도 ({}/{}): {}", i + 1, maxBlogs, blogUrl);

                    try {
                        // 연속 요청에 대한 간단한 레이트 리미팅
                        delayBetweenRequests();
                        // 블로그 포스트 크기 제한
                        Document blogDoc = Jsoup.connect(blogUrl)
                                .timeout(SEARCH_TIMEOUT)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                                .maxBodySize(1024 * 1024) // 1MB 제한
                                .get();

                        int blogHtmlSize = blogDoc.html().length();
                        log.debug("블로그 포스트 HTML 크기: {} KB", blogHtmlSize / 1024);

                        // 블로그 포스트에서 이미지 찾기 (처음 10개만)
                        Elements blogImages = blogDoc.select("img[src*='blogfiles.naver.net'], img[src*='postfiles.naver.net']");
                        log.info("블로그 이미지 발견: {}개 (처음 {}개만 처리)", blogImages.size(), MAX_BLOG_IMAGES);

                        int maxImages = Math.min(MAX_BLOG_IMAGES, blogImages.size());
                        for (int j = 0; j < maxImages; j++) {
                            Element image = blogImages.get(j);
                            String src = image.attr("src");
                            if (src != null && !src.isEmpty()) {
                                log.debug("블로그 이미지 {}: {}", j + 1, src);
                                if (isValidImageUrl(src)) {
                                    log.info("블로그 이미지 검증 통과: {} -> {}", recipeName, src);
                                    return src;
                                }
                            }
                        }

                    } catch (Exception e) {
                        log.warn("블로그 포스트 크롤링 실패 ({}/{}): {} - {}", i + 1, maxBlogs, blogUrl, e.getMessage());
                        continue;
                    }
                }
            }

            log.info("블로그 크롤링으로 이미지를 찾을 수 없음: {}", recipeName);
            return null;

        } catch (Exception e) {
            log.error("네이버 블로그 직접 검색 실패: {} - {}", recipeName, e.getMessage());
            return null;
        }
    }

    private void delayBetweenRequests() {
        try {
            Thread.sleep(BLOG_REQUEST_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 크기 정보가 없는 이미지가 대용량 이미지일 가능성이 높은지 판단
     */
    private boolean isLikelyLargeImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return false;

        String lowerUrl = imageUrl.toLowerCase();

        // 네이버 이미지 검색에서 원본 이미지로 추정되는 패턴
        if (lowerUrl.contains("search.pstatic.net")) {
            // URL이 충분히 길고, 작은 이미지 관련 키워드가 없는 경우
            if (imageUrl.length() > 200 &&
                    !lowerUrl.contains("type=fff") &&
                    !lowerUrl.contains("type=f30") &&
                    !lowerUrl.contains("type=f48") &&
                    !lowerUrl.contains("type=f54") &&
                    !lowerUrl.contains("type=f64") &&
                    !lowerUrl.contains("type=f128") &&
                    !lowerUrl.contains("_t.") &&
                    !lowerUrl.contains("_s.") &&
                    !lowerUrl.contains("_m.") &&
                    !lowerUrl.contains("_l.") &&
                    !lowerUrl.contains("thumb") &&
                    !lowerUrl.contains("small") &&
                    !lowerUrl.contains("mini") &&
                    !lowerUrl.contains("tiny")) {
                log.info("대용량 이미지로 추정: {}", imageUrl);
                return true;
            }
        }

        // 블로그나 다른 소스의 이미지
        if (lowerUrl.contains("blogfiles") || lowerUrl.contains("daumcdn")) {
            // 썸네일 관련 키워드가 없는 경우
            if (!lowerUrl.contains("thumb") &&
                    !lowerUrl.contains("small") &&
                    !lowerUrl.contains("mini") &&
                    !lowerUrl.contains("tiny")) {
                log.info("블로그 이미지로 추정되어 대용량으로 판단: {}", imageUrl);
                return true;
            }
        }

        log.info("대용량 이미지로 추정되지 않음: {}", imageUrl);
        return false;
    }

    /**
     * 원본 이미지 URL인지 확인 (썸네일이 아닌 큰 이미지)
     */
    private boolean isOriginalImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return false;

        String lowerUrl = imageUrl.toLowerCase();

        // 네이버 이미지 검색에서 원본 이미지 URL 패턴 확인
        if (lowerUrl.contains("search.pstatic.net")) {
            // 썸네일 관련 키워드가 없고, URL이 충분히 긴 경우 원본으로 판단
            if (!lowerUrl.contains("thumb") &&
                    !lowerUrl.contains("small") &&
                    !lowerUrl.contains("mini") &&
                    !lowerUrl.contains("type=fff") &&
                    !lowerUrl.contains("type=f30") &&
                    !lowerUrl.contains("type=f48") &&
                    !lowerUrl.contains("type=f54") && // 54x54 추가
                    !lowerUrl.contains("type=f64") &&
                    !lowerUrl.contains("type=f128") &&
                    !lowerUrl.contains("_t.") &&
                    !lowerUrl.contains("_s.") &&
                    !lowerUrl.contains("_m.") &&
                    imageUrl.length() > 300) { // URL 길이를 300자 이상으로 증가
                return true;
            }
        }

        // 블로그나 다른 소스의 이미지도 확인
        if (lowerUrl.contains("blogfiles") || lowerUrl.contains("daumcdn")) {
            // 썸네일 관련 키워드가 없는 경우
            if (!lowerUrl.contains("thumb") &&
                    !lowerUrl.contains("small") &&
                    !lowerUrl.contains("mini")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 이미지 링크에서 원본 이미지 URL 추출
     */
    private String extractOriginalImageUrl(String imageLink) {
        try {
            // 네이버 이미지 링크에서 원본 URL 추출 시도
            if (imageLink.contains("search.pstatic.net")) {
                // 썸네일 URL을 원본 URL로 변환 시도
                // 예: thumb -> original, small -> original 등
                String originalUrl = imageLink
                        .replaceAll("thumb", "original")
                        .replaceAll("small", "original")
                        .replaceAll("mini", "original")
                        .replaceAll("_t", "_o")  // _t (thumbnail) -> _o (original)
                        .replaceAll("_s", "_o"); // _s (small) -> _o (original)

                // URL 길이가 충분히 긴 경우 원본으로 판단
                if (originalUrl.length() > 200) {
                    log.debug("썸네일 URL을 원본 URL로 변환: {} -> {}", imageLink, originalUrl);
                    return originalUrl;
                }
            }

            return null;
        } catch (Exception e) {
            log.debug("원본 이미지 URL 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 앞에서부터 단어를 하나씩 제거하며 이미지 검색 (새로운 전략)
     * 예: "AA BB CC" -> "BB CC 레시피" -> "BB CC 사진" -> "BB CC" -> "CC 레시피" -> "CC 사진" -> "CC"
     */
    private String searchWithProgressiveWordRemoval(String recipeName) {
        try {
            // 공백으로 단어 분리
            String[] words = recipeName.trim().split("\\s+");

            // 단어가 2개 이상일 때만 점진적 제거 시도
            if (words.length < 2) {
                log.debug("단어가 2개 미만이어서 점진적 제거 검색을 건너뜀: {}", recipeName);
                return null;
            }

            log.info("점진적 단어 제거 검색 시작: {} (단어 수: {})", recipeName, words.length);

            // 앞에서부터 단어를 하나씩 제거하며 검색
            for (int i = 1; i < words.length; i++) {
                // i번째 단어부터 끝까지 조합
                StringBuilder searchQuery = new StringBuilder();
                for (int j = i; j < words.length; j++) {
                    if (j > i) searchQuery.append(" ");
                    searchQuery.append(words[j]);
                }

                String simplifiedQuery = searchQuery.toString();
                log.info("점진적 검색 {}단계: {} -> {}", i, recipeName, simplifiedQuery);

                // 1차: 원본 검색어로 검색 (키워드 없이)
                log.info("  - 1차 시도: {}", simplifiedQuery);
                String imageUrl = searchNaverImage(simplifiedQuery);
                if (imageUrl != null) {
                    log.info("점진적 검색 성공 (원본): {} -> {}", simplifiedQuery, imageUrl);
                    return imageUrl;
                }

                // 2차: '사진' 키워드 추가로 검색
                log.info("  - 2차 시도: {} 사진", simplifiedQuery);
                imageUrl = searchNaverImage(simplifiedQuery + " 사진");
                if (imageUrl != null) {
                    log.info("점진적 검색 성공 (사진): {} -> {}", simplifiedQuery, imageUrl);
                    return imageUrl;
                }

                // 3차: '레시피' 키워드 추가로 검색
                log.info("  - 3차 시도: {} 레시피", simplifiedQuery);
                imageUrl = searchNaverImage(simplifiedQuery + " 레시피");
                if (imageUrl != null) {
                    log.info("점진적 검색 성공 (레시피): {} -> {}", simplifiedQuery, imageUrl);
                    return imageUrl;
                }
            }

            log.info("점진적 단어 제거 검색으로도 이미지를 찾을 수 없음: {}", recipeName);
            return null;

        } catch (Exception e) {
            log.error("점진적 단어 제거 검색 실패: {} - {}", recipeName, e.getMessage());
            return null;
        }
    }











    /**
     * 혼합 언어 검색어인지 확인 (한글+영어 조합)
     */
    private boolean containsMixedLanguage(String text) {
        if (text == null || text.isEmpty()) return false;

        boolean hasKorean = text.matches(".*[가-힣].*");
        boolean hasEnglish = text.matches(".*[a-zA-Z].*");

        return hasKorean && hasEnglish;
    }

    /**
     * 혼합 언어에서 영어 단어만 추출
     */
    private String extractEnglishWords(String text) {
        if (text == null || text.isEmpty()) return "";

        // 영어 단어만 추출 (공백으로 구분)
        String[] words = text.split("\\s+");
        StringBuilder englishWords = new StringBuilder();

        for (String word : words) {
            if (word.matches("[a-zA-Z]+")) {
                if (englishWords.length() > 0) {
                    englishWords.append(" ");
                }
                englishWords.append(word);
            }
        }

        return englishWords.toString();
    }

    /**
     * 이미지 크기 유효성 검사 (더 엄격한 기준)
     */
    private boolean isValidImageSize(String width, String height) {
        try {
            // 너무 작은 이미지 차단 (400px 미만)
            if (width != null && !width.isEmpty()) {
                int w = Integer.parseInt(width);
                if (w < 400) {
                    log.debug("너비가 너무 작음: {}px (최소 400px 필요)", w);
                    return false;
                }
            }

            if (height != null && !height.isEmpty()) {
                int h = Integer.parseInt(height);
                if (h < 400) {
                    log.debug("높이가 너무 작음: {}px (최소 400px 필요)", h);
                    return false;
                }
            }

            // 너무 극단적인 비율 차단 (예: 400x4000 같은 세로로 긴 이미지)
            if (width != null && !width.isEmpty() && height != null && !height.isEmpty()) {
                int w = Integer.parseInt(width);
                int h = Integer.parseInt(height);
                if (w > 0 && h > 0) {
                    double ratio = (double) Math.max(w, h) / Math.min(w, h);
                    if (ratio > 3.0) { // 비율을 3:1로 더 엄격하게 제한
                        log.debug("이미지 비율이 너무 극단적임: {}x{} (비율: {})", w, h, String.format("%.2f", ratio));
                        return false;
                    }
                }
            }

            return true;
        } catch (NumberFormatException e) {
            // 크기 정보가 없거나 숫자가 아닌 경우 기본적으로 허용
            log.debug("크기 정보 파싱 실패: width={}, height={}", width, height);
            return true;
        }
    }
}
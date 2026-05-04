package com.mumuk.global.util;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 *  MultipartFile => HTTP 전송 가능한 형태로 변환해주는 Util 클래스
 */
public class FileResourceUtil {

    public static Resource toResource(MultipartFile file) throws IOException {
        return new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename());
    }

    private static class MultipartInputStreamFileResource extends InputStreamResource {
        private final String filename;

        public MultipartInputStreamFileResource(InputStream inputStream, String filename) {
            super(inputStream);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return this.filename;
        }

        @Override
        public long contentLength() {
            return -1; // unknown
        }
    }
}

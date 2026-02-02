package com.example.demo.service;

import java.io.File;
import java.nio.file.Files;

import org.springframework.stereotype.Service;

@Service
public class ImageStorageService {

    private final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    public String save(File tempFile) throws Exception {

        String fileName = tempFile.getName();
        File dest = new File(UPLOAD_DIR + fileName);

        // フォルダ作成
        dest.getParentFile().mkdirs();

        Files.copy(tempFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // ★ ブラウザからアクセスする URL を返す
        return "/uploads/" + fileName;
    }
}

package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    // 画像を保存するディレクトリ (src/main/resources/static/uploads)
    private final Path rootLocation = Paths.get("src/main/resources/static/uploads");

    public FileStorageService() {
        try {
            // 起動時に /static/uploads ディレクトリがなければ作成する
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    /**
     * ファイルを保存し、ブラウザからアクセス可能なURLパスを返す
     * @param file アップロードされたファイル
     * @return /uploads/xxxxxxxx-xxxx.jpg のようなURLパス
     */
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            return null; // ファイルが空なら何もしない
        }
        
        try {
            // ファイル名が衝突しないように、ランダムなUUIDを付与
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String storedFilename = UUID.randomUUID().toString() + extension;
            
            // ファイルの保存先パスを決定
            Path destinationFile = this.rootLocation.resolve(storedFilename).normalize().toAbsolutePath();

            // ファイルを保存
            file.transferTo(destinationFile);
            
            // ブラウザからアクセスするためのパスを返す
            return "/uploads/" + storedFilename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}
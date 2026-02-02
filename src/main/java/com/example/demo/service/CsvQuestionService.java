package com.example.demo.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.Question;
import com.example.demo.entity.QuestionChoice;
import com.example.demo.repository.QuestionRepository;

@Service
@Transactional
public class CsvQuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    public List<Question> importCsv(MultipartFile csvFile, MultipartFile zipFile) throws Exception {
        try {
            System.out.println("===== CSVインポート開始 =====");
            Map<String, String> imageMap = extractZipImages(zipFile);

            List<Question> result = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(csvFile.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

                reader.mark(1);
                if (reader.read() != 0xFEFF) reader.reset();

                // ★ ここがポイント：引用符の設定を明示
                CSVFormat format = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreSurroundingSpaces(true)
                        .setQuote('"')             // 引用符をダブルクォーテーションに指定
                        .setAllowMissingColumnNames(true)
                        .build();

                try (CSVParser csvParser = new CSVParser(reader, format)) {
                    int row = 1;

                    for (CSVRecord record : csvParser) {
                        row++;
                        // record.get() で取得する時点で、外側の " " は既に外されています
                        
                        // 例: record.get(6) が "\"問題文\"" ではなく "問題文" になります
                        
                        if (record.size() < 18) continue;

                        Question q = new Question();
                        q.setExamCategory(record.get(0));
                        q.setExamSession(record.get(1));
                        q.setYear(Integer.parseInt(record.get(2).trim()));
                        q.setSeason(record.get(3));
                        q.setQuestionNumber(Integer.parseInt(record.get(4).trim()));
                        q.setTagId(Integer.parseInt(record.get(5).trim()));
                        
                        // テキストをセット（ライブラリが自動で " を除去済み）
                        q.setQuestionText(record.get(6));
                        q.setExplanation(record.get(16));

                        q.setImageUrl(imageMap.get(record.get(17)));

                        String correctRaw = record.get(15).trim();
                        int correctIndex = switch (correctRaw) {
                            case "ア" -> 0;
                            case "イ" -> 1;
                            case "ウ" -> 2;
                            case "エ" -> 3;
                            default -> throw new RuntimeException("不正な正解値 [" + correctRaw + "] row=" + row);
                        };

                        List<QuestionChoice> choices = new ArrayList<>();
                        choices.add(buildChoice(q, record.get(7),  imageMap.get(record.get(8)),  0, correctIndex));
                        choices.add(buildChoice(q, record.get(9),  imageMap.get(record.get(10)), 1, correctIndex));
                        choices.add(buildChoice(q, record.get(11), imageMap.get(record.get(12)), 2, correctIndex));
                        choices.add(buildChoice(q, record.get(13), imageMap.get(record.get(14)), 3, correctIndex));
                        q.setChoices(choices);

                        questionRepository.save(q);
                        result.add(q);
                        System.out.println("✔ 登録成功: 問" + q.getQuestionNumber());
                    }
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private Map<String, String> extractZipImages(MultipartFile zipFile) throws Exception {
        Map<String, String> savedMap = new HashMap<>();
        if (zipFile == null || zipFile.isEmpty()) {
            System.out.println("ZIPファイルが空のためスキップします");
            return savedMap;
        }

        File tempDir = Files.createTempDirectory("csv_zip").toFile();

        // ↓ ここを修正: 第2引数に Charset.forName("MS932") を追加
        try (ZipInputStream zis = new ZipInputStream(
                zipFile.getInputStream(), 
                java.nio.charset.Charset.forName("MS932"))) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                String fullName = entry.getName();
                String fileName = new File(fullName).getName();

                // システムファイルや隠しファイルをスキップ
                if (fileName.startsWith(".") || fileName.startsWith("__MACOSX") || fileName.isEmpty()) {
                    continue;
                }

                File temp = new File(tempDir, fileName);
                try (var fos = new java.io.FileOutputStream(temp)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = zis.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                }

                try {
                    String url = imageStorageService.save(temp);
                    savedMap.put(fileName, url);
                    System.out.println("画像保存: " + fileName + " -> " + url);
                } catch (Exception e) {
                    System.err.println("❌ 画像保存エラー (" + fileName + "): " + e.getMessage());
                }
            }
        } catch (IllegalArgumentException e) {
            // もし MS932 でもエラーが出る場合は、ここに来る可能性があります
            System.err.println("ZIPの文字コードエラーが依然として発生しています。");
            throw e;
        }
        return savedMap;
    }

    private QuestionChoice buildChoice(Question q, String text, String imageUrl, int index, int correctIndex) {
        QuestionChoice c = new QuestionChoice();
        c.setQuestion(q);
        c.setChoiceText(text == null || text.isBlank() ? null : text);
        c.setImageUrl(imageUrl);
        c.setCorrect(index == correctIndex);
        return c;
    }
}
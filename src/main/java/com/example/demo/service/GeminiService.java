package com.example.demo.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    // 最新のGemini 2.5 Flash モデルを使用
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String askGemini(String questionText, String userMessage, String imageBase64, String mimeType) {
        try {
            // ★★★ プロンプト（AIへの命令）を修正 ★★★
            // 「友達口調」や「過度な励まし」を排除し、事務的でフラットなAIアシスタントに変更
            String prompt = String.format("""
                あなたは学習支援用のAIアシスタントです。
                以下の問題についてユーザーから質問があります。
                
                【問題文】
                %s
                
                【ユーザーの質問】
                %s
                
                以下のルールを守って回答してください：
                1. 感情を込めすぎず、フラットで丁寧な「です・ます」調で話すこと。
                2. ユーザーを子供扱いしたり、過度に励ましたりしないこと。
                3. 質問に対して、簡潔かつ論理的に答えること。
                4. 「答えを教えて」と聞かれた場合は、正解とその理由（解説）を明確に提示すること。
                5. 画像が送られた場合は、その画像の内容も踏まえて回答すること。
                """, questionText, userMessage);

            // JSONボディ作成
            String jsonBody;
            
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                // 画像がある場合
                jsonBody = """
                    {
                      "contents": [{
                        "parts": [
                          {"text": "%s"},
                          {
                            "inline_data": {
                              "mime_type": "%s",
                              "data": "%s"
                            }
                          }
                        ]
                      }]
                    }
                    """.formatted(escapeJson(prompt), mimeType, imageBase64);
            } else {
                // テキストのみの場合
                jsonBody = """
                    {
                      "contents": [{
                        "parts": [{"text": "%s"}]
                      }]
                    }
                    """.formatted(escapeJson(prompt));
            }

            // 空白削除対策
            String safeKey = apiKey.trim();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL + "?key=" + safeKey))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(jsonBody))
                    .build();

            // 送信
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // デバッグログ
            System.out.println("--- Gemini API Debug ---");
            System.out.println("Status Code: " + response.statusCode());
            System.out.println("------------------------");

            if (response.statusCode() != 200) {
                return "APIエラーが発生しました: " + response.statusCode();
            }

            return extractTextFromResponse(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return "システムエラーが発生しました。";
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String extractTextFromResponse(String jsonResponse) {
        try {
            int startIndex = jsonResponse.indexOf("\"text\"");
            if (startIndex == -1) return "エラー: 解答が見つかりませんでした";
            
            startIndex = jsonResponse.indexOf("\"", startIndex + 6) + 1;
            int endIndex = jsonResponse.indexOf("\"", startIndex);
            
            String text = jsonResponse.substring(startIndex, endIndex);
            return text.replace("\\n", "\n").replace("\\\"", "\"");
        } catch (Exception e) {
            return "エラー: レスポンスの解析に失敗しました";
        }
    }
}
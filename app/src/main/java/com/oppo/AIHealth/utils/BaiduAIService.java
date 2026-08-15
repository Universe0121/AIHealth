package com.oppo.AIHealth.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.oppo.AIHealth.BuildConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BaiduAIService {

    private static final String TAG = "BaiduAIService";
    
    // 百度AI应用的API Key和Secret Key
    private static final String API_KEY = BuildConfig.BAIDU_API_KEY;
    private static final String SECRET_KEY = BuildConfig.BAIDU_SECRET_KEY;
    
    // 获取access_token的URL
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    
    // 食品识别API的URL
    private static final String FOOD_RECOGNITION_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v2/dish";
    
    private static BaiduAIService instance;
    private OkHttpClient client;
    private String accessToken;
    private long tokenExpireTime;
    
    private BaiduAIService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }
    
    public static synchronized BaiduAIService getInstance() {
        if (instance == null) {
            instance = new BaiduAIService();
        }
        return instance;
    }
    
    /**
     * 检查access_token是否有效
     */
    public boolean isTokenValid() {
        return accessToken != null && System.currentTimeMillis() < tokenExpireTime;
    }
    
    /**
     * 获取access_token
     */
    public void getAccessToken(final TokenCallback callback) {
        if (API_KEY.isEmpty() || SECRET_KEY.isEmpty()) {
            if (callback != null) {
                callback.onError("Baidu AI credentials are not configured.");
            }
            return;
        }

        if (isTokenValid()) {
            if (callback != null) {
                callback.onSuccess(accessToken);
            }
            return;
        }
        
        String url = TOKEN_URL + "?grant_type=client_credentials&client_id=" + API_KEY + "&client_secret=" + SECRET_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取access_token失败: " + e.getMessage());
                if (callback != null) {
                    callback.onError("获取access_token失败: " + e.getMessage());
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "获取access_token失败: " + response.code());
                    if (callback != null) {
                        callback.onError("获取access_token失败: " + response.code());
                    }
                    return;
                }
                
                String responseBody = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                
                if (jsonObject.has("access_token")) {
                    accessToken = jsonObject.get("access_token").getAsString();
                    int expiresIn = jsonObject.get("expires_in").getAsInt();
                    tokenExpireTime = System.currentTimeMillis() + (expiresIn - 3600) * 1000; // 提前1小时过期
                    Log.d(TAG, "获取access_token成功: " + accessToken);
                    if (callback != null) {
                        callback.onSuccess(accessToken);
                    }
                } else {
                    String errorMsg = jsonObject.has("error_description") ? jsonObject.get("error_description").getAsString() : "未知错误";
                    Log.e(TAG, "获取access_token失败: " + errorMsg);
                    if (callback != null) {
                        callback.onError("获取access_token失败: " + errorMsg);
                    }
                }
            }
        });
    }
    
    /**
     * 识别食物图片
     */
    public void recognizeFood(Bitmap bitmap, final RecognitionCallback callback) {
        getAccessToken(new TokenCallback() {
            @Override
            public void onSuccess(String token) {
                doRecognizeFood(bitmap, token, callback);
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * 执行食物识别
     */
    private void doRecognizeFood(Bitmap bitmap, String token, final RecognitionCallback callback) {
        try {
            // 将Bitmap转换为Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            
            // 构建请求参数
            RequestBody formBody = new FormBody.Builder()
                    .add("image", imageBase64)
                    .add("top_num", "5")
                    .add("filter_threshold", "0.5")
                    .build();
            
            String url = FOOD_RECOGNITION_URL + "?access_token=" + token;
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "食物识别失败: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("食物识别失败: " + e.getMessage());
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "食物识别失败: " + response.code());
                        if (callback != null) {
                            callback.onError("食物识别失败: " + response.code());
                        }
                        return;
                    }
                    
                    String responseBody = response.body().string();
                    Log.d(TAG, "食物识别结果: " + responseBody);
                    
                    JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                    if (jsonObject.has("result")) {
                        if (callback != null) {
                            callback.onSuccess(jsonObject);
                        }
                    } else {
                        String errorMsg = jsonObject.has("error_msg") ? jsonObject.get("error_msg").getAsString() : "未知错误";
                        Log.e(TAG, "食物识别失败: " + errorMsg);
                        if (callback != null) {
                            callback.onError("食物识别失败: " + errorMsg);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "食物识别失败: " + e.getMessage());
            if (callback != null) {
                callback.onError("食物识别失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * Token回调接口
     */
    public interface TokenCallback {
        void onSuccess(String token);
        void onError(String error);
    }
    
    /**
     * 识别回调接口
     */
    public interface RecognitionCallback {
        void onSuccess(JsonObject result);
        void onError(String error);
    }
}

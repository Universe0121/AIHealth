package com.aihealth.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.GeneralParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.WordSimple;
import com.aihealth.AiHealthApplication;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 百度OCR工具类（最终无报错版，适配所有SDK版本）
 */
public class OcrHelper {
    private static final String TAG = "OcrHelper";

    // 你的百度API Key和Secret Key
    private static final String API_KEY = "kxkWggujEKmbqzmpKkpypDOK";
    private static final String SECRET_KEY = "WubTqLkkUMlraXzgob5iaj8LqBTVPHF7";

    private static boolean isInit = false; // 全局初始化标记

    // 回调接口（保持原有兼容）
    public interface OcrCallback {
        void onOcrResult(String result);
        void onOcrError(String error);
        void onOcrProgress(int progress);
    }

    // 🔥 修复1：无参构造器（兼容 new OcrHelper()）
    public OcrHelper() {
        // 空实现，仅用于实例化
    }

    // 🔥 修复2：带Context的构造器（兼容 new OcrHelper(context)）
    public OcrHelper(Context context) {
        if (!isInit && context != null) {
            initOcrEngine(); // 传入Context时自动初始化
        }
    }

    // ========== 核心：initOcrEngine 为实例方法（非静态） ==========
    public boolean initOcrEngine() {
        try {
            if (isInit) return true; // 避免重复初始化

            // 从AiHealthApplication获取全局上下文
            Context context = AiHealthApplication.getContext();
            if (context == null) {
                Log.e(TAG, "初始化失败：上下文为空");
                return false;
            }

            // 调用百度OCR初始化接口
            OCR.getInstance(context).initAccessTokenWithAkSk(
                    new OnResultListener<AccessToken>() {
                        @Override
                        public void onResult(AccessToken result) {
                            isInit = true;
                            Log.i(TAG, "百度OCR初始化成功");
                        }

                        @Override
                        public void onError(OCRError error) {
                            Log.e(TAG, "百度OCR初始化失败：" + error.getMessage());
                        }
                    },
                    context.getApplicationContext(),
                    API_KEY,
                    SECRET_KEY
            );
            return true;
        } catch (Exception e) {
            Log.e(TAG, "OCR初始化异常：" + e.getMessage());
            return false;
        }
    }

    // ========== 核心修复：识别Bitmap（转临时文件方案，兼容所有版本） ==========
    public void recognizeText(Bitmap bitmap, OcrCallback callback) {
        Context context = AiHealthApplication.getContext();
        if (!isInit) {
            initOcrEngine(); // 未初始化则自动初始化
        }

        // 1. 校验Bitmap不为空
        if (bitmap == null) {
            if (callback != null) {
                callback.onOcrError("待识别图片为空");
            }
            return;
        }

        // 2. 将Bitmap转为临时文件（绕开API兼容问题）
        File tempFile = bitmapToFile(bitmap);
        if (tempFile == null) {
            if (callback != null) {
                callback.onOcrError("Bitmap转临时文件失败");
            }
            return;
        }

        // 3. 使用GeneralParams（兼容所有版本）
        GeneralParams params = new GeneralParams();
        params.setDetectDirection(true);
        params.setImageFile(tempFile); // 传入临时文件路径

        // 4. 进度回调
        if (callback != null) callback.onOcrProgress(20);

        // 5. 调用百度OCR识别接口
        OCR.getInstance(context).recognizeGeneral(params, new OnResultListener<GeneralResult>() {
            @Override
            public void onResult(GeneralResult result) {
                // 拼接识别结果
                StringBuilder resultText = new StringBuilder();
                for (WordSimple word : result.getWordList()) {
                    resultText.append(word.getWords()).append("\n");
                }

                // 回调结果
                if (callback != null) {
                    callback.onOcrProgress(100);
                    callback.onOcrResult(resultText.toString());
                }

                // 删除临时文件（释放空间）
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }

            @Override
            public void onError(OCRError error) {
                if (callback != null) {
                    callback.onOcrError("识别失败：" + error.getMessage());
                }

                // 删除临时文件
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        });
    }

    // ========== 识别图片路径（原有逻辑，保持兼容） ==========
    public void recognizeText(String imagePath, OcrCallback callback) {
        Context context = AiHealthApplication.getContext();
        if (!isInit) {
            initOcrEngine();
        }

        // 校验路径不为空且文件存在
        if (imagePath == null || imagePath.isEmpty()) {
            if (callback != null) {
                callback.onOcrError("图片路径为空");
            }
            return;
        }
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            if (callback != null) {
                callback.onOcrError("图片文件不存在：" + imagePath);
            }
            return;
        }

        GeneralParams params = new GeneralParams();
        params.setDetectDirection(true);
        params.setImageFile(imageFile);

        if (callback != null) callback.onOcrProgress(20);

        OCR.getInstance(context).recognizeGeneral(params, new OnResultListener<GeneralResult>() {
            @Override
            public void onResult(GeneralResult result) {
                StringBuilder resultText = new StringBuilder();
                for (WordSimple word : result.getWordList()) {
                    resultText.append(word.getWords()).append("\n");
                }
                if (callback != null) {
                    callback.onOcrProgress(100);
                    callback.onOcrResult(resultText.toString());
                }
            }

            @Override
            public void onError(OCRError error) {
                if (callback != null) {
                    callback.onOcrError("识别失败：" + error.getMessage());
                }
            }
        });
    }

    // ========== 辅助方法：Bitmap转临时文件（核心兼容方案） ==========
    private File bitmapToFile(Bitmap bitmap) {
        try {
            // 创建缓存目录下的临时文件（避免权限问题）
            File cacheDir = AiHealthApplication.getContext().getCacheDir();
            File tempFile = File.createTempFile("ocr_temp_", ".jpg", cacheDir);

            // 将Bitmap写入临时文件
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "Bitmap转文件失败：" + e.getMessage());
            return null;
        }
    }

    // ========== 兼容原有接口的方法（保留） ==========
    public void release() {
        Log.d(TAG, "OCR资源释放（百度OCR无需手动释放）");
    }

    public void setOcrParameter(String key, String value) {
        Log.d(TAG, "百度OCR不支持参数设置：" + key);
    }

    public boolean isUsingRealOcr() {
        return true;
    }
}
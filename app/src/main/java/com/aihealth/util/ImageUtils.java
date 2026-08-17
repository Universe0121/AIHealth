package com.aihealth.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUtils {
    private static final String TAG = "ImageUtils";

    // 压缩图片到指定大小（单位：KB）
    public static Bitmap compressImage(Bitmap image, int maxSizeKB) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            int quality = 85;
            while (outputStream.toByteArray().length / 1024 > maxSizeKB && quality > 10) {
                outputStream.reset();
                quality -= 5;
                image.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                Log.d(TAG, "压缩质量: " + quality + "%, 大小: " + outputStream.toByteArray().length / 1024 + "KB");
            }

            byte[] compressedData = outputStream.toByteArray();
            return BitmapFactory.decodeByteArray(compressedData, 0, compressedData.length);
        } catch (Exception e) {
            Log.e(TAG, "图片压缩失败: " + e.getMessage());
            return image;
        }
    }

    // 调整图片尺寸
    public static Bitmap resizeImage(Bitmap image, int maxWidth, int maxHeight) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();

            if (width <= maxWidth && height <= maxHeight) {
                return image;
            }

            float ratio = Math.min(
                    (float) maxWidth / width,
                    (float) maxHeight / height
            );

            int newWidth = (int) (width * ratio);
            int newHeight = (int) (height * ratio);

            return Bitmap.createScaledBitmap(image, newWidth, newHeight, true);
        } catch (Exception e) {
            Log.e(TAG, "调整图片尺寸失败: " + e.getMessage());
            return image;
        }
    }

    // 修正图片方向（有些手机拍照会旋转）
    public static Bitmap fixImageRotation(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }

            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            Log.e(TAG, "修正图片方向失败: " + e.getMessage());
            return bitmap;
        }
    }

    // 保存图片到应用私有目录
    public static String saveImageToPrivateStorage(Context context, Bitmap bitmap) {
        try {
            // 创建按日期分类的目录
            String dateDir = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date());

            File storageDir = new File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    dateDir
            );

            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            // 生成唯一文件名
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String fileName = "diagnosis_" + timeStamp + ".jpg";

            File imageFile = new File(storageDir, fileName);

            // 压缩并保存图片
            Bitmap compressedBitmap = compressImage(bitmap, 200); // 压缩到200KB以内
            compressedBitmap = resizeImage(compressedBitmap, 1200, 1200); // 限制最大尺寸

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
            outputStream.flush();
            outputStream.close();

            Log.d(TAG, "图片已保存: " + imageFile.getAbsolutePath() +
                    ", 大小: " + imageFile.length() / 1024 + "KB");

            return imageFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "保存图片失败: " + e.getMessage());
            return null;
        }
    }

    // 保存图片到公共相册（可选）
    public static Uri saveImageToGallery(Context context, Bitmap bitmap, String albumName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用MediaStore
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();

            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME,
                    "diagnosis_" + System.currentTimeMillis() + ".jpg");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/" + albumName);

            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

            if (imageUri != null) {
                try {
                    OutputStream outputStream = resolver.openOutputStream(imageUri);
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
                        outputStream.close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "保存到相册失败: " + e.getMessage());
                }
            }

            return imageUri;
        } else {
            // Android 10以下
            String savedImagePath = MediaStore.Images.Media.insertImage(
                    context.getContentResolver(),
                    bitmap,
                    "diagnosis_" + System.currentTimeMillis(),
                    "医疗诊断单"
            );

            return savedImagePath != null ? Uri.parse(savedImagePath) : null;
        }
    }

    // 从URI获取Bitmap
    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            Log.e(TAG, "从URI获取Bitmap失败: " + e.getMessage());
            return null;
        }
    }

    // 删除图片文件
    public static boolean deleteImageFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        try {
            File file = new File(filePath);
            if (file.exists()) {
                return file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "删除图片文件失败: " + e.getMessage());
        }

        return false;
    }

    // 获取图片文件信息
    public static String getImageFileInfo(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "未知文件";
        }

        try {
            File file = new File(filePath);
            if (file.exists()) {
                long fileSizeKB = file.length() / 1024;
                String fileName = file.getName();
                String fileDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date(file.lastModified()));

                return String.format(Locale.getDefault(),
                        "文件名: %s\n大小: %d KB\n修改时间: %s",
                        fileName, fileSizeKB, fileDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "获取图片文件信息失败: " + e.getMessage());
        }

        return "文件信息获取失败";
    }
}
package com.pic.catcher.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import com.lu.magic.util.AppUtil;
import com.lu.magic.util.IOUtil;
import com.lu.magic.util.thread.AppExecutor;
import com.pic.catcher.config.ModuleConfig;
import com.pic.catcher.provider.LogProvider;
import com.pic.catcher.ui.config.PicFormat;
import com.pic.catcher.util.FileUtils;
import com.pic.catcher.util.Md5Util;
import com.pic.catcher.util.PicUtil;
import com.pic.catcher.util.XLog;
import com.pic.catcher.util.http.HttpConnectUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * 图片导出管理器 - 采用 ContentProvider 跨进程安全写入
 */
public class PicExportManager {
    private static final String TAG = "PicCatcher";
    private static PicExportManager sInstance;

    public synchronized static PicExportManager getInstance() {
        if (sInstance == null) {
            sInstance = new PicExportManager();
        }
        return sInstance;
    }

    public void log(String msg) {
        XLog.i(AppUtil.getContext(), msg);
    }

    /**
     * 将字节数组通过 ContentProvider 写入插件目录
     */
    private void writeToProvider(byte[] data, String fileName) {
        Context context = AppUtil.getContext();
        // 构造特定的 Uri: content://com.evo.piccatcher.logprovider/save_pic?name=...&pkg=...
        Uri uri = LogProvider.CONTENT_URI.buildUpon()
                .appendPath("save_pic")
                .appendQueryParameter("name", fileName)
                .appendQueryParameter("pkg", context.getPackageName())
                .build();

        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "w")) {
            if (pfd != null) {
                try (FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor())) {
                    fos.write(data);
                    log("Success save (Provider): " + fileName);
                }
            }
        } catch (Exception e) {
            log("Provider save error: " + e.getMessage());
            // 如果 Provider 失败，作为兜底保存到宿主私有目录
            saveToHostPrivate(data, fileName);
        }
    }

    /**
     * 兜底方案：保存到宿主 App 自己的 Android/data 下
     */
    private void saveToHostPrivate(byte[] data, String fileName) {
        try {
            File dir = AppUtil.getContext().getExternalFilesDir("Pictures");
            if (dir != null) {
                File dest = new File(dir, fileName);
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    fos.write(data);
                    log("Fallback save to host: " + dest.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            log("All save methods failed: " + e.getMessage());
        }
    }

    public void exportBitmap(Bitmap bitmap) {
        runOnIo(() -> {
            if (bitmap == null || bitmap.isRecycled()) return;
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                String format = ModuleConfig.getInstance().getPicDefaultSaveFormat();
                Bitmap.CompressFormat cf = PicFormat.JPG.equals(format) ? Bitmap.CompressFormat.JPEG : 
                                         (PicFormat.PNG.equals(format) ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.WEBP);
                
                synchronized (bitmap) {
                    if (bitmap.isRecycled()) return;
                    bitmap.compress(cf, 100, bos);
                }
                
                byte[] bytes = bos.toByteArray();
                if (bytes.length == 0 || ModuleConfig.isLessThanMinSize(bytes.length)) return;

                String fileName = Md5Util.get(bytes) + "." + format;

                if (ModuleConfig.getInstance().isSaveToInternal()) {
                    writeToProvider(bytes, fileName);
                } else {
                    saveToPublic(bytes, fileName);
                }
            } catch (Exception e) {
                log("Export bitmap error: " + e.getMessage());
            }
        });
    }

    private void saveToPublic(byte[] data, String fileName) {
        try {
            File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File dir = new File(publicDir, "PicCatcher/" + AppUtil.getContext().getPackageName());
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(data);
                log("Success save public: " + dest.getAbsolutePath());
            }
        } catch (Exception e) {
            log("Public save error: " + e.getMessage());
        }
    }

    public void exportByteArray(final byte[] dataBytes, String lastName) {
        if (dataBytes == null || dataBytes.length == 0) return;
        if (ModuleConfig.isLessThanMinSize(dataBytes.length)) return;
        
        runOnIo(() -> {
            String suffix = TextUtils.isEmpty(lastName) ? PicUtil.detectImageType(dataBytes, "bin") : lastName;
            if (!suffix.startsWith(".")) suffix = "." + suffix;
            String fileName = Md5Util.get(dataBytes) + suffix;

            if (ModuleConfig.getInstance().isSaveToInternal()) {
                writeToProvider(dataBytes, fileName);
            } else {
                saveToPublic(dataBytes, fileName);
            }
        });
    }

    public void exportBitmapFile(File file) {
        runOnIo(() -> {
            if (!file.exists()) return;
            try (FileInputStream fis = new FileInputStream(file);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                // 使用项目中已有的 FileUtils.copyFile 替代找不到的 IOUtil.copy
                FileUtils.copyFile(fis, bos);
                exportByteArray(bos.toByteArray(), MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath()));
            } catch (Exception e) {
                log("Export file error: " + e.getMessage());
            }
        });
    }

    public void exportUrlIfNeed(String url) {
        runOnIo(() -> {
            if (TextUtils.isEmpty(url)) return;
            String fileEx = MimeTypeMap.getFileExtensionFromUrl(url).toLowerCase(Locale.ROOT);
            if (URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url)) {
                HttpConnectUtil.request("GET", url, null, null, true, response -> {
                    byte[] body = response.getBody();
                    if (body != null) exportByteArray(body, fileEx);
                    return null;
                });
            }
        });
    }

    public void runOnIo(Runnable runnable) {
        if (Looper.getMainLooper().isCurrentThread()) {
            AppExecutor.io().execute(runnable);
        } else {
            runnable.run();
        }
    }
}

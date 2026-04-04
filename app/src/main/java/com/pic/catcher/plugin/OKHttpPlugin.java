package com.pic.catcher.plugin;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.lu.lposed.api2.XC_MethodHook2;
import com.lu.lposed.api2.XposedHelpers2;
import com.lu.lposed.plugin.IPlugin;
import com.lu.magic.util.AppUtil;
import com.lu.magic.util.log.LogUtil;
import com.pic.catcher.ClazzN;
import com.pic.catcher.config.ModuleConfig;
import com.pic.catcher.util.PicUtil;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class OKHttpPlugin implements IPlugin {
    @Override
    public void handleHook(Context context, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        handleHookOkHttp3(context, loadPackageParam);
        handleHookAndroidOkHttp(context, loadPackageParam);
    }

    private void handleHookAndroidOkHttp(Context context, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        Class<?> httpEngineClazz = ClazzN.from("com.android.okhttp.internal.http.HttpEngine");
        if (httpEngineClazz == null) return;

        XposedHelpers2.findAndHookMethod(
                httpEngineClazz,
                "readResponse",
                new XC_MethodHook2() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!ModuleConfig.getInstance().isCatchNetPic()) return;
                        
                        try {
                            Object response = XposedHelpers2.getObjectField(param.thisObject, "userResponse");
                            if (response == null) return;

                            String contentType = (String) XposedHelpers2.callMethod(response, "header", "Content-Type");
                            if (TextUtils.isEmpty(contentType)) return;

                            String guessFileEx = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType);
                            if (!PicUtil.isPicSuffix(guessFileEx)) return;

                            Object body = XposedHelpers2.callMethod(response, "body");
                            if (body == null) return;

                            Object bufferSource = XposedHelpers2.callMethod(body, "source");
                            if (bufferSource == null) return;

                            // 注意：readByteArray 会消耗掉流，导致原应用无法读取而闪退。
                            // 在没有 peekSource 的情况下，Hook 这里风险极高。
                            // 暂时增加 try-catch 保护
                            Object bytes = XposedHelpers2.callMethod(bufferSource, "readByteArray");
                            if (bytes != null) {
                                // 尝试写回（这是一个黑科技，不一定在所有版本生效）
                                Object buffer = XposedHelpers2.getObjectField(bufferSource, "buffer");
                                if (buffer != null) {
                                    XposedHelpers2.callMethod(buffer, "write", bytes);
                                }
                                PicExportManager.getInstance().exportByteArray((byte[]) bytes, guessFileEx);
                            }
                        } catch (Throwable t) {
                            LogUtil.w("AndroidOkHttp hook error", t);
                        }
                    }
                }
        );
    }

    private void handleHookOkHttp3(Context context, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        ClassLoader clazzLoader = AppUtil.getClassLoader();
        Class<?> realCallClazz = ClazzN.from("okhttp3.RealCall", clazzLoader);
        if (realCallClazz == null) return;

        XposedHelpers2.findAndHookMethod(
                realCallClazz,
                "getResponseWithInterceptorChain",
                new XC_MethodHook2() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!ModuleConfig.getInstance().isCatchNetPic()) return;
                        
                        Object response = param.getResult();
                        if (response == null) return;

                        try {
                            String contentType = (String) XposedHelpers2.callMethod(response, "header", "Content-Type");
                            if (TextUtils.isEmpty(contentType)) return;

                            String guessFileEx = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType);
                            if (!PicUtil.isPicSuffix(guessFileEx)) return;

                            // OkHttp3 使用 peekBody 是安全的，它不会消耗原始流
                            Object response2 = XposedHelpers2.callMethod(response, "peekBody", 1024 * 1024 * 5L); // 限制 5MB
                            if (response2 != null) {
                                Object bytes = XposedHelpers2.callMethod(response2, "bytes");
                                if (bytes instanceof byte[]) {
                                    PicExportManager.getInstance().exportByteArray((byte[]) bytes, guessFileEx);
                                }
                            }
                        } catch (Throwable t) {
                            LogUtil.w("OkHttp3 hook error", t);
                        }
                    }
                }
        );
    }
}

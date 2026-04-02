package com.pic.catcher

import android.util.Base64
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AppBuildInfo(
    val buildTime: String,
    val branch: String,
    val commit: String,
) {
    companion object {
        fun of(): com.pic.catcher.AppBuildInfo {
            val json = JSONObject(Base64.decode(BuildConfig.buildInfoJson64, Base64.DEFAULT).toString(Charsets.UTF_8))
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'XXX", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("GMT+08:00")
            return com.pic.catcher.AppBuildInfo(
                sdf.format(json.optLong("time")),
                json.optString("branch"),
                json.optString("commit").substring(0, 11)
            )
        }
    }
}

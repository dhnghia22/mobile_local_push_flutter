package com.yourorg.mobile_local_push

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    override fun doWork(): Result {
        return try {
            val id = inputData.getString("id") ?: ""
            val title = inputData.getString("title") ?: ""
            val description = inputData.getString("description") ?: ""
            
            Log.d("NotificationWorker", "Displaying notification: $id, $title")
            
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "mobile_local_push_channel"
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(channelId, "Local Push", android.app.NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }
            
            // Tạo intent để mở app khi tap notification
            val appIntent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            appIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            
            val pendingIntent = android.app.PendingIntent.getActivity(
                applicationContext,
                id.hashCode(),
                appIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val builder = android.app.Notification.Builder(applicationContext, channelId)
                .setContentTitle(title)
                .setContentText(description)
                .setSmallIcon(getDefaultIcon(applicationContext))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val defaultColor = getDefaultColor(applicationContext)
            if (defaultColor != null) {
                builder.setColor(defaultColor)
            }
                
            notificationManager.notify(id.hashCode(), builder.build())
            
            // Xóa notification khỏi SharedPreferences sau khi đã hiển thị
            val prefs = applicationContext.getSharedPreferences("mobile_local_push", Context.MODE_PRIVATE)
            val notifications = prefs.getStringSet("notifications", mutableSetOf()) ?: mutableSetOf()
            val filtered = notifications.filterNot { notif: String -> notif.startsWith("${id}|") }.toMutableSet()
            prefs.edit().putStringSet("notifications", filtered).apply()
            
            Log.d("NotificationWorker", "Notification displayed and removed from SharedPreferences: $id")
            
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error displaying notification", e)
            Result.failure()
        }
    }

    private fun getDefaultColor(context: Context): Int? {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(
                context.packageName, 
                PackageManager.GET_META_DATA
            )
            val colorResId = applicationInfo.metaData?.getInt("com.nghiadinh.mobile_local_push.default_color")
            if (colorResId != null && colorResId != 0) {
                context.getColor(colorResId)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getDefaultIcon(context: Context): Int {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(
                context.packageName, 
                PackageManager.GET_META_DATA
            )
            val iconResId = applicationInfo.metaData?.getInt("com.nghiadinh.mobile_local_push.default_icon")
            if (iconResId != null && iconResId != 0) {
                iconResId
            } else {
                // Sử dụng ic_launcher hoặc fallback an toàn
                context.applicationInfo.icon
            }
        } catch (e: Exception) {
            // Fallback cuối cùng - sử dụng ic_launcher của app
            context.applicationInfo.icon
        }
    }
}

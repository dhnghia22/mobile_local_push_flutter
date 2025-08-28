
package com.yourorg.mobile_local_push

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import android.app.Activity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.util.Log

/** MobileLocalPushPlugin */
class MobileLocalPushPlugin :
    FlutterPlugin,
    MethodCallHandler,
    ActivityAware {
    // The MethodChannel that will the communication between Flutter and native Android
    //
    // This local reference serves to register the plugin with the Flutter Engine and unregister it
    // when the Flutter Engine is detached from the Activity1
    private lateinit var channel: MethodChannel
    private lateinit var context: android.content.Context
    private var activity: Activity? = null
    private var permissionResult: Result? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "mobile_local_push")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addRequestPermissionsResultListener { requestCode, permissions, grantResults ->
            if (requestCode == 1001 && permissionResult != null) {
                val granted = grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
                permissionResult?.success(granted)
                permissionResult = null
                return@addRequestPermissionsResultListener true
            }
            false
        }
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onMethodCall(
        call: MethodCall,
        result: Result
    ) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "requestPermission" -> {
                val act = activity
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    if (act != null) {
                        val permission = android.Manifest.permission.POST_NOTIFICATIONS
                        if (android.content.pm.PackageManager.PERMISSION_GRANTED ==
                            act.checkSelfPermission(permission)) {
                            result.success(true)
                        } else {
                            permissionResult = result
                            act.requestPermissions(arrayOf(permission), 1001)
                        }
                    } else {
                        result.success(false)
                    }
                } else {
                    result.success(true)
                }
            }
            "getPermissionStatus" -> {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    val act = activity
                    if (act != null) {
                        val permission = android.Manifest.permission.POST_NOTIFICATIONS
                        val granted = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                            act.checkSelfPermission(permission)
                        result.success(granted)
                    } else {
                        result.success(false)
                    }
                } else {
                    result.success(true)
                }
            }
            // ...existing code...
            "schedule" -> {
                val title = call.argument<String>("title") ?: ""
                val description = call.argument<String>("description") ?: ""
                val timestamp = call.argument<Int>("timestamp") ?: 0
                val id = timestamp.toString()
                Log.d("MobileLocalPushPlugin", "Scheduling alarm for timestamp: $timestamp, id: $id, title: $title")

                val intent = android.content.Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("id", id)
                    putExtra("title", title)
                    putExtra("description", description)
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    id.hashCode(),
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                
                // Kiểm tra quyền SCHEDULE_EXACT_ALARM cho Android 12+
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Log.e("MobileLocalPushPlugin", "Cannot schedule exact alarms - permission not granted")
                        result.error("PERMISSION_DENIED", "Cannot schedule exact alarms", null)
                        return
                    }
                }
                
                val scheduleTime = timestamp.toLong() * 1000
                val currentTime = System.currentTimeMillis()
                Log.d("MobileLocalPushPlugin", "Current time: $currentTime, Schedule time: $scheduleTime")
                
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    scheduleTime,
                    pendingIntent
                )
                
                Log.d("MobileLocalPushPlugin", "Alarm scheduled successfully")
                
                // Lưu vào SharedPreferences
                val prefs = context.getSharedPreferences("mobile_local_push", android.content.Context.MODE_PRIVATE)
                val notifications = prefs.getStringSet("notifications", mutableSetOf()) ?: mutableSetOf()
                val info = "${id}|${title}|${description}|${timestamp}"
                notifications.add(info)
                prefs.edit().putStringSet("notifications", notifications).apply()
                result.success(null)
            }
            "getAllScheduledNotifications" -> {
                val prefs = context.getSharedPreferences("mobile_local_push", android.content.Context.MODE_PRIVATE)
                val notifications = prefs.getStringSet("notifications", mutableSetOf()) ?: mutableSetOf()
                val list = notifications.map { notif: String ->
                    val parts = notif.split("|")
                    mapOf(
                        "id" to parts.getOrNull(0),
                        "title" to parts.getOrNull(1),
                        "description" to parts.getOrNull(2),
                        "timestamp" to parts.getOrNull(3)?.toLongOrNull()
                    )
                }
                result.success(list)
            }
            "cancelNotification" -> {
                val id = call.argument<String>("id") ?: ""
                val intent = android.content.Intent(context, NotificationReceiver::class.java)
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    id.hashCode(),
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                alarmManager.cancel(pendingIntent)
                // Xóa khỏi SharedPreferences
                val prefs = context.getSharedPreferences("mobile_local_push", android.content.Context.MODE_PRIVATE)
                val notifications = prefs.getStringSet("notifications", mutableSetOf()) ?: mutableSetOf()
                val filtered = notifications.filterNot { notif: String -> notif.startsWith("${id}|") }.toMutableSet()
                prefs.edit().putStringSet("notifications", filtered).apply()
                result.success(null)
            }
            "cancelAllNotifications" -> {
                val prefs = context.getSharedPreferences("mobile_local_push", android.content.Context.MODE_PRIVATE)
                val notifications = prefs.getStringSet("notifications", mutableSetOf()) ?: mutableSetOf()
                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                notifications.forEach { notif: String ->
                    val id = notif.split("|").getOrNull(0) ?: ""
                    val intent = android.content.Intent(context, NotificationReceiver::class.java)
                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        id.hashCode(),
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.cancel(pendingIntent)
                }
                prefs.edit().putStringSet("notifications", mutableSetOf()).apply()
                result.success(null)
            }
            "rescheduleNotifications" -> {
                val prefs = context.getSharedPreferences("mobile_local_push", android.content.Context.MODE_PRIVATE)
                val notifications = prefs.getStringSet("notifications", mutableSetOf()) ?: mutableSetOf()
                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                
                // Kiểm tra quyền SCHEDULE_EXACT_ALARM cho Android 12+
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Log.e("MobileLocalPushPlugin", "Cannot reschedule exact alarms - permission not granted")
                        result.error("PERMISSION_DENIED", "Cannot reschedule exact alarms", null)
                        return
                    }
                }
                
                val currentTime = System.currentTimeMillis()
                var rescheduledCount = 0
                val validNotifications = mutableSetOf<String>()
                
                notifications.forEach { notif: String ->
                    val parts = notif.split("|")
                    if (parts.size >= 4) {
                        val id = parts[0]
                        val title = parts[1]
                        val description = parts[2]
                        val timestamp = parts[3].toLongOrNull() ?: 0
                        val scheduleTime = timestamp * 1000
                        
                        // Chỉ reschedule những notification chưa hết hạn
                        if (scheduleTime > currentTime) {
                            val intent = android.content.Intent(context, NotificationReceiver::class.java).apply {
                                putExtra("id", id)
                                putExtra("title", title)
                                putExtra("description", description)
                            }
                            
                            val pendingIntent = android.app.PendingIntent.getBroadcast(
                                context,
                                id.hashCode(),
                                intent,
                                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                            )
                            
                            alarmManager.setExact(
                                android.app.AlarmManager.RTC_WAKEUP,
                                scheduleTime,
                                pendingIntent
                            )
                            
                            validNotifications.add(notif)
                            rescheduledCount++
                        }
                    }
                }
                
                // Cập nhật SharedPreferences, xóa những notification đã hết hạn
                prefs.edit().putStringSet("notifications", validNotifications).apply()
                result.success(rescheduledCount)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
class NotificationReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        Log.d("NotificationReceiver", "onReceive called")
        val id = intent.getStringExtra("id") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "mobile_local_push_channel"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Local Push", android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        // Tạo intent để mở app khi tap notification
        val appIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        appIntent?.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            id.hashCode(),
            appIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = android.app.Notification.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(getDefaultIcon(context))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // Thêm intent để mở app

        val defaultColor = getDefaultColor(context)
        if (defaultColor != null) {
            builder.setColor(defaultColor)
        }
            
        notificationManager.notify(id.hashCode(), builder.build())
        
        // Xóa notification khỏi SharedPreferences sau khi đã hiển thị
        val prefs = context.getSharedPreferences("mobile_local_push", android.content.Context.MODE_PRIVATE)
        val notifications = prefs.getStringSet("notifications", mutableSetOf()) ?: mutableSetOf()
        val filtered = notifications.filterNot { notif: String -> notif.startsWith("${id}|") }.toMutableSet()
        prefs.edit().putStringSet("notifications", filtered).apply()
        
        Log.d("NotificationReceiver", "Notification displayed and removed from SharedPreferences: $id")
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

    // Function để đọc icon từ metadata
    private fun getDefaultIcon(context: Context): Int {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(
                context.packageName, 
                PackageManager.GET_META_DATA
            )
            val iconResId = applicationInfo.metaData?.getInt("com.nghiadinh.mobile_local_push.default_icon")
            iconResId ?: android.R.drawable.ic_dialog_info
        } catch (e: Exception) {
            android.R.drawable.ic_dialog_info
        }
    }
}

class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        Log.d("BootReceiver", "Boot completed, rescheduling notifications")
        
        if (intent.action == android.content.Intent.ACTION_BOOT_COMPLETED ||
            intent.action == android.content.Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == android.content.Intent.ACTION_PACKAGE_REPLACED) {
            
            val prefs = context.getSharedPreferences("mobile_local_push", android.content.Context.MODE_PRIVATE)
            val notifications = prefs.getStringSet("notifications", mutableSetOf()) ?: mutableSetOf()
            val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            
            // Kiểm tra quyền SCHEDULE_EXACT_ALARM cho Android 12+
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e("BootReceiver", "Cannot reschedule exact alarms - permission not granted")
                    return
                }
            }
            
            val currentTime = System.currentTimeMillis()
            var rescheduledCount = 0
            val validNotifications = mutableSetOf<String>()
            
            notifications.forEach { notif: String ->
                val parts = notif.split("|")
                if (parts.size >= 4) {
                    val id = parts[0]
                    val title = parts[1] 
                    val description = parts[2]
                    val timestamp = parts[3].toLongOrNull() ?: 0
                    val scheduleTime = timestamp * 1000
                    
                    // Chỉ reschedule những notification chưa hết hạn
                    if (scheduleTime > currentTime) {
                        val notificationIntent = android.content.Intent(context, NotificationReceiver::class.java).apply {
                            putExtra("id", id)
                            putExtra("title", title)
                            putExtra("description", description)
                        }
                        
                        val pendingIntent = android.app.PendingIntent.getBroadcast(
                            context,
                            id.hashCode(),
                            notificationIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        
                        alarmManager.setExact(
                            android.app.AlarmManager.RTC_WAKEUP,
                            scheduleTime,
                            pendingIntent
                        )
                        
                        validNotifications.add(notif)
                        rescheduledCount++
                        Log.d("BootReceiver", "Rescheduled notification: $id at $scheduleTime")
                    } else {
                        Log.d("BootReceiver", "Skipped expired notification: $id")
                    }
                }
            }
            
            // Cập nhật SharedPreferences, xóa những notification đã hết hạn
            prefs.edit().putStringSet("notifications", validNotifications).apply()
            Log.d("BootReceiver", "Rescheduled $rescheduledCount notifications after boot")
        }
    }
}

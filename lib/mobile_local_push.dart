import 'mobile_local_push_platform_interface.dart';

class MobileLocalPush {
  Future<void> schedule({
    required String title,
    required String description,
    required int timestamp,
  }) {
    return MobileLocalPushPlatform.instance.schedule(
      title: title,
      description: description,
      timestamp: timestamp,
    );
  }

  Future<List<Map<String, dynamic>>> getAllScheduledNotifications() {
    return MobileLocalPushPlatform.instance.getAllScheduledNotifications();
  }

  Future<void> cancelAllNotifications() {
    return MobileLocalPushPlatform.instance.cancelAllNotifications();
  }

  Future<bool> getPermissionStatus() {
    return MobileLocalPushPlatform.instance.getPermissionStatus();
  }

  Future<String?> getPlatformVersion() {
    return MobileLocalPushPlatform.instance.getPlatformVersion();
  }

  Future<bool> requestPermission() {
    return MobileLocalPushPlatform.instance.requestPermission();
  }

  /// Reschedules all valid notifications (useful after device restart).
  /// Returns the number of notifications that were rescheduled.
  Future<int> rescheduleNotifications() {
    return MobileLocalPushPlatform.instance.rescheduleNotifications();
  }
}

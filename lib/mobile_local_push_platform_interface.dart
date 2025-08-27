import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'mobile_local_push_method_channel.dart';

abstract class MobileLocalPushPlatform extends PlatformInterface {
  /// Returns true if notification permission is granted, false otherwise.
  Future<bool> getPermissionStatus() {
    throw UnimplementedError('getPermissionStatus() has not been implemented.');
  }

  /// Constructs a MobileLocalPushPlatform.
  MobileLocalPushPlatform() : super(token: _token);

  static final Object _token = Object();

  static MobileLocalPushPlatform _instance = MethodChannelMobileLocalPush();

  /// The default instance of [MobileLocalPushPlatform] to use.
  ///
  /// Defaults to [MethodChannelMobileLocalPush].
  static MobileLocalPushPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [MobileLocalPushPlatform] when
  /// they register themselves.
  static set instance(MobileLocalPushPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  /// Schedules a local notification with title, description, and timestamp.
  Future<void> schedule({
    required String title,
    required String description,
    required int timestamp,
  }) {
    throw UnimplementedError('schedule() has not been implemented.');
  }

  /// Gets all scheduled notifications.
  Future<List<Map<String, dynamic>>> getAllScheduledNotifications() {
    throw UnimplementedError(
      'getAllScheduledNotifications() has not been implemented.',
    );
  }

  /// Cancels a scheduled notification by id.
  Future<void> cancelNotification(String id) {
    throw UnimplementedError('cancelNotification() has not been implemented.');
  }

  /// Cancels all scheduled notifications.
  Future<void> cancelAllNotifications() {
    throw UnimplementedError(
      'cancelAllNotifications() has not been implemented.',
    );
  }

  /// Requests notification permission from the user.
  Future<bool> requestPermission() {
    throw UnimplementedError('requestPermission() has not been implemented.');
  }

  /// Reschedules all valid notifications (useful after device restart).
  Future<int> rescheduleNotifications() {
    throw UnimplementedError(
      'rescheduleNotifications() has not been implemented.',
    );
  }
}

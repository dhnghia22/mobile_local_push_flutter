import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'mobile_local_push_platform_interface.dart';

/// An implementation of [MobileLocalPushPlatform] that uses method channels.
class MethodChannelMobileLocalPush extends MobileLocalPushPlatform {
  @override
  Future<bool> getPermissionStatus() async {
    final result = await methodChannel.invokeMethod<bool>(
      'getPermissionStatus',
    );
    return result ?? false;
  }

  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('mobile_local_push');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }

  @override
  Future<void> schedule({
    required String title,
    required String description,
    required int timestamp,
  }) async {
    await methodChannel.invokeMethod('schedule', {
      'title': title,
      'description': description,
      'timestamp': timestamp,
    });
  }

  @override
  Future<List<Map<String, dynamic>>> getAllScheduledNotifications() async {
    final result = await methodChannel.invokeMethod<List>(
      'getAllScheduledNotifications',
    );
    if (result == null) return [];
    return result.map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  @override
  Future<void> cancelNotification(String id) async {
    await methodChannel.invokeMethod('cancelNotification', {'id': id});
  }

  @override
  Future<void> cancelAllNotifications() async {
    await methodChannel.invokeMethod('cancelAllNotifications');
  }

  @override
  Future<bool> requestPermission() async {
    final result = await methodChannel.invokeMethod<bool>('requestPermission');
    return result ?? false;
  }

  @override
  Future<int> rescheduleNotifications() async {
    final result = await methodChannel.invokeMethod<int>(
      'rescheduleNotifications',
    );
    return result ?? 0;
  }
}

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile_local_push/mobile_local_push.dart';
import 'package:mobile_local_push/mobile_local_push_platform_interface.dart';
import 'package:mobile_local_push/mobile_local_push_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockMobileLocalPushPlatform
    with MockPlatformInterfaceMixin
    implements MobileLocalPushPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final MobileLocalPushPlatform initialPlatform = MobileLocalPushPlatform.instance;

  test('$MethodChannelMobileLocalPush is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelMobileLocalPush>());
  });

  test('getPlatformVersion', () async {
    MobileLocalPush mobileLocalPushPlugin = MobileLocalPush();
    MockMobileLocalPushPlatform fakePlatform = MockMobileLocalPushPlatform();
    MobileLocalPushPlatform.instance = fakePlatform;

    expect(await mobileLocalPushPlugin.getPlatformVersion(), '42');
  });
}

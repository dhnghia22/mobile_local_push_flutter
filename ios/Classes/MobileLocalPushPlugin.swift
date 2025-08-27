import Flutter
import UIKit

public class MobileLocalPushPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "mobile_local_push", binaryMessenger: registrar.messenger())
    let instance = MobileLocalPushPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "getPlatformVersion":
      result("iOS " + UIDevice.current.systemVersion)
    case "requestPermission":
      UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
        result(granted)
      }
    case "getPermissionStatus":
      UNUserNotificationCenter.current().getNotificationSettings { settings in
        result(settings.authorizationStatus == .authorized)
      }
    case "schedule":
      guard let args = call.arguments as? [String: Any],
            let title = args["title"] as? String,
            let description = args["description"] as? String,
            let timestamp = args["timestamp"] as? Int else {
        result(FlutterError(code: "INVALID_ARGUMENTS", message: "Missing arguments", details: nil))
        return
      }
      let id = String(timestamp)
      let content = UNMutableNotificationContent()
      content.title = title
      content.body = description
      content.userInfo = ["id": id]
      let now = Int(Date().timeIntervalSince1970)
      let interval = max(1, Double(timestamp - now))
      let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
      let request = UNNotificationRequest(identifier: id, content: content, trigger: trigger)
      UNUserNotificationCenter.current().add(request) { error in
        if let error = error {
          result(FlutterError(code: "SCHEDULE_ERROR", message: error.localizedDescription, details: nil))
        } else {
          result(nil)
        }
      }
    case "getAllScheduledNotifications":
      UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
        let notifications = requests.map { req in
          [
            "id": req.identifier,
            "title": req.content.title,
            "description": req.content.body,
            "timestamp": (req.trigger as? UNTimeIntervalNotificationTrigger)?.nextTriggerDate()?.timeIntervalSince1970 ?? 0
          ]
        }
        result(notifications)
      }
    case "cancelNotification":
      guard let args = call.arguments as? [String: Any], let id = args["id"] as? String else {
        result(FlutterError(code: "INVALID_ARGUMENTS", message: "Missing id", details: nil))
        return
      }
      UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id])
      result(nil)
    case "cancelAllNotifications":
      UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
      result(nil)
    case "rescheduleNotifications":
      // iOS không cần reschedule vì UNUserNotificationCenter tự động persist
      // Trả về số notification đang pending
       UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
         result(requests.count)
       }
    default:
      result(FlutterMethodNotImplemented)
    }
  }
}

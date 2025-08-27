import 'package:flutter/material.dart';
import 'package:mobile_local_push/mobile_local_push.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Mobile Local Push Example',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final MobileLocalPush _push = MobileLocalPush();
  String _platformVersion = '';
  bool _permissionGranted = false;
  List<Map<String, dynamic>> _scheduled = [];

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    final version = await _push.getPlatformVersion();
    final granted = await _push.getPermissionStatus();
    setState(() {
      _platformVersion = version ?? '';
      _permissionGranted = granted;
    });
    _loadScheduled();
  }

  Future<void> _loadScheduled() async {
    final scheduled = await _push.getAllScheduledNotifications();
    setState(() {
      _scheduled = scheduled;
    });
  }

  Future<void> _requestPermission() async {
    final granted = await _push.requestPermission();
    setState(() {
      _permissionGranted = granted;
    });
  }

  Future<void> _scheduleNotification() async {
    final now = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    await _push.schedule(
      title: 'Test Notification',
      description: 'This is a test notification!',
      timestamp: now + 10, // 10 seconds later
    );
    _loadScheduled();
  }

  Future<void> _cancelAll() async {
    await _push.cancelAllNotifications();
    _loadScheduled();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mobile Local Push Example')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Platform: $_platformVersion'),
            const SizedBox(height: 8),
            Text('Permission granted: $_permissionGranted'),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: _requestPermission,
              child: const Text('Request Permission'),
            ),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: _permissionGranted ? _scheduleNotification : null,
              child: const Text('Schedule Notification (10s)'),
            ),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: _cancelAll,
              child: const Text('Cancel All Notifications'),
            ),
            const SizedBox(height: 16),
            const Text('Scheduled Notifications:'),
            Expanded(
              child: ListView.builder(
                itemCount: _scheduled.length,
                itemBuilder: (context, index) {
                  final item = _scheduled[index];
                  return ListTile(
                    title: Text(item['title'] ?? ''),
                    subtitle: Text(item['description'] ?? ''),
                    trailing: Text('ID: ${item['id']}'),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

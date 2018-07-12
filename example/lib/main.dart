import 'package:flutter/material.dart';

import 'package:flutter_music_plugin/flutter_music_plugin.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _status = "ready";
  Duration _duration = Duration(seconds: 0);
  Duration _position = Duration(seconds: 0);

  @override
  void initState() {
    super.initState();
    // connect to native channels
    FlutterMusicPlugin.listenStatus(_onPlayerStatus, _onPlayerStatusError);
    FlutterMusicPlugin.listenPosition(_onPosition, _onPlayerStatusError);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: <Widget>[
            Center(
                child: Text(_status.toUpperCase(),
                    style: TextStyle(fontSize: 32.0))),
            Center(
                child: Text(
                    _position.toString().split('.').first +
                        '/' +
                        _duration.toString().split('.').first,
                    style: TextStyle(fontSize: 24.0))),
            IconButton(
              icon: Icon(_status == "started" ? Icons.pause : Icons.play_arrow),
              onPressed: _status == "started" ||
                      _status == "paused" ||
                      _status == "completed"
                  ? _playPause
                  : null,
              iconSize: 64.0,
            ),
            RaisedButton(
              child: Center(child: Text("OPEN")),
              onPressed: _open,
            )
          ],
        ),
      ),
    );
  }

  void _playPause() {
    switch (_status) {
      case "started":
        FlutterMusicPlugin.pause();
        break;
      case "paused":
      case "completed":
        FlutterMusicPlugin.start();
        break;
    }
  }

  void _open() {
    FlutterMusicPlugin.open();
  }

  void _onPlayerStatusError(Object event) {
    print(event);
  }

  void _onPlayerStatus(Object event) {
    setState(() {
      _status = event;
    });
    if (_status == "started") {
      _getDuration();
    }
  }

  void _getDuration() async {
    Duration duration = await FlutterMusicPlugin.getDuration();
    setState(() {
      _duration = duration;
    });
  }

  void _onPosition(Object event) {
    Duration position = Duration(milliseconds: event);
    setState(() {
      _position = position;
    });
  }
}

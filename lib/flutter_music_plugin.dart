import 'dart:async';

import 'package:flutter/services.dart';

typedef void EventHandler(Object event);

class FlutterMusicPlugin {
  static const MethodChannel _channel = const MethodChannel('flutter_music_plugin');
  static const EventChannel _status_channel = const EventChannel('flutter_music_plugin.event.status');
  static const EventChannel _position_channel = const EventChannel('flutter_music_plugin.event.position');
  static const EventChannel _spectrum_channel = const EventChannel('flutter_music_plugin.event.spectrum');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<void> open() async {
    await _channel.invokeMethod('open');
  }

  static Future<void> pause() async {
    await _channel.invokeMethod('pause');
  }

  static Future<void> start() async {
    await _channel.invokeMethod('start');
  }

  static Future<Duration> getDuration() async {
    int duration = await _channel.invokeMethod('getDuration');
    return Duration(milliseconds: duration);
  }

  static listenStatus(EventHandler onEvent, EventHandler onError) {
    _status_channel.receiveBroadcastStream().listen(onEvent, onError: onError);
  }

  static listenPosition(EventHandler onEvent, EventHandler onError) {
  _position_channel.receiveBroadcastStream().listen(onEvent, onError: onError);
  }

  static listenSpectrum(EventHandler onEvent, EventHandler onError) {
  _spectrum_channel.receiveBroadcastStream().listen(onEvent, onError: onError);
  }
}

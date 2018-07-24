import 'package:flutter/material.dart';
import 'package:flutter_music_plugin/flutter_music_plugin.dart';

import 'dart:typed_data';

const COLUMNS_COUNT= 48;
const DURATION = const Duration(milliseconds: 200);

class Visualizer extends StatefulWidget {
  @override
  VisualizerState createState() => VisualizerState();
}

class VisualizerState extends State<Visualizer> with SingleTickerProviderStateMixin {

  Uint8List _spectrum;

  AnimationController _controller;

  @override
  void initState() {
    super.initState();
    // connect to native channels
    FlutterMusicPlugin.listenSpectrum(_onSpectrum, _onSpectrumError);

    _controller = AnimationController(duration: DURATION, vsync: this);
    _controller.addListener(_onTick);
  }


  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: VisualizerPainter(_spectrum)
    );
  }

  void _onSpectrumError(Object event) {
    print(event);
  }

  void _onSpectrum(Object event) {
    setState(() {
      if (_spectrum == null) {
        _spectrum = event;
      } else {
        if (event is Uint8List) {
          for (int i = 0; i < COLUMNS_COUNT; i++) {
            if (event[i] > _spectrum[i]) {
              _spectrum[i] = event[i];
            }
          }
        }
      }
    });
    _controller.forward(from: 0.0);
  }

  void _onTick() {
    setState(() {
     for (int i=0; i<COLUMNS_COUNT; i++) {
         _spectrum[i] = (_spectrum[i] - 1).clamp(0, 127);
     }

    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }
}

class VisualizerPainter extends CustomPainter {

  final Uint8List _spectrum;

  VisualizerPainter(this._spectrum);

  @override
  void paint(Canvas canvas, Size size) {

    // draw background
    var rect = Offset.zero & size;
    canvas.drawRect(
      rect,
      Paint()..color = Color(0xFF000000)
    );

    // draw spectrum
    LinearGradient gradient = LinearGradient(colors: [const Color(0xFF33FF33), const Color(0xFFFF0033)], begin: Alignment.bottomCenter, end: Alignment.topCenter);
    double columnWidth = size.width / COLUMNS_COUNT;
    double step = size.height / 127;

    for (int i=0; i<COLUMNS_COUNT; i++) {
      double volume = 2.0;
      if (_spectrum != null && i < _spectrum.length) {
        volume = _spectrum[i] * step + 2;
      }
      Rect column = Rect.fromLTRB(columnWidth*i, size.height-volume, columnWidth*i+columnWidth - 1, size.height);
      canvas.drawRect(
          column,
          //Paint()..color = Color(0xFFFFB90F)
          Paint()..shader = gradient.createShader(column)
      );
    }

  }

  @override
  bool shouldRepaint(VisualizerPainter oldDelegate) => true;//oldDelegate._spectrum != _spectrum;
}

import 'package:flutter/material.dart';
import 'package:flutter_music_plugin/flutter_music_plugin.dart';

import 'dart:typed_data';

const COLUMNS_COUNT= 64;

class Visualizer extends StatefulWidget {
  @override
  VisualizerState createState() => VisualizerState();
}

class VisualizerState extends State<Visualizer> {

  Uint8List _spectrum;

  @override
  void initState() {
    super.initState();
    // connect to native channels
    FlutterMusicPlugin.listenSpectrum(_onSpectrum, _onSpectrumError);
  }


  @override
  Widget build(BuildContext context) {
    return new CustomPaint(
      painter: VisualizerPainter(_spectrum)
    );
  }

  void _onSpectrumError(Object event) {
    print(event);
  }

  void _onSpectrum(Object event) {
    setState(() {
      _spectrum = event;
    });
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
  bool shouldRepaint(VisualizerPainter oldDelegate) => oldDelegate._spectrum != _spectrum;
}

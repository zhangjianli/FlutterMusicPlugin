package io.github.zhangjianli.fluttermusicplugin;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterNativeView;

import static android.app.Activity.RESULT_OK;

/**
 * FlutterMusicPlugin
 */
public class FlutterMusicPlugin implements MethodCallHandler, PluginRegistry.ViewDestroyListener, PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener {

    private static final String TAG = FlutterMusicPlugin.class.getSimpleName();

    private static final String METHOD_CHANNEL_NAME = "flutter_music_plugin";
    private static final String PLAYER_EVENT_STATUS_CHANNEL_NAME = "flutter_music_plugin.event.status";
    private static final String PLAYER_EVENT_POSITION_CHANNEL_NAME = "flutter_music_plugin.event.position";
    private static final String PLAYER_EVENT_SPECTRUM_CHANNEL_NAME = "flutter_music_plugin.event.spectrum";
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1001;
    private static final int PERMISSIONS_REQUEST_READ_STORAGE = 1002;

    private static final int REQUEST_CODE_OPEN = 12345;

    private Handler mHandler = new Handler();

    private Runnable mPositionReporter = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mPositionSink.success(mMediaPlayer.getCurrentPosition());
                mHandler.postDelayed(mPositionReporter, 500);
            }
        }
    };

    private MediaPlayer mMediaPlayer;
    private Visualizer mVisualizer;

    private Activity mActivity;

    private EventChannel.EventSink mStateSink;
    private EventChannel.EventSink mPositionSink;
    private EventChannel.EventSink mSpectrumSink;

    private FlutterMusicPlugin(Activity activity) {
        mActivity = activity;
        if (mActivity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            mActivity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final FlutterMusicPlugin plugin = new FlutterMusicPlugin(registrar.activity());
        registrar.addViewDestroyListener(plugin);
        registrar.addActivityResultListener(plugin);
        registrar.addRequestPermissionsResultListener(plugin);
        final MethodChannel channel = new MethodChannel(registrar.messenger(), METHOD_CHANNEL_NAME);
        channel.setMethodCallHandler(plugin);
        EventChannel status_channel = new EventChannel(registrar.messenger(), PLAYER_EVENT_STATUS_CHANNEL_NAME);
        status_channel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, EventChannel.EventSink eventSink) {
                plugin.setStateSink(eventSink);
            }

            @Override
            public void onCancel(Object o) {

            }
        });
        EventChannel position_channel = new EventChannel(registrar.messenger(), PLAYER_EVENT_POSITION_CHANNEL_NAME);
        position_channel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, EventChannel.EventSink eventSink) {
                plugin.setPositionSink(eventSink);
            }

            @Override
            public void onCancel(Object o) {

            }
        });
        EventChannel spectrum_channel = new EventChannel(registrar.messenger(), PLAYER_EVENT_SPECTRUM_CHANNEL_NAME);
        spectrum_channel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, EventChannel.EventSink eventSink) {
                plugin.setSpectrumSink(eventSink);
            }

            @Override
            public void onCancel(Object o) {

            }
        });

    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "pause":
                mMediaPlayer.pause();
                mVisualizer.setEnabled(false);
                mHandler.removeCallbacks(mPositionReporter);
                mStateSink.success("paused");
                break;
            case "start":
                mMediaPlayer.start();
                mVisualizer.setEnabled(true);
                mStateSink.success("started");
                mHandler.postDelayed(mPositionReporter, 500);
                break;
            case "open":
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                mActivity.startActivityForResult(intent, REQUEST_CODE_OPEN);
                break;
            case "getDuration":
                if (mMediaPlayer != null) {
                    result.success(mMediaPlayer.getDuration());
                } else {
                    result.error("ERROR", "no valid media player", null);
                }
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public boolean onViewDestroy(FlutterNativeView flutterNativeView) {
        stop();
        return false;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_OPEN && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                Log.d(TAG, "opening " + uri);
                play(uri);
            } else {
                mStateSink.error("ERROR", "invalid media file", null);
            }
            return true;
        }
        return false;
    }

    private void stop() {
        if (mMediaPlayer != null) {
            if (mVisualizer != null) {
                mVisualizer.release();
            }
            mHandler.removeCallbacks(mPositionReporter);
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mStateSink.success("stopped");
        }
    }

    private void play(Uri uri) {
        stop();
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mVisualizer.setEnabled(false);
                    mStateSink.success("completed");
                    mHandler.removeCallbacks(mPositionReporter);
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mStateSink.error("ERROR", "MediaPlayer error: " + what, null);
                    return true;
                }
            });
            try {
                mMediaPlayer.setDataSource(mActivity, uri);
                mMediaPlayer.prepare();
                mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
                mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[0]);
                mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                    public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {}

                    public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                        //Log.d(TAG,"captrued length "+bytes.length);
                        byte[] spectrum = new byte[bytes.length / 2];
                        for (int i = 0; i < spectrum.length; i++) {
                            Double magnitude = Math.hypot(bytes[2*i], bytes[2*i+1]);
                            if (magnitude < 0) {
                                spectrum[i] = 0;
                            } else if (magnitude > 127) {
                                spectrum[i] = 127 & 0xFF;
                            } else {
                                spectrum[i] = magnitude.byteValue();
                            }
                        }
                        mSpectrumSink.success(spectrum);
                        //Log.d(TAG,"captrued: "+ spectrum[0]+" "+spectrum[1]+" "+spectrum[2]);
                    }
                }, Visualizer.getMaxCaptureRate()/2, false, true);
                mVisualizer.setEnabled(true);
                mMediaPlayer.start();
                mStateSink.success("started");
                mHandler.postDelayed(mPositionReporter, 500);
            } catch (Exception e) {
                e.printStackTrace();
                mStateSink.error("ERROR", e.getMessage(), null);
            }
        }

    }

    public void setStateSink(EventChannel.EventSink stateSink) {
        mStateSink = stateSink;
    }

    public void setPositionSink(EventChannel.EventSink positionSink) {
        mPositionSink = positionSink;
    }

    public void setSpectrumSink(EventChannel.EventSink spectrumSink) {
        mSpectrumSink = spectrumSink;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_RECORD_AUDIO :
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        // Permission is not granted
                        mActivity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_STORAGE);
                    }
                    return true;
                } else {
                    mActivity.finish();
                    return false;
                }
            case PERMISSIONS_REQUEST_READ_STORAGE :
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return true;
                } else {
                    mActivity.finish();
                    return false;
                }
            default:
                return  false;
        }
    }
}

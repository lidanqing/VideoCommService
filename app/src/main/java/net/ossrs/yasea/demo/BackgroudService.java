package net.ossrs.yasea.demo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;

import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublisher;
import net.ossrs.yasea.SrsRecordHandler;

import java.io.IOException;
import java.net.SocketException;
import java.util.Random;

/**
 * Created by li on 2017/8/24.
 */

public class BackgroudService extends Service implements RtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {
    private String TAG = "Service";
    private WindowManager mWindowManager;
    private SrsCameraView bgSurfaceView;
    private SrsPublisher mPublisher;
    private String rtmpUrl;
    //private IjkMediaPlayer player;

    private PLMediaPlayer mMediaPlayer;
    private String mAudioPath;
    private AVOptions mAVOptions;

    @Override
    public void onCreate() {
        super.onCreate();
        /*player = new IjkMediaPlayer();

        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 2048);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1000);      //播放前的探测时间
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);        //  关闭播放器缓冲
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "nobuffer");
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "max_delay", "0");
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reorder_queue_size", "0");
        //player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "avioflags", "direct");
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtmp_transport", "0");
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_frame", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "sync", "ext");*/
        mAVOptions = new AVOptions();
        // the unit of timeout is ms
        mAVOptions.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        // 默认的缓存大小，单位是 ms
        mAVOptions.setInteger(AVOptions.KEY_CACHE_BUFFER_DURATION, 300);
        // 最大的缓存大小，单位是 ms
        mAVOptions.setInteger(AVOptions.KEY_MAX_CACHE_BUFFER_DURATION, 300);
        mAVOptions.setInteger(AVOptions.KEY_MEDIACODEC,AVOptions.MEDIA_CODEC_HW_DECODE);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);


        Log.i(TAG, "onCreate..");
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);
        Log.i(TAG, "onStart..");


        mWindowManager = (WindowManager) getSystemService(Service.WINDOW_SERVICE);
        bgSurfaceView = new SrsCameraView(this);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1, WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
                layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
                mWindowManager.addView(bgSurfaceView, layoutParams);

        mPublisher = new SrsPublisher(bgSurfaceView);
        mPublisher.setEncodeHandler(new SrsEncodeHandler(this));
        mPublisher.setRtmpHandler(new RtmpHandler(this));
        mPublisher.setRecordHandler(new SrsRecordHandler(this));
        //mPublisher.setPreviewResolution(640, 360);
        //mPublisher.setOutputResolution(360, 640);
        mPublisher.setPreviewResolution(320, 240);
        mPublisher.setOutputResolution(240, 320);
        mPublisher.setVideoSmoothMode();
        mPublisher.switchToSoftEncoder();    //
        mPublisher.startCamera();

        /*rtmpUrl = "rtmp://118.178.122.224:1935/live/livestream";
        mPublisher.startPublish(rtmpUrl);
        mPublisher.startCamera();*/

        new Thread(new Runnable() {
            @Override
            public void run() {
                prepare();
                rtmpUrl = "rtmp://118.178.122.224:1935/live/livestream";
                mPublisher.startPublish(rtmpUrl);
                mPublisher.startCamera();
            }
        }).start();

        /*try {
            player.setDataSource("rtmp://118.178.122.224:1935/live/livestream/vczz");
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.prepareAsync();
        player.start();*/

    }

    /**
     * PLDroidPlayer
     */
    private void prepare() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new PLMediaPlayer(getApplicationContext(), mAVOptions);
            mMediaPlayer.setDebugLoggingEnabled(true);
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }
        try {
            mAudioPath = "rtmp://118.178.122.224:1935/live/livestream/li";
            mMediaPlayer.setDataSource(mAudioPath);
            mMediaPlayer.setOnPreparedListener(new PLMediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(PLMediaPlayer plMediaPlayer, int i) {
                    mMediaPlayer.start();
                }
            });
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub

        super.onDestroy();
        mPublisher.stopPublish();
        mPublisher.stopRecord();
        release();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(null);
        Log.e(TAG, "Service stopped..");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind..");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind..");
        return super.onUnbind(intent);
    }


    /**
     * push stream overide methods
     */

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPublisher.stopEncode();
        mPublisher.stopRecord();
        mPublisher.setScreenOrientation(newConfig.orientation);
            mPublisher.startEncode();
        mPublisher.startCamera();
    }

    private static String getRandomAlphaString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    private static String getRandomAlphaDigitString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    private void handleException(Exception e) {
        try {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            mPublisher.stopPublish();
            mPublisher.stopRecord();
        } catch (Exception e1) {
            // Ignore
        }
    }

    // Implementation of SrsRtmpListener.

    @Override
    public void onRtmpConnecting(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpConnected(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoStreaming() {
    }

    @Override
    public void onRtmpAudioStreaming() {
    }

    @Override
    public void onRtmpStopped() {
        Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpDisconnected() {
        Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {
        Log.i(TAG, String.format("Output Fps: %f", fps));
    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Video bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Video bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Audio bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Audio bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpSocketException(SocketException e) {
        if (isNetworkAvailable())
        {
            Toast.makeText(getApplicationContext(), "当前有可用网络！", Toast.LENGTH_LONG).show();
            /*mPublisher = new SrsPublisher(bgSurfaceView);
            mPublisher.setEncodeHandler(new SrsEncodeHandler(this));        //编码状态回调
            mPublisher.setRtmpHandler(new RtmpHandler(this));               //rtmp推流状态回调
            mPublisher.setRecordHandler(new SrsRecordHandler(this));
            mPublisher.setPreviewResolution(320, 240);
            mPublisher.setOutputResolution(240, 320);
            mPublisher.setVideoSmoothMode();
            mPublisher.startCamera();*/
            handleException(e);
            mPublisher.setEncodeHandler(new SrsEncodeHandler(this));
            mPublisher.setRtmpHandler(new RtmpHandler(this));
            mPublisher.setRecordHandler(new SrsRecordHandler(this));
            //mPublisher.setPreviewResolution(640, 360);
            //mPublisher.setOutputResolution(360, 640);
            mPublisher.setPreviewResolution(320, 240);
            mPublisher.setOutputResolution(240, 320);
            mPublisher.setVideoSmoothMode();
            mPublisher.switchToSoftEncoder();    //
            mPublisher.startCamera();
            rtmpUrl = "rtmp://118.178.122.224:1935/live/livestream";
            //rtmpUrl = "rtmp://ossrs.net/live/vczz";
            mPublisher.startPublish(rtmpUrl);
            mPublisher.startCamera();
            Log.e(TAG,"youwang............................");
        }
        else
        {
            handleException(e);
            Toast.makeText(getApplicationContext(), "当前没有可用网络！", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalStateException(IllegalStateException e) {
        handleException(e);
    }

    // Implementation of SrsRecordHandler.

    @Override
    public void onRecordPause() {
        Toast.makeText(getApplicationContext(), "Record paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordResume() {
        Toast.makeText(getApplicationContext(), "Record resumed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordStarted(String msg) {
        Toast.makeText(getApplicationContext(), "Recording file: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordFinished(String msg) {
        Toast.makeText(getApplicationContext(), "MP4 file saved: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    // Implementation of SrsEncodeHandler.

    @Override
    public void onNetworkWeak() {
        Toast.makeText(getApplicationContext(), "Network weak", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNetworkResume() {
        Toast.makeText(getApplicationContext(), "Network resume", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    /**
     * 检查当前网络是否可用
     *
     */
    public boolean isNetworkAvailable()
    {
        Context context = getApplicationContext();
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null)
        {
            return false;
        }
        else
        {
            // 获取NetworkInfo对象
            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();

            if (networkInfo != null && networkInfo.length > 0)
            {
                for (int i = 0; i < networkInfo.length; i++)
                {
                    Log.e(TAG,i + "===状态===" + networkInfo[i].getState());
                    Log.e(TAG,i + "===类型===" + networkInfo[i].getTypeName());
                    // 判断当前网络状态是否为连接状态
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}

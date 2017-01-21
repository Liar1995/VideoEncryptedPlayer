package com.cocomeng.videoencryptedplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cocomeng.videoencryptedplayer.httpserver.HttpConnection;
import com.cocomeng.videoencryptedplayer.httpserver.HttpServer;
import com.cocomeng.videoencryptedplayer.httpserver.IHttpStream;
import com.cocomeng.videoencryptedplayer.widget.FullScreenVideoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Sunmeng on 1/3/2017.
 * E-Mail:Sunmeng1995@outlook.com
 * Description:加密视频播放器
 */

public class EncryptedVideo extends AppCompatActivity implements View.OnClickListener{

    private HttpServer httpServer;
    private static final int MESSAGE_HIDE_CENTER_BOX = 4;
    private static final int MESSAGE_SEEK_NEW_POSITION = 3;
    private static final int HIDE_TIME = 5000;
    private FullScreenVideoView mVideo;
    private View mTopView;
    private View mBottomView;
    private SeekBar mSeekBar;
    private ImageView mPlay;
    private TextView mPlayTime;
    private TextView mDurationTime;
    private AudioManager mAudioManager;
    private LinearLayout app_video_volume_box, app_video_brightness_box, app_video_fastForward_box;
    private ImageView app_video_volume_icon, app_video_fullscreen;
    private TextView app_video_volume, app_video_brightness, app_video_fastForward, app_video_fastForward_target, app_video_fastForward_all;
    private int mMaxVolume;
    private int screenWidthPixels;
    private int volume = -1;
    private long newPosition = -1;
    private float brightness = -1;
    private String filePath;

    /****
     * @param filePath  视频key或者路径
     ***/
    public static void inVideoActivity(Activity context, String filePath) {
        Intent intent = new Intent(context, EncryptedVideo.class);
        intent.putExtra("key", filePath);
        context.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_encrypted);
        mVideo = (FullScreenVideoView) findViewById(R.id.videoview);
        mPlayTime = (TextView) findViewById(R.id.play_time);
        mDurationTime = (TextView) findViewById(R.id.total_time);
        mPlay = (ImageView) findViewById(R.id.play_btn);
        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mTopView = findViewById(R.id.app_video_top_box);
        mBottomView = findViewById(R.id.app_video_bottom_box);
        app_video_volume_box = (LinearLayout) findViewById(R.id.app_video_volume_box);
        app_video_brightness_box = (LinearLayout) findViewById(R.id.app_video_brightness_box);
        app_video_fastForward_box = (LinearLayout) findViewById(R.id.app_video_fastForward_box);
        app_video_fullscreen = (ImageView) findViewById(R.id.app_video_fullscreen);
        app_video_volume_icon = (ImageView) findViewById(R.id.app_video_volume_icon);
        app_video_volume = (TextView) findViewById(R.id.app_video_volume);
        app_video_brightness = (TextView) findViewById(R.id.app_video_brightness);
        app_video_fastForward = (TextView) findViewById(R.id.app_video_fastForward);
        app_video_fastForward_target = (TextView) findViewById(R.id.app_video_fastForward_target);
        app_video_fastForward_all = (TextView) findViewById(R.id.app_video_fastForward_all);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mPlay.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        screenWidthPixels = getResources().getDisplayMetrics().widthPixels;
        app_video_fullscreen.setOnClickListener(this);
        filePath=getIntent().getStringExtra("key");
        initServer("/sdcard/DCIM/my_video.mp4");
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mHandler.postDelayed(hideRunnable, HIDE_TIME);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mHandler.removeCallbacks(hideRunnable);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                int time = progress * mVideo.getDuration() / 100;
                mVideo.seekTo(time);
            }
        }
    };

    private void onVolumeSlide(float percent) {
        if (volume == -1) {
            volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volume < 0)
                volume = 0;
        }
        int index = (int) (percent * mMaxVolume) + volume;
        if (index > mMaxVolume)
            index = mMaxVolume;
        else if (index < 0)
            index = 0;
        // 变更声音
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
        // 变更进度条
        int i = (int) (index * 1.0 / mMaxVolume * 100);
        String s = i + "%";
        if (i == 0) {
            s = "off";
        }
        // 显示
        if (i == 0) app_video_volume_icon.setImageResource(R.drawable.ic_volume_off_white_36dp);
        else app_video_volume_icon.setImageResource(R.drawable.ic_volume_up_white_36dp);
        app_video_brightness_box.setVisibility(View.GONE);
        app_video_volume_box.setVisibility(View.VISIBLE);
        app_video_volume.setText(s);
    }

    private void playVideo(Uri uri) {
        File file = new File(uri.getPath());
        if (!file.exists()) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_LONG).show();
            return;
        }
        mVideo.setVideoURI(uri);
        mPlay.setImageResource(R.drawable.ic_stop_white_24dp);
        mVideo.requestFocus();
        mVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mVideo.setVideoWidth(mp.getVideoWidth());
                mVideo.setVideoHeight(mp.getVideoHeight());
                mVideo.start();
                mHandler.removeCallbacks(hideRunnable);
                mHandler.postDelayed(hideRunnable, HIDE_TIME);
                mDurationTime.setText(formatTime(mVideo.getDuration()));
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(1);
                    }
                }, 0, 1000);
            }
        });
        mVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlay.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                mPlayTime.setText("00:00");
                mSeekBar.setProgress(0);
            }
        });
        final GestureDetector gestureDetector = new GestureDetector(this, new PlayerGestureListener());
        View liveBox = findViewById(R.id.app_video_box);
        liveBox.setClickable(true);
        liveBox.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (gestureDetector.onTouchEvent(motionEvent))
                    return true;
                // 处理手势结束
                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP:
                        endGesture();
                        break;
                }
                return false;
            }
        });
    }

    private void onBrightnessSlide(float percent) {
        if (brightness < 0) {
            brightness = getWindow().getAttributes().screenBrightness;
            if (brightness <= 0.00f) {
                brightness = 0.50f;
            } else if (brightness < 0.01f) {
                brightness = 0.01f;
            }
        }
        Log.d(this.getClass().getSimpleName(), "brightness:" + brightness + ",percent:" + percent);
        app_video_brightness_box.setVisibility(View.VISIBLE);
        WindowManager.LayoutParams lpa = getWindow().getAttributes();
        lpa.screenBrightness = brightness + percent;
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f;
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f;
        }
        Log.i("Sunmeng", "lpa.screenBrightness : " + lpa.screenBrightness * 100);
        app_video_brightness.setText(((int) (lpa.screenBrightness * 100)) + "%");
        getWindow().setAttributes(lpa);
    }

    private class PlayerGestureListener extends GestureDetector.SimpleOnGestureListener {
        private boolean firstTouch;
        private boolean volumeControl;
        private boolean toSeek;

        /**
         * 双击
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            firstTouch = true;
            return super.onDown(e);
        }

        /**
         * 滑动
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float mOldX = e1.getX(), mOldY = e1.getY();
            float deltaY = mOldY - e2.getY();
            float deltaX = mOldX - e2.getX();
            if (firstTouch) {
                toSeek = Math.abs(distanceX) >= Math.abs(distanceY);
                volumeControl = mOldX > screenWidthPixels * 0.5f;
                firstTouch = false;
            }
            if (toSeek) {
                onProgressSlide(-deltaX / mVideo.getWidth());
            } else {
                float percent = deltaY / mVideo.getHeight();
                if (volumeControl) {
                    onVolumeSlide(percent);
                } else {
                    onBrightnessSlide(percent);
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            showOrHide();
            return true;
        }
    }

    private void endGesture() {
        volume = -1;
        brightness = -1f;
        if (newPosition >= 0) {
            mHandler.removeMessages(MESSAGE_SEEK_NEW_POSITION);
            mHandler.sendEmptyMessage(MESSAGE_SEEK_NEW_POSITION);
        }
        mHandler.removeMessages(MESSAGE_HIDE_CENTER_BOX);
        mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_CENTER_BOX, 500);
    }

    private void onProgressSlide(float percent) {
        long position = mVideo.getCurrentPosition();
        long duration = mVideo.getDuration();
        long deltaMax = Math.min(100 * 1000, duration - position);
        long delta = (long) (deltaMax * percent);
        newPosition = delta + position;
        if (newPosition > duration) {
            newPosition = duration;
        } else if (newPosition <= 0) {
            newPosition = 0;
            delta = -position;
        }
        int showDelta = (int) delta / 1000;
        if (showDelta != 0) {
            app_video_fastForward_box.setVisibility(View.VISIBLE);
            String text = showDelta > 0 ? ("+" + showDelta) : "" + showDelta;
            app_video_fastForward.setText(text + "s");
            app_video_fastForward_target.setText(generateTime(newPosition) + "/");
            app_video_fastForward_all.setText(generateTime(duration));
        }
    }

    private String generateTime(long time) {
        int totalSeconds = (int) (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
    }

    private Runnable hideRunnable = new Runnable() {

        @Override
        public void run() {
            showOrHide();
        }
    };

    @SuppressLint("SimpleDateFormat")
    private String formatTime(long time) {
        DateFormat formatter = new SimpleDateFormat("mm:ss");
        return formatter.format(new Date(time));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_btn:
                if (mVideo.isPlaying()) {
                    mVideo.pause();
                    mPlay.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                } else {
                    mVideo.start();
                    mPlay.setImageResource(R.drawable.ic_stop_white_24dp);
                }
                break;
            case R.id.app_video_fullscreen:
                toggleFullScreen();
                break;
            default:
                break;
        }
    }

    private void showOrHide() {
        if (mTopView.getVisibility() == View.VISIBLE) {
            mTopView.clearAnimation();
            Animation animation = AnimationUtils.loadAnimation(this,
                    R.anim.option_leave_from_top);
            animation.setAnimationListener(new AnimationImp() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    super.onAnimationEnd(animation);
                    mTopView.setVisibility(View.GONE);
                }
            });
            mTopView.startAnimation(animation);

            mBottomView.clearAnimation();
            Animation animation1 = AnimationUtils.loadAnimation(this,
                    R.anim.option_leave_from_bottom);
            animation1.setAnimationListener(new AnimationImp() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    super.onAnimationEnd(animation);
                    mBottomView.setVisibility(View.GONE);
                }
            });
            mBottomView.startAnimation(animation1);
        } else {
            mTopView.setVisibility(View.VISIBLE);
            mTopView.clearAnimation();
            Animation animation = AnimationUtils.loadAnimation(this,
                    R.anim.option_entry_from_top);
            mTopView.startAnimation(animation);

            mBottomView.setVisibility(View.VISIBLE);
            mBottomView.clearAnimation();
            Animation animation1 = AnimationUtils.loadAnimation(this,
                    R.anim.option_entry_from_bottom);
            mBottomView.startAnimation(animation1);
            mHandler.removeCallbacks(hideRunnable);
            mHandler.postDelayed(hideRunnable, HIDE_TIME);
        }
    }

    private class AnimationImp implements Animation.AnimationListener {

        @Override
        public void onAnimationEnd(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    }

    private int getScreenOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

    private void toggleFullScreen() {
        if (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        updateFullScreenButton();
    }

    private void updateFullScreenButton() {
        if (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            app_video_fullscreen.setImageResource(R.drawable.ic_fullscreen_exit_white_36dp);
        } else {
            app_video_fullscreen.setImageResource(R.drawable.ic_fullscreen_white_24dp);
        }
    }

    void initServer(final String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_LONG).show();
            return;
        }
        httpServer = HttpServer.getInstance();
        httpServer.start(new IHttpStream() {
            @Override
            public boolean writeStream(OutputStream out, String path, int rangS, int rangE) throws IOException {
                int streamLen;
                int readLen;
                int leftLen;
                Uri uri = Uri.parse(path);
                String pathString = uri.getQueryParameter("path");
                if (pathString == null || pathString.equals("")) {
                    HttpConnection.send404Response(out, path);
                } else {
                    String type = HttpConnection.getContentType(pathString);
                    byte[] buffer = new byte[1024 * 10];
                    InputStream mMediaInputStream = new FileInputStream(filePath);
                    if (isEncrypted(filePath, "547fedc3a4bff6c8758987daa2a1cb84")) {
                        mMediaInputStream.skip(32);
                    }
                    streamLen = mMediaInputStream.available();
                    if (rangS >= 0) {
                        mMediaInputStream.skip(rangS);
                        rangE = rangE > streamLen ? streamLen : rangE;
                        HttpConnection.sendOkResponse(out, rangE - rangS, type, rangS, rangE, mMediaInputStream.available());
                        leftLen = rangE - rangS;
                        while (leftLen > 0) {
                            readLen = mMediaInputStream.read(buffer);
                            out.write(buffer, 0, readLen);
                            leftLen -= readLen;
                        }
                        out.flush();
                        out.close();
                    } else {
                        HttpConnection.sendOkResponse(out, mMediaInputStream.available(), type);
                        while (true) {
                            readLen = mMediaInputStream.read(buffer);
                            if (readLen <= 0) break;
                            out.write(buffer, 0, readLen);
                        }
                        out.flush();
                        out.close();
                    }
                }
                return false;
            }

            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public boolean acceptRange() {
                return true;
            }
        }, 8080);
        Uri uri = Uri.parse(httpServer.getHttpAddr() + "/?path=" + URLEncoder.encode(filePath));
        Log.i("Sunmeng",uri.getPath());
        playVideo(uri);
    }

    private boolean isEncrypted(String filePath, String key) {
        try {
            InputStream encrypted = new FileInputStream(filePath);
            byte[] b = new byte[32];
            encrypted.read(b);
            if (!key.equals(new String(b, "UTF-8"))) {
                encrypted.close();
                return false;
            } else if (key.equals(new String(b, "UTF-8"))) {
                encrypted.close();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(0);
        mHandler.removeCallbacksAndMessages(null);
        if (httpServer != null)
            httpServer.stop();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if (mVideo.getCurrentPosition() > 0) {
                        mPlayTime.setText(formatTime(mVideo.getCurrentPosition()));
                        int progress = mVideo.getCurrentPosition() * 100 / mVideo.getDuration();
                        mSeekBar.setProgress(progress);
                        if (mVideo.getCurrentPosition() > mVideo.getDuration() - 100) {
                            mPlayTime.setText("00:00");
                            mSeekBar.setProgress(0);
                        }
                        mSeekBar.setSecondaryProgress(mVideo.getBufferPercentage());
                    } else {
                        mPlayTime.setText("00:00");
                        mSeekBar.setProgress(0);
                    }

                    break;
                case 2:
                    showOrHide();
                    break;
                case MESSAGE_HIDE_CENTER_BOX:
                    //隐藏调节操作悬浮框
                    app_video_volume_box.setVisibility(View.GONE);
                    app_video_brightness_box.setVisibility(View.GONE);
                    app_video_fastForward_box.setVisibility(View.GONE);
                    break;
                case MESSAGE_SEEK_NEW_POSITION:
                    if (newPosition >= 0) {
                        mVideo.seekTo((int) newPosition);
                        newPosition = -1;
                    }
                    break;
                default:
                    break;
            }
        }
    };
}

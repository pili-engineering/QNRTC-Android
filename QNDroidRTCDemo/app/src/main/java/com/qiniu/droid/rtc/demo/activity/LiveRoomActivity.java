package com.qiniu.droid.rtc.demo.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.pili.pldroid.player.PLOnErrorListener;
import com.pili.pldroid.player.PLOnInfoListener;
import com.pili.pldroid.player.widget.PLVideoView;
import com.qiniu.droid.rtc.QNLocalSurfaceView;
import com.qiniu.droid.rtc.QNRTCManager;
import com.qiniu.droid.rtc.QNRTCSetting;
import com.qiniu.droid.rtc.QNRemoteSurfaceView;
import com.qiniu.droid.rtc.QNRoomEventListener;
import com.qiniu.droid.rtc.QNRoomState;
import com.qiniu.droid.rtc.QNStatisticsReport;
import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.ui.CircleTextView;
import com.qiniu.droid.rtc.demo.utils.QNAppServer;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.util.ArrayList;
import java.util.List;

public class LiveRoomActivity extends Activity implements QNRoomEventListener {
    private static final String TAG = "LiveRoomActivity";
    public static final String EXTRA_ROOM_ID = "ROOM_ID";
    public static final String EXTRA_ROOM_TOKEN = "ROOM_TOKEN";
    public static final String EXTRA_USER_ID = "USER_ID";
    private static final String BASE_URL = "rtmp://pili-rtmp.qnsdk.com/sdk-live/";

    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.CAMERA"
    };

    private PLVideoView mVideoView;
    private LinearLayout mLogView;
    private TextView mAudioBitrateText;
    private TextView mAudioFpsText;
    private TextView mVideoBitrateText;
    private TextView mVideoFpsText;
    private TextView mRemoteTextView;
    private Chronometer mTimer;
    private PopupWindow mPopWindow;
    private Toast mLogToast;

    private QNRTCManager mRTCManager;
    private StringBuffer mRemoteLogText;
    private List<String> mUserList;
    private String mToken;
    private UserListAdapter mUserListAdapter;
    private String mChooseUserId;
    private String[] mMergeStreamPosition;
    private boolean mIsJoinedRoom = false;
    private String mUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        setContentView(R.layout.activity_live_room);

        Intent intent = getIntent();
        String mRtmpUrl = BASE_URL + intent.getStringExtra(EXTRA_ROOM_ID);
        mToken = intent.getStringExtra(EXTRA_ROOM_TOKEN);
        mUserName = intent.getStringExtra(EXTRA_USER_ID);

        mTimer = (Chronometer) findViewById(R.id.timer);
        mLogView = (LinearLayout) findViewById(R.id.log_text);
        ImageButton mLogButton = (ImageButton) findViewById(R.id.log_shown_button);
        mLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogView.setVisibility(mLogView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        mAudioBitrateText = (TextView) findViewById(R.id.audio_bitrate_log_text);
        mAudioFpsText = (TextView) findViewById(R.id.audio_fps_log_text);
        mVideoBitrateText = (TextView) findViewById(R.id.video_bitrate_log_text);
        mVideoFpsText = (TextView) findViewById(R.id.video_fps_log_text);
        mRemoteTextView = (TextView) findViewById(R.id.remote_log_text);
        mRemoteTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mVideoView = (PLVideoView) findViewById(R.id.PLVideoView);

        mUserList = new ArrayList<>();
        mMergeStreamPosition = new String[9];

        //权限校验
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        //初始化RTC
        QNRTCSetting setting = new QNRTCSetting();
        setting.setVideoEnabled(false);
        setting.setAudioEnabled(false);
        mRTCManager = new QNRTCManager();
        mRTCManager.setRoomEventListener(this);
        mRTCManager.initialize(LiveRoomActivity.this, setting);

        //播放器相关
        mVideoView.setVideoPath(mRtmpUrl);
        mVideoView.setOnErrorListener(new PLOnErrorListener() {
            @Override
            public boolean onError(int errorCode) {
                switch (errorCode) {
                    case ERROR_CODE_OPEN_FAILED:
                        logAndToast("播放器打开失败");
                        break;
                    case ERROR_CODE_IO_ERROR:
                        logAndToast("网络异常");
                        break;
                    default:
                        logAndToast("PlayerError Code: " + errorCode);
                        break;
                }
                return false;
            }
        });
        mVideoView.setOnInfoListener(new PLOnInfoListener() {
            @Override
            public void onInfo(int what, int extra) {
                switch (what) {
                    case MEDIA_INFO_VIDEO_RENDERING_START:
                        mTimer.setBase(SystemClock.elapsedRealtime());
                        mTimer.start();
                        break;
                    case MEDIA_INFO_VIDEO_BITRATE:
                        mVideoBitrateText.setText("VideoBitrate: " + extra / 1000 + " kb/s");
                        break;
                    case MEDIA_INFO_VIDEO_FPS:
                        mVideoFpsText.setText("VideoFps: " + extra);
                        break;
                    case MEDIA_INFO_AUDIO_BITRATE:
                        mAudioBitrateText.setText("AudioBitrate: " + extra / 1000 + " kb/s");
                        break;
                    case MEDIA_INFO_AUDIO_FPS:
                        mAudioFpsText.setText("AudioFps: " + extra);
                        break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCall();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.stopPlayback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPopWindow != null && mPopWindow.isShowing()) {
            mPopWindow.dismiss();
        }
        mPopWindow = null;
    }

    @Override
    public void onBackPressed() {
        disconnect();
        super.onBackPressed();
    }

    private void startCall() {
        if (mRTCManager == null) {
            return;
        }
        logAndToast("正在连接");
        mRTCManager.joinRoom(mToken);
    }

    private void disconnect() {
        if (mLogToast != null) {
            mLogToast.cancel();
        }
        if (mRTCManager != null) {
            if (isAdmin(mUserName)) {
                mRTCManager.stopMergeStream();
            }
            mRTCManager.destroy();
        }
        mIsJoinedRoom = false;
    }

    private void logAndToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, msg);
                if (mLogToast != null) {
                    mLogToast.cancel();
                }
                mLogToast = Toast.makeText(LiveRoomActivity.this, msg, Toast.LENGTH_SHORT);
                mLogToast.show();
            }
        });
    }

    public void onClickConfig(View view) {
        if (mIsJoinedRoom && mUserList.size() != 0) {
            if (!isAdmin(mUserName)) {
                ToastUtils.s(LiveRoomActivity.this, "您不是admin用户，无法进行合流操作");
            } else {
                mChooseUserId = mUserList.get(0);
                //配置页
                View content = LayoutInflater.from(getApplicationContext()).inflate(R.layout.layout_config_view, null, true);
                RecyclerView mUserListView = (RecyclerView) content.findViewById(R.id.user_list_view);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
                linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
                mUserListView.setLayoutManager(linearLayoutManager);
                mUserListAdapter = new UserListAdapter();
                mUserListView.setAdapter(mUserListAdapter);
                Button mBtnConfirm = (Button) content.findViewById(R.id.btn_confirm);
                final EditText editTextX = (EditText) content.findViewById(R.id.x_edit_text);
                final EditText editTextY = (EditText) content.findViewById(R.id.y_edit_text);
                final EditText editTextZ = (EditText) content.findViewById(R.id.z_edit_text);
                final EditText editTextWidth = (EditText) content.findViewById(R.id.width_edit_text);
                final EditText editTextHeight = (EditText) content.findViewById(R.id.height_edit_text);
                final Switch videoSwitch = (Switch) content.findViewById(R.id.video_switch);
                final Switch audioSwitch = (Switch) content.findViewById(R.id.audio_switch);
                final boolean[] isHidden = {false};
                final boolean[] isMuted = {false};
                mPopWindow = new PopupWindow(content,
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                mPopWindow.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.popupWindowBackground)));

                videoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        isHidden[0] = !isChecked;
                    }
                });

                audioSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        isMuted[0] = !isChecked;
                    }
                });

                mBtnConfirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            int x = Integer.parseInt(editTextX.getText().toString());
                            int y = Integer.parseInt(editTextY.getText().toString());
                            int z = Integer.parseInt(editTextZ.getText().toString());
                            int width = Integer.parseInt(editTextWidth.getText().toString());
                            int height = Integer.parseInt(editTextHeight.getText().toString());
                            mRTCManager.setMergeStreamLayout(mChooseUserId, x, y, z, width, height, isHidden[0], isMuted[0]);
                            mPopWindow.dismiss();
                            ToastUtils.s(LiveRoomActivity.this, "已发送合流配置，请等待合流画面生效");
                        } catch (Exception e) {
                            ToastUtils.s(LiveRoomActivity.this, "请输入所有值");//处理空值
                        }
                    }
                });
                mPopWindow.showAtLocation(getWindow().getDecorView().getRootView(), Gravity.BOTTOM, 0, 0);
            }
        } else {
            logAndToast("会议房间目前没有用户");
        }
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    public void updateRemoteLogText(final String logText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRemoteLogText == null) {
                    mRemoteLogText = new StringBuffer();
                }
                if (mLogView != null && mLogView.getVisibility() == View.VISIBLE) {
                    mRemoteTextView.setText(mRemoteLogText.append(logText + "\n"));
                }
            }
        });

    }

    @Override
    public void onCameraCaptureReady() {
    }

    @Override
    public void onJoinedRoom() {
        mIsJoinedRoom = true;
        mUserList = mRTCManager.getPublishingUserList();
        if (mUserList.size() == 0) {
            showErrorDialog("找不到会议直播，请确认该房间是否有其他用户发布流。");
        } else {
            if (isAdmin(mUserName)) {
                for (int i = 0; i < mUserList.size(); i++) {
                    setMergeRemoteStreamLayout(mUserList.get(i));
                }
            }
            mVideoView.start();
            logAndToast(getString(R.string.connected_to_room));
        }
    }

    @Override
    public void onLocalPublished() {
        Log.i(TAG, "onLocalPublished");
    }

    @Override
    public void onSubscribed(String s) {
        Log.i(TAG, "onSubscribed:" + s);
        updateRemoteLogText("onSubscribed : " + s);
    }

    @Override
    public void onRemotePublished(String s, boolean b, boolean b1) {
        mUserList.add(s);
        updateRemoteLogText("onRemotePublished : " + s + " hasAudio : " + b + " hasVideo : " + b1);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mUserListAdapter != null) {
                    mUserListAdapter.notifyDataSetChanged();
                }
            }
        });
        if (isAdmin(mUserName)) {
            resetMergeStream();
        }
    }

    @Override
    public QNRemoteSurfaceView onRemoteStreamAdded(String s, boolean b, boolean b1, boolean b2, boolean b3) {
        updateRemoteLogText("onRemoteStreamAdded : " + s);
        return null;
    }

    @Override
    public void onRemoteStreamRemoved(String s) {
        Log.i(TAG, "onRemoteStreamRemoved");
        updateRemoteLogText("onRemoteStreamRemoved : " + s);
    }

    @Override
    public void onRemoteUserJoined(String s) {
        Log.i(TAG, "onRemoteUserJoined");
        updateRemoteLogText("onRemoteUserJoined : " + s);
    }

    @Override
    public void onRemoteUserLeaved(String s) {
        Log.i(TAG, "onRemoteUserLeaved : " + s);
        updateRemoteLogText("onRemoteUserLeaved : " + s);
    }

    @Override
    public void onRemoteUnpublished(String s) {
        mUserList.remove(s);
        updateRemoteLogText("onRemoteUnpublished : " + s);
        if (isAdmin(mUserName)) {
            resetMergeStream();
        }
    }

    @Override
    public void onRemoteMute(String s, boolean b, boolean b1) {
        Log.i(TAG, "onRemoteMute");
        updateRemoteLogText("onRemoteMute : " + s);
    }

    @Override
    public void onStateChanged(QNRoomState qnRoomState) {
        Log.i(TAG, "onStateChanged");
        updateRemoteLogText("onStateChanged : " + qnRoomState.name());
    }

    @Override
    public void onError(int i, final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(LiveRoomActivity.this, s);
            }
        });
        mIsJoinedRoom = false;
        disconnect();
        finish();
    }

    @Override
    public void onStatisticsUpdated(QNStatisticsReport report) {
        Log.i(TAG, "onStatisticsUpdated");
    }

    @Override
    public void onUserKickedOut(String s) {
        Log.i(TAG, "onUserKickedOut");
        updateRemoteLogText("onUserKickedOut : " + s);
    }

    @Override
    public void onAudioRouteChanged(QNAudioDevice routing) {
        Log.i(TAG, "onAudioRouteChanged: " + routing.value());
    }
  
    @Override
    public void onCreateMergeJobSuccess(String mergeJobId) {
        Log.i(TAG, "onCreateMergeJobSuccess: " + mergeJobId);
    }

    private class UserListAdapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(getApplicationContext()).inflate(R.layout.item_user, parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.username.setText(mUserList.get(position));
            int[] mColor = {
                    Color.parseColor("#588CEE"), Color.parseColor("#F8CF5F"), Color.parseColor("#4D9F67"), Color.parseColor("#F23A48")
            };
            holder.username.setCircleColor(mColor[position % 4]);
            if (mUserList.get(position).equals(mChooseUserId)) {
                holder.itemView.setBackground(getResources().getDrawable(R.drawable.white_background));
            } else {
                holder.itemView.setBackgroundResource(0);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mChooseUserId = mUserList.get(holder.getAdapterPosition());
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mUserList.size();
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        CircleTextView username;

        private ViewHolder(View itemView) {
            super(itemView);
            username = (CircleTextView) itemView.findViewById(R.id.user_name_text);
        }
    }

    private void resetMergeStream() {
        if (mMergeStreamPosition != null) {
            for (int i = 0; i < mMergeStreamPosition.length; i++) {
                mMergeStreamPosition[i] = null;
            }
            for (int i = 0; i < mUserList.size(); i++) {
                setMergeRemoteStreamLayout(mUserList.get(i));
            }
        }
    }

    private synchronized void clearMergeStreamPos(String userId) {
        int pos = -1;
        if (mMergeStreamPosition != null && !TextUtils.isEmpty(userId)) {
            for (int i = 0; i < mMergeStreamPosition.length; i++) {
                if (userId.equals(mMergeStreamPosition[i])) {
                    pos = i;
                    break;
                }
            }
        }
        if (pos >= 0 && pos < mMergeStreamPosition.length) {
            mMergeStreamPosition[pos] = null;
        }
    }

    private int getMergeStreamIdlePos() {
        int pos = -1;
        for (int i = 0; i < mMergeStreamPosition.length; i++) {
            if (TextUtils.isEmpty(mMergeStreamPosition[i])) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    private synchronized void setMergeRemoteStreamLayout(String userId) {
        int pos = getMergeStreamIdlePos();
        if (pos == -1) {
            Log.e(TAG, "No idle position for merge streaming, so discard.");
            return;
        }
        if (mUserList.size() == 1 || mUserList.get(0).equals(userId)) {
            mRTCManager.setMergeStreamLayout(mUserList.get(0), 0, 0, 0, QNAppServer.STREAMING_WIDTH, QNAppServer.STREAMING_HEIGHT, false, false);
        } else {
            int x = QNAppServer.MERGE_STREAM_POS[pos][0];
            int y = QNAppServer.MERGE_STREAM_POS[pos][1];
            mRTCManager.setMergeStreamLayout(userId, x, y, 1, QNAppServer.MERGE_STREAM_WIDTH, QNAppServer.MERGE_STREAM_HEIGHT, false, false);
            mMergeStreamPosition[pos] = userId;
        }
    }

    private boolean isAdmin(String username) {
        return username.trim().equals(QNAppServer.ADMIN_USER);
    }

    private void showErrorDialog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(LiveRoomActivity.this)
                        .setTitle(getText(R.string.channel_error_title))
                        .setMessage(message)
                        .setCancelable(false)
                        .setNeutralButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        disconnect();
                                        finish();
                                    }
                                })
                        .create()
                        .show();
            }
        });
    }
}

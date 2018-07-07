package com.qiniu.droid.rtc.demo.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qiniu.droid.rtc.demo.R;

/**
 * Fragment for call control.
 */
public class ControlFragment extends Fragment {
    private View mControlView;
    private ImageButton mDisconnectButton;
    private ImageButton mCameraSwitchButton;
    private ImageButton mToggleMuteButton;
    private ImageButton mToggleBeautyButton;
    private ImageButton mToggleSpeakerButton;
    private ImageButton mToggleVideoButton;
    private ImageButton mLogShownButton;
    private LinearLayout mLogView;
    private TextView mLocalTextView;
    private TextView mRemoteTextView;
    private StringBuffer mRemoteLogText;
    private Chronometer mTimer;
    private OnCallEvents mCallEvents;
    private boolean mIsVideoEnabled = true;
    private boolean mIsShowingLog = false;
    private boolean mIsScreenCaptureEnabled = false;

    /**
     * Call control interface for container activity.
     */
    public interface OnCallEvents {
        void onCallHangUp();

        void onCameraSwitch();

        boolean onToggleMic();

        boolean onToggleVideo();

        boolean onToggleSpeaker();

        boolean onToggleBeauty();
    }

    public void setScreenCaptureEnabled(boolean isScreenCaptureEnabled) {
        mIsScreenCaptureEnabled = isScreenCaptureEnabled;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mControlView = inflater.inflate(R.layout.fragment_room, container, false);

        mDisconnectButton = (ImageButton) mControlView.findViewById(R.id.disconnect_button);
        mCameraSwitchButton = (ImageButton) mControlView.findViewById(R.id.camera_switch_button);
        mToggleBeautyButton = (ImageButton) mControlView.findViewById(R.id.beauty_button);
        mToggleMuteButton = (ImageButton) mControlView.findViewById(R.id.microphone_button);
        mToggleSpeakerButton = (ImageButton) mControlView.findViewById(R.id.speaker_button);
        mToggleVideoButton = (ImageButton) mControlView.findViewById(R.id.camera_button);
        mLogShownButton = (ImageButton) mControlView.findViewById(R.id.log_shown_button);
        mLogView = (LinearLayout) mControlView.findViewById(R.id.log_text);
        mLocalTextView = (TextView) mControlView.findViewById(R.id.local_log_text);
        mLocalTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mRemoteTextView = (TextView) mControlView.findViewById(R.id.remote_log_text);
        mRemoteTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mTimer = (Chronometer) mControlView.findViewById(R.id.timer);

        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallEvents.onCallHangUp();
            }
        });

        if (!mIsScreenCaptureEnabled) {
            mCameraSwitchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCallEvents.onCameraSwitch();
                }
            });
        }

        if (!mIsScreenCaptureEnabled) {
            mToggleBeautyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean enabled = mCallEvents.onToggleBeauty();
                    mToggleBeautyButton.setImageResource(enabled ? R.mipmap.face_beauty_open : R.mipmap.face_beauty_close);
                }
            });
        }

        mToggleMuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean enabled = mCallEvents.onToggleMic();
                mToggleMuteButton.setImageResource(enabled ? R.mipmap.microphone : R.mipmap.microphone_disable);
            }
        });

        if (mIsScreenCaptureEnabled) {
            mToggleVideoButton.setImageResource(R.mipmap.video_close);
        } else {
            mToggleVideoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean enabled = mCallEvents.onToggleVideo();
                    mToggleVideoButton.setImageResource(enabled ? R.mipmap.video_open : R.mipmap.video_close);
                }
            });
        }

        mToggleSpeakerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean enabled = mCallEvents.onToggleSpeaker();
                mToggleSpeakerButton.setImageResource(enabled ? R.mipmap.loudspeaker : R.mipmap.loudspeaker_disable);
            }
        });

        mLogShownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogView.setVisibility(mIsShowingLog ? View.INVISIBLE : View.VISIBLE);
                mIsShowingLog = !mIsShowingLog;
            }
        });

        return mControlView;
    }

    public void startTimer() {
        mTimer.setBase(SystemClock.elapsedRealtime());
        mTimer.start();
    }

    public void stopTimer() {
        mTimer.stop();
    }

    public void updateLocalLogText(String logText) {
        if (mLogView.getVisibility() == View.VISIBLE) {
            mLocalTextView.setText(logText);
        }
    }

    public void updateRemoteLogText(String logText) {
        if (mRemoteLogText == null) {
            mRemoteLogText = new StringBuffer();
        }
        if (mLogView != null && mLogView.getVisibility() == View.VISIBLE) {
            mRemoteTextView.setText(mRemoteLogText.append(logText + "\n"));
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!mIsVideoEnabled) {
            mCameraSwitchButton.setVisibility(View.INVISIBLE);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallEvents = (OnCallEvents) activity;
    }
}

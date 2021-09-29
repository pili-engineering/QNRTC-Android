package com.qiniu.droid.rtc.demo.fragment;

import android.app.Activity;
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

import androidx.fragment.app.Fragment;

import com.qiniu.droid.rtc.demo.R;

import org.jetbrains.annotations.NotNull;

/**
 * Fragment for call control.
 */
public class ControlFragment extends Fragment {
    private ImageButton mToggleMuteButton;
    private ImageButton mToggleBeautyButton;
    private ImageButton mToggleSpeakerButton;
    private ImageButton mToggleVideoButton;
    private LinearLayout mLogView;
    private TextView mDirectStreamingButton;
    private TextView mLocalTextViewForVideo;
    private TextView mLocalTextViewForAudio;
    private TextView mRemoteTextView;
    private StringBuffer mRemoteLogText;
    private Chronometer mTimer;
    private OnCallEvents mCallEvents;
    private boolean mIsShowingLog = false;
    private boolean mIsScreenCaptureEnabled = false;
    private boolean mIsAudioOnly = false;

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

        void onCallMerge();

        void onToggleDirectLiving();
    }

    public void setScreenCaptureEnabled(boolean isScreenCaptureEnabled) {
        mIsScreenCaptureEnabled = isScreenCaptureEnabled;
    }

    public void setAudioOnly(boolean isAudioOnly) {
        mIsAudioOnly = isAudioOnly;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View controlView = inflater.inflate(R.layout.fragment_room, container, false);

        ImageButton disconnectButton = controlView.findViewById(R.id.disconnect_button);
        ImageButton cameraSwitchButton = controlView.findViewById(R.id.camera_switch_button);
        mToggleBeautyButton = controlView.findViewById(R.id.beauty_button);
        mToggleMuteButton = controlView.findViewById(R.id.microphone_button);
        mToggleSpeakerButton = controlView.findViewById(R.id.speaker_button);
        mToggleVideoButton = controlView.findViewById(R.id.camera_button);
        ImageButton logShownButton = controlView.findViewById(R.id.log_shown_button);
        mLogView = controlView.findViewById(R.id.log_text);
        TextView transcodingStreamingButton = controlView.findViewById(R.id.transcoding_streaming_button);
        mDirectStreamingButton = controlView.findViewById(R.id.direct_streaming_button);
        mLocalTextViewForVideo = controlView.findViewById(R.id.local_log_text_video);
        mLocalTextViewForVideo.setMovementMethod(ScrollingMovementMethod.getInstance());
        mLocalTextViewForAudio = controlView.findViewById(R.id.local_log_text_audio);
        mLocalTextViewForAudio.setMovementMethod(ScrollingMovementMethod.getInstance());
        mRemoteTextView = controlView.findViewById(R.id.remote_log_text);
        mRemoteTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mTimer = controlView.findViewById(R.id.timer);

        disconnectButton.setOnClickListener(view -> mCallEvents.onCallHangUp());

        if (!mIsScreenCaptureEnabled && !mIsAudioOnly) {
            cameraSwitchButton.setOnClickListener(view -> mCallEvents.onCameraSwitch());
        }

        if (!mIsScreenCaptureEnabled && !mIsAudioOnly) {
            mToggleBeautyButton.setOnClickListener(v -> {
                boolean enabled = mCallEvents.onToggleBeauty();
                mToggleBeautyButton.setImageResource(enabled ? R.mipmap.face_beauty_open : R.mipmap.face_beauty_close);
            });
        }

        mToggleMuteButton.setOnClickListener(view -> {
            boolean enabled = mCallEvents.onToggleMic();
            mToggleMuteButton.setImageResource(enabled ? R.mipmap.microphone : R.mipmap.microphone_disable);
        });

        if (mIsScreenCaptureEnabled || mIsAudioOnly) {
            mToggleVideoButton.setImageResource(R.mipmap.video_close);
        } else {
            mToggleVideoButton.setOnClickListener(v -> {
                boolean enabled = mCallEvents.onToggleVideo();
                mToggleVideoButton.setImageResource(enabled ? R.mipmap.video_open : R.mipmap.video_close);
            });
        }

        mToggleSpeakerButton.setOnClickListener(v -> {
            boolean enabled = mCallEvents.onToggleSpeaker();
            mToggleSpeakerButton.setImageResource(enabled ? R.mipmap.loudspeaker : R.mipmap.loudspeaker_disable);
        });

        logShownButton.setOnClickListener(v -> {
            mLogView.setVisibility(mIsShowingLog ? View.INVISIBLE : View.VISIBLE);
            mIsShowingLog = !mIsShowingLog;
        });

        transcodingStreamingButton.setOnClickListener(v -> mCallEvents.onCallMerge());

        mDirectStreamingButton.setOnClickListener(v -> mCallEvents.onToggleDirectLiving());
        return controlView;
    }

    public void startTimer() {
        mTimer.setBase(SystemClock.elapsedRealtime());
        mTimer.start();
    }

    public void stopTimer() {
        mTimer.stop();
    }

    public void updateLocalVideoLogText(String logText) {
        if (mLogView.getVisibility() == View.VISIBLE) {
            mLocalTextViewForVideo.setText(logText);
        }
    }

    public void updateLocalAudioLogText(String logText) {
        if (mLogView.getVisibility() == View.VISIBLE) {
            mLocalTextViewForAudio.setText(logText);
        }
    }

    public void updateRemoteLogText(String logText) {
        if (mRemoteLogText == null) {
            mRemoteLogText = new StringBuffer();
        }
        if (mLogView != null) {
            mRemoteTextView.setText(mRemoteLogText.append(logText + "\n"));
        }
    }

    public void updateDirectText(String directText) {
        if (mDirectStreamingButton != null) {
            mDirectStreamingButton.setText(directText);
        }
    }

    @Override
    public void onAttach(@NotNull Activity activity) {
        super.onAttach(activity);
        mCallEvents = (OnCallEvents) activity;
    }
}

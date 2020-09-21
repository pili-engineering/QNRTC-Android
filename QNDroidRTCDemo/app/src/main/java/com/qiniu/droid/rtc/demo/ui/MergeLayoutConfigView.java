package com.qiniu.droid.rtc.demo.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.model.RTCTrackMergeOption;
import com.qiniu.droid.rtc.demo.model.RTCUserMergeOptions;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNMergeJob;
import com.qiniu.droid.rtc.model.QNMergeTrackOption;
import com.qiniu.droid.rtc.model.QNStretchMode;

import java.util.ArrayList;
import java.util.List;

public class MergeLayoutConfigView extends FrameLayout {

    private boolean mInit;
    private OnClickedListener mOnClickedListener;
    private RecyclerView mUserListView;
    private Switch mStreamingEnableSwitch;
    private Switch mFirstVideoSwitch;
    private EditText mFirstEditTextX;
    private EditText mFirstEditTextY;
    private EditText mFirstEditTextZ;
    private EditText mFirstEditTextWidth;
    private EditText mFirstEditTextHeight;

    private Switch mSecondVideoSwitch;
    private EditText mSecondEditTextX;
    private EditText mSecondEditTextY;
    private EditText mSecondEditTextZ;
    private EditText mSecondEditTextWidth;
    private EditText mSecondEditTextHeight;
    private Switch mAudioSwitch;
    private Button mBtnConfirm;

    private RTCTrackMergeOption mUserAudioTrack;
    private RTCTrackMergeOption mUserFirstVideoTrack;
    private RTCTrackMergeOption mUserSecondVideoTrack;

    private LinearLayout mCustomMergeJobLayout;
    private Switch mCustomMergeJobSwitch;
    private TextView mPublishUrlText;
    private EditText mStreamWidthText;
    private EditText mStreamHeightText;
    private EditText mStreamFpsText;
    private EditText mCustomJobIdText;
    private EditText mStreamBitrateText;
    private EditText mStreamMinBitrateText;
    private EditText mStreamMaxBitrateText;
    private RadioGroup mStretchModeRadioGroup;
    private QNStretchMode mStretchMode;
    private QNMergeJob mCurrentMergeJob;

    private String mRoomId;
    private boolean mIsStreamingEnabled;
    private boolean mIsCustomMergeJobEnabled;
    private volatile int mSerialNum;

    public interface OnClickedListener {
        void onConfirmClicked();
    }

    public MergeLayoutConfigView(Context context) {
        super(context);
        init(context);
    }

    public MergeLayoutConfigView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MergeLayoutConfigView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(21)
    public MergeLayoutConfigView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public void setOnClickedListener(OnClickedListener listener) {
        mOnClickedListener = listener;
    }

    public void setRoomId(String roomId) {
        mRoomId = roomId;
        mCustomJobIdText.setText(mRoomId);
        String publishUrl = String.format(getResources().getString(R.string.publish_url), mRoomId, mSerialNum);
        mPublishUrlText.setText(publishUrl);
    }

    public RecyclerView getUserListView() {
        return mUserListView;
    }

    /**
     * 是否开启合流
     *
     * @return true or false
     */
    public boolean isStreamingEnabled() {
        return mIsStreamingEnabled;
    }

    /**
     * 是否自定义合流
     *
     * @return true or false
     */
    public boolean isCustomMergeJob() {
        return mIsCustomMergeJobEnabled;
    }

    /**
     * 获取 User 对应的 track 信息，并更新 UI
     *
     * @param chooseUser 对应 user
     */
    public void updateConfigInfo(RTCUserMergeOptions chooseUser) {
        if (chooseUser == null) {
            return;
        }

        mUserAudioTrack = chooseUser.getAudioTrack();
        updateSwitchState(mUserAudioTrack, mAudioSwitch);

        List<RTCTrackMergeOption> videoTracks = chooseUser.getVideoTracks();
        if (videoTracks.isEmpty()) {
            setFirstRemoteTrack(null);
            setSecondRemoteTrack(null);
        } else {
            setFirstRemoteTrack(videoTracks.get(0));
            if (videoTracks.size() > 1) {
                setSecondRemoteTrack(videoTracks.get(1));
            } else {
                setSecondRemoteTrack(null);
            }
        }
    }

    /**
     * 获取选中用户更新后的合流配置信息
     *
     * @return 选中用户的合流配置信息
     */
    public List<RTCTrackMergeOption> updateMergeOptions() {
        List<RTCTrackMergeOption> result = new ArrayList<>();
        if (mUserAudioTrack != null) {
            mUserAudioTrack.setTrackInclude(mAudioSwitch.isChecked());
            result.add(mUserAudioTrack);
        }
        if (mUserFirstVideoTrack != null) {
            mUserFirstVideoTrack.setTrackInclude(mFirstVideoSwitch.isChecked());
            QNMergeTrackOption option = mUserFirstVideoTrack.getQNMergeTrackOption();
            try {
                int x = Integer.parseInt(mFirstEditTextX.getText().toString());
                int y = Integer.parseInt(mFirstEditTextY.getText().toString());
                int z = Integer.parseInt(mFirstEditTextZ.getText().toString());
                int width = Integer.parseInt(mFirstEditTextWidth.getText().toString());
                int height = Integer.parseInt(mFirstEditTextHeight.getText().toString());
                option.setX(x);
                option.setY(y);
                option.setZ(z);
                option.setWidth(width);
                option.setHeight(height);
            } catch (Exception e) {
                ToastUtils.s(getContext(), "请输入所有值");//处理空值
            }
            result.add(mUserFirstVideoTrack);
        }
        if (mUserSecondVideoTrack != null) {
            mUserSecondVideoTrack.setTrackInclude(mSecondVideoSwitch.isChecked());
            QNMergeTrackOption option = mUserSecondVideoTrack.getQNMergeTrackOption();
            try {
                int x = Integer.parseInt(mSecondEditTextX.getText().toString());
                int y = Integer.parseInt(mSecondEditTextY.getText().toString());
                int z = Integer.parseInt(mSecondEditTextZ.getText().toString());
                int width = Integer.parseInt(mSecondEditTextWidth.getText().toString());
                int height = Integer.parseInt(mSecondEditTextHeight.getText().toString());
                option.setX(x);
                option.setY(y);
                option.setZ(z);
                option.setWidth(width);
                option.setHeight(height);
            } catch (Exception e) {
                ToastUtils.s(getContext(), "请输入所有值");//处理空值
            }
            result.add(mUserSecondVideoTrack);
        }
        return result;
    }

    /**
     * 获取自定义合流任务的对象
     *
     * @return 合流任务对象实例
     */
    public QNMergeJob getCustomMergeJob() {
        if (!isNeedUpdateMergeJob()) {
            return null;
        }
        if (mCurrentMergeJob == null) {
            mCurrentMergeJob = new QNMergeJob();
        }
        mCurrentMergeJob.setMergeJobId(mCustomJobIdText.getText().toString().trim());
        mCurrentMergeJob.setPublishUrl(mPublishUrlText.getText().toString().trim());
        mCurrentMergeJob.setWidth(Integer.parseInt(mStreamWidthText.getText().toString().trim()));
        mCurrentMergeJob.setHeight(Integer.parseInt(mStreamHeightText.getText().toString().trim()));
        // QNMergeJob 中码率单位为 bps，所以，若期望码率为 1200kbps，则实际传入的参数值应为 1200 * 1000
        mCurrentMergeJob.setBitrate(Integer.parseInt(mStreamBitrateText.getText().toString().trim()) * 1000);
        mCurrentMergeJob.setMinBitrate(Integer.parseInt(mStreamMinBitrateText.getText().toString().trim()) * 1000);
        mCurrentMergeJob.setMaxBitrate(Integer.parseInt(mStreamMaxBitrateText.getText().toString().trim()) * 1000);
        mCurrentMergeJob.setFps(Integer.parseInt(mStreamFpsText.getText().toString().trim()));
        mCurrentMergeJob.setStretchMode(mStretchMode);
        return mCurrentMergeJob;
    }

    /**
     * 更新合流任务信息，用于用户在更改后没有确认生效的场景下，恢复默认值
     */
    public void updateMergeJobConfigInfo() {
        mStreamingEnableSwitch.setChecked(mIsStreamingEnabled);
        mCustomMergeJobSwitch.setChecked(mCurrentMergeJob != null && mIsCustomMergeJobEnabled);
        mCustomMergeJobLayout.setVisibility(mCustomMergeJobSwitch.isChecked() ? VISIBLE : GONE);
        String publishUrl = String.format(getResources().getString(R.string.publish_url), mRoomId, mSerialNum);
        mPublishUrlText.setText(publishUrl);
        if (mIsCustomMergeJobEnabled) {
            mStreamWidthText.setText(String.valueOf(mCurrentMergeJob != null ? mCurrentMergeJob.getWidth() : 480));
            mStreamHeightText.setText(String.valueOf(mCurrentMergeJob != null ? mCurrentMergeJob.getHeight() : 848));
            mStreamFpsText.setText(String.valueOf(mCurrentMergeJob != null ? mCurrentMergeJob.getFps() : 25));
            mCustomJobIdText.setText(String.valueOf(mCurrentMergeJob != null ? mCurrentMergeJob.getMergeJobId() : mRoomId));
            mStreamBitrateText.setText(String.valueOf(mCurrentMergeJob != null ? mCurrentMergeJob.getBitrate() / 1000 : 1000));
            mStreamMinBitrateText.setText(String.valueOf(mCurrentMergeJob != null ? mCurrentMergeJob.getMinBitrate() / 1000 : 800));
            mStreamMaxBitrateText.setText(String.valueOf(mCurrentMergeJob != null ? mCurrentMergeJob.getMaxBitrate() / 1000 : 1200));
            if (mCurrentMergeJob == null || mCurrentMergeJob.getStretchMode() == null) {
                mStretchModeRadioGroup.check(R.id.radio_aspect_fill);
            } else {
                switch (mCurrentMergeJob.getStretchMode()) {
                    case ASPECT_FILL:
                        mStretchModeRadioGroup.check(R.id.radio_aspect_fill);
                        break;
                    case ASPECT_FIT:
                        mStretchModeRadioGroup.check(R.id.radio_aspect_fit);
                        break;
                    case SCALE_TO_FIT:
                        mStretchModeRadioGroup.check(R.id.radio_scale_to_fit);
                        break;
                }
            }
        }
    }

    public void updateSerialNum(int serialNum) {
        mSerialNum = serialNum;
    }

    public void updateStreamingStatus(boolean isStreaming) {
        mIsStreamingEnabled = isStreaming;
        mStreamingEnableSwitch.setChecked(isStreaming);
    }

    private void init(Context context) {
        if (mInit) {
            return;
        }
        mInit = true;
        View view = LayoutInflater.from(context).inflate(R.layout.layout_config_view, this, true);
        intView(view);
    }

    private void intView(View view) {
        mUserListView = view.findViewById(R.id.user_list_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mUserListView.setLayoutManager(linearLayoutManager);

        mStreamingEnableSwitch = view.findViewById(R.id.streaming_enable_switch);
        mCustomMergeJobLayout = view.findViewById(R.id.merge_job_layout);
        mCustomMergeJobSwitch = view.findViewById(R.id.custom_mergejob_switch);
        mFirstVideoSwitch = view.findViewById(R.id.first_video_switch);
        mFirstEditTextX = view.findViewById(R.id.first_x_edit_text);
        mFirstEditTextY = view.findViewById(R.id.first_y_edit_text);
        mFirstEditTextZ = view.findViewById(R.id.first_z_edit_text);
        mFirstEditTextWidth = view.findViewById(R.id.first_width_edit_text);
        mFirstEditTextHeight = view.findViewById(R.id.first_height_edit_text);

        mSecondVideoSwitch = view.findViewById(R.id.second_video_switch);
        mSecondEditTextX = view.findViewById(R.id.second_x_edit_text);
        mSecondEditTextY = view.findViewById(R.id.second_y_edit_text);
        mSecondEditTextZ = view.findViewById(R.id.second_z_edit_text);
        mSecondEditTextWidth = view.findViewById(R.id.second_width_edit_text);
        mSecondEditTextHeight = view.findViewById(R.id.second_height_edit_text);

        mAudioSwitch = view.findViewById(R.id.audio_switch);
        mBtnConfirm = view.findViewById(R.id.btn_confirm);
        mBtnConfirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsStreamingEnabled = mStreamingEnableSwitch.isChecked();
                mIsCustomMergeJobEnabled = mCustomMergeJobSwitch.isChecked();
                if (mOnClickedListener != null) {
                    mOnClickedListener.onConfirmClicked();
                }
            }
        });

        mPublishUrlText = view.findViewById(R.id.publish_url_text);
        mStreamWidthText = view.findViewById(R.id.stream_width);
        mStreamHeightText = view.findViewById(R.id.stream_height);
        mStreamFpsText = view.findViewById(R.id.stream_fps);
        mCustomJobIdText = view.findViewById(R.id.edit_job_id);
        mStreamBitrateText = view.findViewById(R.id.stream_bitrate);
        mStreamMinBitrateText = view.findViewById(R.id.stream_bitrate_min);
        mStreamMaxBitrateText = view.findViewById(R.id.stream_bitrate_max);
        mStretchModeRadioGroup = view.findViewById(R.id.stretch_mode_radio_button);

        mStretchModeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_aspect_fill:
                        mStretchMode = QNStretchMode.ASPECT_FILL;
                        break;
                    case R.id.radio_aspect_fit:
                        mStretchMode = QNStretchMode.ASPECT_FIT;
                        break;
                    case R.id.radio_scale_to_fit:
                        mStretchMode = QNStretchMode.SCALE_TO_FIT;
                        break;
                }
            }
        });

        mCustomMergeJobSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCustomMergeJobLayout.setVisibility(isChecked ? VISIBLE : GONE);
            }
        });
    }

    private void updateSwitchState(RTCTrackMergeOption trackMergeOption, Switch switchButton) {
        if (trackMergeOption != null) {
            switchButton.setChecked(trackMergeOption.isTrackInclude());
            switchButton.setEnabled(true);
        } else {
            switchButton.setChecked(true);
            switchButton.setEnabled(false);
        }
    }


    private void setFirstRemoteTrack(RTCTrackMergeOption videoTrack) {
        mUserFirstVideoTrack = videoTrack;

        updateSwitchState(mUserFirstVideoTrack, mFirstVideoSwitch);
        boolean hasTrack = mUserFirstVideoTrack != null;
        mFirstEditTextX.setEnabled(hasTrack);
        mFirstEditTextY.setEnabled(hasTrack);
        mFirstEditTextZ.setEnabled(hasTrack);
        mFirstEditTextWidth.setEnabled(hasTrack);
        mFirstEditTextHeight.setEnabled(hasTrack);

        if (hasTrack) {
            QNMergeTrackOption option = mUserFirstVideoTrack.getQNMergeTrackOption();
            String videoTag = mUserFirstVideoTrack.getQNTrackInfo().getTag();
            if (UserTrackView.TAG_CAMERA.equals(videoTag)) {
                mFirstVideoSwitch.setText(R.string.video_camera);
            } else if (UserTrackView.TAG_SCREEN.equals(videoTag)) {
                mFirstVideoSwitch.setText(R.string.video_screen);
            } else {
                if (TextUtils.isEmpty(videoTag)) {
                    mFirstVideoSwitch.setText(R.string.video_camera);
                } else {
                    mFirstVideoSwitch.setText(videoTag);
                }
            }
            mFirstEditTextX.setText(String.valueOf(option.getX()));
            mFirstEditTextY.setText(String.valueOf(option.getY()));
            mFirstEditTextZ.setText(String.valueOf(option.getZ()));
            mFirstEditTextWidth.setText(String.valueOf(option.getWidth()));
            mFirstEditTextHeight.setText(String.valueOf(option.getHeight()));
        } else {
            mFirstVideoSwitch.setText(R.string.video_camera);
            mFirstEditTextX.setText("-");
            mFirstEditTextY.setText("-");
            mFirstEditTextZ.setText("-");
            mFirstEditTextWidth.setText("-");
            mFirstEditTextHeight.setText("-");
        }
    }

    private void setSecondRemoteTrack(RTCTrackMergeOption videoTrack) {
        mUserSecondVideoTrack = videoTrack;
        updateSwitchState(mUserSecondVideoTrack, mSecondVideoSwitch);
        boolean hasTrack = mUserSecondVideoTrack != null;
        mSecondEditTextX.setEnabled(hasTrack);
        mSecondEditTextY.setEnabled(hasTrack);
        mSecondEditTextZ.setEnabled(hasTrack);
        mSecondEditTextWidth.setEnabled(hasTrack);
        mSecondEditTextHeight.setEnabled(hasTrack);

        if (hasTrack) {
            QNMergeTrackOption option = mUserSecondVideoTrack.getQNMergeTrackOption();
            String videoTag = mUserSecondVideoTrack.getQNTrackInfo().getTag();
            if (UserTrackView.TAG_CAMERA.equals(videoTag)) {
                mSecondVideoSwitch.setText(R.string.video_camera);
            } else if (UserTrackView.TAG_SCREEN.equals(videoTag)) {
                mSecondVideoSwitch.setText(R.string.video_screen);
            } else {
                if (TextUtils.isEmpty(videoTag)) {
                    mSecondVideoSwitch.setText(R.string.video_camera);
                } else {
                    mSecondVideoSwitch.setText(videoTag);
                }
            }
            mSecondEditTextX.setText(String.valueOf(option.getX()));
            mSecondEditTextY.setText(String.valueOf(option.getY()));
            mSecondEditTextZ.setText(String.valueOf(option.getZ()));
            mSecondEditTextWidth.setText(String.valueOf(option.getWidth()));
            mSecondEditTextHeight.setText(String.valueOf(option.getHeight()));
        } else {
            mSecondVideoSwitch.setText(R.string.video_screen);
            mSecondEditTextX.setText("-");
            mSecondEditTextY.setText("-");
            mSecondEditTextZ.setText("-");
            mSecondEditTextWidth.setText("-");
            mSecondEditTextHeight.setText("-");
        }
    }

    private boolean isNeedUpdateMergeJob() {
        if (mCurrentMergeJob == null) {
            return true;
        }
        return !mCustomJobIdText.getText().toString().trim().equals(mCurrentMergeJob.getMergeJobId())
                || !mPublishUrlText.getText().toString().trim().equals(mCurrentMergeJob.getPublishUrl())
                || Integer.parseInt(mStreamWidthText.getText().toString().trim()) != mCurrentMergeJob.getWidth()
                || Integer.parseInt(mStreamHeightText.getText().toString().trim()) != mCurrentMergeJob.getHeight()
                || Integer.parseInt(mStreamBitrateText.getText().toString().trim()) != mCurrentMergeJob.getBitrate() / 1000
                || Integer.parseInt(mStreamMinBitrateText.getText().toString().trim()) != mCurrentMergeJob.getMinBitrate() / 1000
                || Integer.parseInt(mStreamMaxBitrateText.getText().toString().trim()) != mCurrentMergeJob.getMaxBitrate() / 1000
                || Integer.parseInt(mStreamFpsText.getText().toString().trim()) != mCurrentMergeJob.getFps()
                || mStretchMode != mCurrentMergeJob.getStretchMode();
    }
}

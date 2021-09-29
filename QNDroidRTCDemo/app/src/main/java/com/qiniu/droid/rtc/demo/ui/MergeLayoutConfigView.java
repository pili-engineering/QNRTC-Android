package com.qiniu.droid.rtc.demo.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qiniu.droid.rtc.QNRenderMode;
import com.qiniu.droid.rtc.QNTranscodingLiveStreamingConfig;
import com.qiniu.droid.rtc.QNTranscodingLiveStreamingTrack;
import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.model.RTCTrackMergeOption;
import com.qiniu.droid.rtc.demo.model.RTCUserMergeOptions;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;

import java.util.List;

import static com.qiniu.droid.rtc.demo.activity.RoomActivity.TRACK_TAG_CAMERA;
import static com.qiniu.droid.rtc.demo.activity.RoomActivity.TRACK_TAG_SCREEN;

public class MergeLayoutConfigView extends FrameLayout {

    private boolean mInit;
    private OnClickedListener mOnClickedListener;
    private RecyclerView mUserListView;
    private SwitchCompat mStreamingEnableSwitch;
    private SwitchCompat mFirstVideoSwitch;
    private EditText mFirstEditTextX;
    private EditText mFirstEditTextY;
    private EditText mFirstEditTextZ;
    private EditText mFirstEditTextWidth;
    private EditText mFirstEditTextHeight;

    private SwitchCompat mSecondVideoSwitch;
    private EditText mSecondEditTextX;
    private EditText mSecondEditTextY;
    private EditText mSecondEditTextZ;
    private EditText mSecondEditTextWidth;
    private EditText mSecondEditTextHeight;
    private SwitchCompat mAudioSwitch;

    private RTCTrackMergeOption mUserAudioMergeOption;
    private RTCTrackMergeOption mUserFirstVideoMergeOption;
    private RTCTrackMergeOption mUserSecondVideoMergeOption;

    private LinearLayout mCustomMergeConfigLayout;
    private SwitchCompat mCustomMergeConfigSwitch;
    private TextView mPublishUrlText;
    private EditText mStreamWidthText;
    private EditText mStreamHeightText;
    private EditText mStreamFpsText;
    private EditText mCustomConfigIdText;
    private EditText mStreamBitrateText;
    private EditText mStreamMinBitrateText;
    private EditText mStreamMaxBitrateText;
    private RadioGroup mStretchModeRadioGroup;
    private QNRenderMode mRenderMode;
    private QNTranscodingLiveStreamingConfig mCurrentMergeConfig;
    private boolean mCurrentMergeConfigValid;

    private String mRoomId;
    private boolean mIsStreamingEnabled;
    private boolean mIsCustomMergeEnabled;
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
        mCustomConfigIdText.setText(mRoomId);
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
    public boolean isCustomMerge() {
        return mIsCustomMergeEnabled;
    }

    /**
     * 获取 User 对应的合流配置信息并更新 UI
     */
    public void updateConfigInfo(RTCUserMergeOptions userMergeOptions) {
        if (userMergeOptions == null) {
            return;
        }

        mUserAudioMergeOption = userMergeOptions.getAudioMergeOption();
        updateSwitchState(mUserAudioMergeOption, mAudioSwitch);

        List<RTCTrackMergeOption> videoMergeOptions = userMergeOptions.getVideoMergeOptions();
        if (videoMergeOptions.isEmpty()) {
            setFirstRemoteTrack(null);
            setSecondRemoteTrack(null);
        } else {
            setFirstRemoteTrack(videoMergeOptions.get(0));
            if (videoMergeOptions.size() > 1) {
                setSecondRemoteTrack(videoMergeOptions.get(1));
            } else {
                setSecondRemoteTrack(null);
            }
        }
    }

    /**
     * 同步 UI 选择的合流参数到合流配置类
     */
    public void updateMergeOptions() {
        if (mUserAudioMergeOption != null) {
            mUserAudioMergeOption.setTrackInclude(mAudioSwitch.isChecked());
        }
        if (mUserFirstVideoMergeOption != null) {
            mUserFirstVideoMergeOption.setTrackInclude(mFirstVideoSwitch.isChecked());
            QNTranscodingLiveStreamingTrack option = mUserFirstVideoMergeOption.getMergeTrack();
            try {
                int x = Integer.parseInt(mFirstEditTextX.getText().toString());
                int y = Integer.parseInt(mFirstEditTextY.getText().toString());
                int z = Integer.parseInt(mFirstEditTextZ.getText().toString());
                int width = Integer.parseInt(mFirstEditTextWidth.getText().toString());
                int height = Integer.parseInt(mFirstEditTextHeight.getText().toString());
                option.setX(x);
                option.setY(y);
                option.setZOrder(z);
                option.setWidth(width);
                option.setHeight(height);
            } catch (Exception e) {
                ToastUtils.showShortToast(getContext(), "请输入所有值");//处理空值
            }
        }
        if (mUserSecondVideoMergeOption != null) {
            mUserSecondVideoMergeOption.setTrackInclude(mSecondVideoSwitch.isChecked());
            QNTranscodingLiveStreamingTrack option = mUserSecondVideoMergeOption.getMergeTrack();
            try {
                int x = Integer.parseInt(mSecondEditTextX.getText().toString());
                int y = Integer.parseInt(mSecondEditTextY.getText().toString());
                int z = Integer.parseInt(mSecondEditTextZ.getText().toString());
                int width = Integer.parseInt(mSecondEditTextWidth.getText().toString());
                int height = Integer.parseInt(mSecondEditTextHeight.getText().toString());
                option.setX(x);
                option.setY(y);
                option.setZOrder(z);
                option.setWidth(width);
                option.setHeight(height);
            } catch (Exception e) {
                ToastUtils.showShortToast(getContext(), "请输入所有值");//处理空值
            }
        }
    }

    /**
     * 获取自定义合流配置的对象
     *
     * @return 合流配置对象实例
     */
    public QNTranscodingLiveStreamingConfig getCustomMergeConfig() {
        if (!isNeedUpdateMergeConfig()) {
            return null;
        }
        if (mCurrentMergeConfig == null) {
            mCurrentMergeConfig = new QNTranscodingLiveStreamingConfig();
        }
        mCurrentMergeConfig.setStreamID(mCustomConfigIdText.getText().toString().trim());
        mCurrentMergeConfig.setUrl(mPublishUrlText.getText().toString().trim());
        mCurrentMergeConfig.setWidth(Integer.parseInt(mStreamWidthText.getText().toString().trim()));
        mCurrentMergeConfig.setHeight(Integer.parseInt(mStreamHeightText.getText().toString().trim()));
        // QNTranscodingLiveStreamingConfig 中码率单位为 Kbps，所以，若期望码率为 1200kbps，则实际传入的参数值应为 1200
        mCurrentMergeConfig.setBitrate(Integer.parseInt(mStreamBitrateText.getText().toString().trim()));
        int minBitrate = Integer.parseInt(mStreamMinBitrateText.getText().toString().trim());
        int maxBitrate = Integer.parseInt(mStreamMaxBitrateText.getText().toString().trim());
        mCurrentMergeConfig.setBitrateRange(minBitrate, maxBitrate);
        mCurrentMergeConfig.setVideoFrameRate(Integer.parseInt(mStreamFpsText.getText().toString().trim()));
        mCurrentMergeConfig.setRenderMode(mRenderMode);
        mCurrentMergeConfigValid = true;
        return mCurrentMergeConfig;
    }

    /**
     * 更新合流配置，用于用户在更改后没有确认生效的场景下，恢复默认值
     */
    public void updateMergeConfigInfo() {
        mStreamingEnableSwitch.setChecked(mIsStreamingEnabled);
        mCustomMergeConfigSwitch.setChecked(mCurrentMergeConfig != null && mIsCustomMergeEnabled);
        mCustomMergeConfigLayout.setVisibility(mCustomMergeConfigSwitch.isChecked() ? VISIBLE : GONE);
        String publishUrl = String.format(getResources().getString(R.string.publish_url), mRoomId, mSerialNum);
        mPublishUrlText.setText(publishUrl);
        if (mIsCustomMergeEnabled) {
            mStreamWidthText.setText(String.valueOf(mCurrentMergeConfig != null ? mCurrentMergeConfig.getWidth() : 480));
            mStreamHeightText.setText(String.valueOf(mCurrentMergeConfig != null ? mCurrentMergeConfig.getHeight() : 848));
            mStreamFpsText.setText(String.valueOf(mCurrentMergeConfig != null ? mCurrentMergeConfig.getVideoFrameRate() : 25));
            mCustomConfigIdText.setText(String.valueOf(mCurrentMergeConfig != null ? mCurrentMergeConfig.getStreamID() : mRoomId));
            mStreamBitrateText.setText(String.valueOf(mCurrentMergeConfig != null ? mCurrentMergeConfig.getBitrate() : 1000));
            mStreamMinBitrateText.setText(String.valueOf(mCurrentMergeConfig != null ? mCurrentMergeConfig.getMinBitrate() : 800));
            mStreamMaxBitrateText.setText(String.valueOf(mCurrentMergeConfig != null ? mCurrentMergeConfig.getMaxBitrate() : 1200));
            if (mCurrentMergeConfig == null || mCurrentMergeConfig.getRenderMode() == null) {
                mStretchModeRadioGroup.check(R.id.radio_aspect_fill);
            } else {
                switch (mCurrentMergeConfig.getRenderMode()) {
                    case ASPECT_FILL:
                        mStretchModeRadioGroup.check(R.id.radio_aspect_fill);
                        break;
                    case ASPECT_FIT:
                        mStretchModeRadioGroup.check(R.id.radio_aspect_fit);
                        break;
                    case FILL:
                        mStretchModeRadioGroup.check(R.id.radio_scale_to_fit);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * 更新当前合流配置是否可用
     *
     * @param valid 是否可用
     */
    public void updateMergeConfigValid(boolean valid) {
        mCurrentMergeConfigValid = valid;
    }

    /**
     * 当前合流配置是否可用
     *
     * @return valid 是否可用
     */
    public boolean isMergeConfigValid() {
        return mCurrentMergeConfigValid;
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
        mCustomMergeConfigLayout = view.findViewById(R.id.merge_layout);
        mCustomMergeConfigSwitch = view.findViewById(R.id.custom_merge_switch);
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
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> {
            mIsStreamingEnabled = mStreamingEnableSwitch.isChecked();
            mIsCustomMergeEnabled = mCustomMergeConfigSwitch.isChecked();
            if (mOnClickedListener != null) {
                mOnClickedListener.onConfirmClicked();
            }
        });

        mPublishUrlText = view.findViewById(R.id.publish_url_text);
        mStreamWidthText = view.findViewById(R.id.stream_width);
        mStreamHeightText = view.findViewById(R.id.stream_height);
        mStreamFpsText = view.findViewById(R.id.stream_fps);
        mCustomConfigIdText = view.findViewById(R.id.edit_config_id);
        mStreamBitrateText = view.findViewById(R.id.stream_bitrate);
        mStreamMinBitrateText = view.findViewById(R.id.stream_bitrate_min);
        mStreamMaxBitrateText = view.findViewById(R.id.stream_bitrate_max);
        mStretchModeRadioGroup = view.findViewById(R.id.stretch_mode_radio_button);

        mStretchModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.radio_aspect_fill:
                    mRenderMode = QNRenderMode.ASPECT_FILL;
                    break;
                case R.id.radio_aspect_fit:
                    mRenderMode = QNRenderMode.ASPECT_FIT;
                    break;
                case R.id.radio_scale_to_fit:
                    mRenderMode = QNRenderMode.FILL;
                    break;
                default:
                    break;
            }
        });

        mCustomMergeConfigSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mCustomMergeConfigLayout.setVisibility(isChecked ? VISIBLE : GONE);
        });
    }

    private void updateSwitchState(RTCTrackMergeOption trackMergeOption, SwitchCompat switchButton) {
        if (trackMergeOption != null) {
            switchButton.setChecked(trackMergeOption.isTrackInclude());
            switchButton.setEnabled(true);
        } else {
            switchButton.setChecked(true);
            switchButton.setEnabled(false);
        }
    }


    private void setFirstRemoteTrack(RTCTrackMergeOption videoMergeOption) {
        mUserFirstVideoMergeOption = videoMergeOption;

        updateSwitchState(mUserFirstVideoMergeOption, mFirstVideoSwitch);
        boolean hasTrack = mUserFirstVideoMergeOption != null;
        mFirstEditTextX.setEnabled(hasTrack);
        mFirstEditTextY.setEnabled(hasTrack);
        mFirstEditTextZ.setEnabled(hasTrack);
        mFirstEditTextWidth.setEnabled(hasTrack);
        mFirstEditTextHeight.setEnabled(hasTrack);

        if (hasTrack) {
            QNTranscodingLiveStreamingTrack option = mUserFirstVideoMergeOption.getMergeTrack();
            String videoTag = mUserFirstVideoMergeOption.getTrack().getTag();
            if (TRACK_TAG_CAMERA.equals(videoTag)) {
                mFirstVideoSwitch.setText(R.string.video_camera);
            } else if (TRACK_TAG_SCREEN.equals(videoTag)) {
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
            mFirstEditTextZ.setText(String.valueOf(option.getZOrder()));
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
        mUserSecondVideoMergeOption = videoTrack;
        updateSwitchState(mUserSecondVideoMergeOption, mSecondVideoSwitch);
        boolean hasTrack = mUserSecondVideoMergeOption != null;
        mSecondEditTextX.setEnabled(hasTrack);
        mSecondEditTextY.setEnabled(hasTrack);
        mSecondEditTextZ.setEnabled(hasTrack);
        mSecondEditTextWidth.setEnabled(hasTrack);
        mSecondEditTextHeight.setEnabled(hasTrack);

        if (hasTrack) {
            QNTranscodingLiveStreamingTrack option = mUserSecondVideoMergeOption.getMergeTrack();
            String videoTag = mUserSecondVideoMergeOption.getTrack().getTag();
            if (TRACK_TAG_CAMERA.equals(videoTag)) {
                mSecondVideoSwitch.setText(R.string.video_camera);
            } else if (TRACK_TAG_SCREEN.equals(videoTag)) {
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
            mSecondEditTextZ.setText(String.valueOf(option.getZOrder()));
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

    private boolean isNeedUpdateMergeConfig() {
        if (mCurrentMergeConfig == null) {
            return true;
        }
        return !mCurrentMergeConfigValid || !isPublishUrlIdentity()
                || !mCustomConfigIdText.getText().toString().trim().equals(mCurrentMergeConfig.getStreamID())
                || Integer.parseInt(mStreamWidthText.getText().toString().trim()) != mCurrentMergeConfig.getWidth()
                || Integer.parseInt(mStreamHeightText.getText().toString().trim()) != mCurrentMergeConfig.getHeight()
                || Integer.parseInt(mStreamBitrateText.getText().toString().trim()) != mCurrentMergeConfig.getBitrate() / 1000
                || Integer.parseInt(mStreamMinBitrateText.getText().toString().trim()) != mCurrentMergeConfig.getMinBitrate() / 1000
                || Integer.parseInt(mStreamMaxBitrateText.getText().toString().trim()) != mCurrentMergeConfig.getMaxBitrate() / 1000
                || Integer.parseInt(mStreamFpsText.getText().toString().trim()) != mCurrentMergeConfig.getVideoFrameRate()
                || mRenderMode != mCurrentMergeConfig.getRenderMode();
    }

    private boolean isPublishUrlIdentity() {
        String url1 = mPublishUrlText.getText().toString().trim();
        String url2 = mCurrentMergeConfig.getUrl();
        return url1.substring(0, url1.indexOf("?serialnum")).equals(url2.substring(0, url2.indexOf("?serialnum")));
    }
}

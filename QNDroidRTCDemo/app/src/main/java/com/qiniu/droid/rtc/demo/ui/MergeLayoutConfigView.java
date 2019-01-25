package com.qiniu.droid.rtc.demo.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Switch;

import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.model.RemoteTrack;
import com.qiniu.droid.rtc.demo.model.RemoteUser;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;
import com.qiniu.droid.rtc.model.QNMergeTrackOption;

import java.util.ArrayList;
import java.util.List;

public class MergeLayoutConfigView extends FrameLayout {

    private boolean mInit;
    private RecyclerView mUserListView;
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

    private void init(Context context) {
        if (mInit) {
            return;
        }
        mInit = true;
        View view = LayoutInflater.from(context).inflate(R.layout.layout_config_view, this, true);
        intView(view);
    }

    private void intView(View view) {
        mUserListView = (RecyclerView) view.findViewById(R.id.user_list_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mUserListView.setLayoutManager(linearLayoutManager);

        mFirstVideoSwitch = (Switch) view.findViewById(R.id.first_video_switch);
        mFirstEditTextX = (EditText) view.findViewById(R.id.first_x_edit_text);
        mFirstEditTextY = (EditText) view.findViewById(R.id.first_y_edit_text);
        mFirstEditTextZ = (EditText) view.findViewById(R.id.first_z_edit_text);
        mFirstEditTextWidth = (EditText) view.findViewById(R.id.first_width_edit_text);
        mFirstEditTextHeight = (EditText) view.findViewById(R.id.first_height_edit_text);

        mSecondVideoSwitch = (Switch) view.findViewById(R.id.second_video_switch);
        mSecondEditTextX = (EditText) view.findViewById(R.id.second_x_edit_text);
        mSecondEditTextY = (EditText) view.findViewById(R.id.second_y_edit_text);
        mSecondEditTextZ = (EditText) view.findViewById(R.id.second_z_edit_text);
        mSecondEditTextWidth = (EditText) view.findViewById(R.id.second_width_edit_text);
        mSecondEditTextHeight = (EditText) view.findViewById(R.id.second_height_edit_text);

        mAudioSwitch = (Switch) view.findViewById(R.id.audio_switch);
        mBtnConfirm = (Button) view.findViewById(R.id.btn_confirm);
    }

    public RecyclerView getUserListView() {
        return mUserListView;
    }

    public Button getBtnConfirm() {
        return mBtnConfirm;
    }

    private RemoteTrack mRemoteAudioTrack;
    private RemoteTrack mRemoteFirstVideoTrack;
    private RemoteTrack mRemoteSecondVideoTrack;

    public void updateConfigInfo(RemoteUser chooseUser) {
        if (chooseUser == null) {
            return;
        }

        mRemoteAudioTrack = chooseUser.getRemoteAudioTrack();
        updateSwitchState(mRemoteAudioTrack, mAudioSwitch);

        List<RemoteTrack> videoTracks = chooseUser.getRemoteVideoTracks();
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

    private void updateSwitchState(RemoteTrack remoteTrack, Switch switchButton) {
        if (remoteTrack != null) {
            switchButton.setChecked(remoteTrack.isTrackInclude());
            switchButton.setEnabled(true);
        } else {
            switchButton.setChecked(true);
            switchButton.setEnabled(false);
        }
    }


    private void setFirstRemoteTrack(RemoteTrack videoTrack) {
        mRemoteFirstVideoTrack = videoTrack;

        updateSwitchState(mRemoteFirstVideoTrack, mFirstVideoSwitch);
        boolean hasTrack = mRemoteFirstVideoTrack != null;
        mFirstEditTextX.setEnabled(hasTrack);
        mFirstEditTextY.setEnabled(hasTrack);
        mFirstEditTextZ.setEnabled(hasTrack);
        mFirstEditTextWidth.setEnabled(hasTrack);
        mFirstEditTextHeight.setEnabled(hasTrack);

        if (hasTrack) {
            QNMergeTrackOption option = mRemoteFirstVideoTrack.getQNMergeTrackOption();
            String videoTag = mRemoteFirstVideoTrack.getQNTrackInfo().getTag();
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

    private void setSecondRemoteTrack(RemoteTrack videoTrack) {
        mRemoteSecondVideoTrack = videoTrack;
        updateSwitchState(mRemoteSecondVideoTrack, mSecondVideoSwitch);
        boolean hasTrack = mRemoteSecondVideoTrack != null;
        mSecondEditTextX.setEnabled(hasTrack);
        mSecondEditTextY.setEnabled(hasTrack);
        mSecondEditTextZ.setEnabled(hasTrack);
        mSecondEditTextWidth.setEnabled(hasTrack);
        mSecondEditTextHeight.setEnabled(hasTrack);

        if (hasTrack) {
            QNMergeTrackOption option = mRemoteSecondVideoTrack.getQNMergeTrackOption();
            String videoTag = mRemoteSecondVideoTrack.getQNTrackInfo().getTag();
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

    public List<RemoteTrack> updateMergeOptions() {
        List<RemoteTrack> result = new ArrayList<>();
        if (mRemoteAudioTrack != null) {
            mRemoteAudioTrack.setTrackInclude(mAudioSwitch.isChecked());
            result.add(mRemoteAudioTrack);
        }
        if (mRemoteFirstVideoTrack != null) {
            mRemoteFirstVideoTrack.setTrackInclude(mFirstVideoSwitch.isChecked());
            QNMergeTrackOption option = mRemoteFirstVideoTrack.getQNMergeTrackOption();
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
            result.add(mRemoteFirstVideoTrack);
        }
        if (mRemoteSecondVideoTrack != null) {
            mRemoteSecondVideoTrack.setTrackInclude(mSecondVideoSwitch.isChecked());
            QNMergeTrackOption option = mRemoteSecondVideoTrack.getQNMergeTrackOption();
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
            result.add(mRemoteSecondVideoTrack);
        }
        return result;
    }
}

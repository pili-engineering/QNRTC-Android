package com.qiniu.droid.rtc.api.examples.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.model.AudioEffect;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AudioEffectAdapter extends RecyclerView.Adapter<AudioEffectAdapter.Holder> {
    private List<AudioEffect> mAudioEffects;
    private OnAudioEffectClickListener mAudioEffectClickListener;

    public interface OnAudioEffectClickListener {
        void onStartClicked(int effectID, boolean start);
        void onPauseClicked(int effectID, boolean pause);
        void onPublishClicked(int effectID, boolean publish);
    }

    public void init(List<AudioEffect> audioEffects, OnAudioEffectClickListener audioEffectClickListener) {
        mAudioEffects = audioEffects;
        mAudioEffectClickListener = audioEffectClickListener;
    }

    public void stopAll() {
        for (AudioEffect audioEffect : mAudioEffects) {
            audioEffect.setStarted(false);
            audioEffect.setPaused(false);
        }
        notifyDataSetChanged();
    }

    public void pauseAll() {
        for (AudioEffect audioEffect : mAudioEffects) {
            if (audioEffect.isStarted()) {
                audioEffect.setPaused(true);
            }
        }
        notifyDataSetChanged();
    }

    public void resumeAll() {
        for (AudioEffect audioEffect : mAudioEffects) {
            if (audioEffect.isStarted()) {
                audioEffect.setPaused(false);
            }
        }
        notifyDataSetChanged();
    }

    public void audioEffectMixFinished(int effectID) {
        for (int i = 0; i < mAudioEffects.size(); i++) {
            AudioEffect audioEffect = mAudioEffects.get(i);
            if (audioEffect.getAudioEffect().getID() == effectID) {
                audioEffect.setStarted(false);
                audioEffect.setPaused(false);
                notifyItemChanged(i);
            }
        }
    }

    @NonNull
    @NotNull
    @Override
    public Holder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audio_effect_control, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull Holder holder, int position) {
        AudioEffect audioEffect = mAudioEffects.get(position);
        holder.mEffectNameText.setText(new File(audioEffect.getAudioEffect().getFilePath()).getName());
        holder.mStartButton.setText(audioEffect.isStarted() ? R.string.stop_audio_mix : R.string.start_audio_mix);
        holder.mStartButton.setOnClickListener(v -> {
            if (mAudioEffectClickListener != null) {
                audioEffect.setStarted(!audioEffect.isStarted());
                if (!audioEffect.isStarted()) {
                    audioEffect.setPaused(false);
                }
                holder.mPauseButton.setText(audioEffect.isPaused() ? R.string.resume_audio_mix : R.string.pause_audio_mix);
                holder.mStartButton.setText(audioEffect.isStarted() ? R.string.stop_audio_mix : R.string.start_audio_mix);
                mAudioEffectClickListener.onStartClicked(audioEffect.getAudioEffect().getID(), audioEffect.isStarted());
            }
        });
        holder.mPauseButton.setText(audioEffect.isPaused() ? R.string.resume_audio_mix : R.string.pause_audio_mix);
        holder.mPauseButton.setOnClickListener(v -> {
            if (!audioEffect.isStarted()) {
                return;
            }
            if (mAudioEffectClickListener != null) {
                audioEffect.setPaused(!audioEffect.isPaused());
                holder.mPauseButton.setText(audioEffect.isPaused() ? R.string.resume_audio_mix : R.string.pause_audio_mix);
                mAudioEffectClickListener.onPauseClicked(audioEffect.getAudioEffect().getID(), audioEffect.isPaused());
            }
        });
        holder.mPublishButton.setOnClickListener(view -> {
            if (mAudioEffectClickListener != null) {
                audioEffect.setPublish(!audioEffect.isPublish());
                holder.mPublishButton.setText(audioEffect.isPublish() ? R.string.no_publish_audio_mix : R.string.publish_audio_mix);
                mAudioEffectClickListener.onPublishClicked(audioEffect.getAudioEffect().getID(), audioEffect.isPublish());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mAudioEffects.size();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        public TextView mEffectNameText;
        public Button mStartButton;
        public Button mPauseButton;
        public Button mPublishButton;

        public Holder(View view) {
            super(view);
            mEffectNameText = view.findViewById(R.id.effect_name_text);
            mStartButton = view.findViewById(R.id.start_audio_mix_button);
            mPauseButton = view.findViewById(R.id.pause_audio_mix_button);
            mPublishButton = view.findViewById(R.id.publish_audio_mix_button);
        }
    }
}

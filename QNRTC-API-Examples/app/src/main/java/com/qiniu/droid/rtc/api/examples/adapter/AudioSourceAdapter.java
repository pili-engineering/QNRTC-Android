package com.qiniu.droid.rtc.api.examples.adapter;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.qiniu.droid.rtc.api.examples.R;
import com.qiniu.droid.rtc.api.examples.model.AudioSource;

import org.jetbrains.annotations.NotNull;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AudioSourceAdapter extends RecyclerView.Adapter<AudioSourceAdapter.Holder> {

    private List<AudioSource> mAudioSources;
    private OnAudioSourceClickListener mAudioSourceClickListener;

    public interface OnAudioSourceClickListener {
        void onPublishClicked(int effectID, boolean publish);
    }

    public void init(List<AudioSource> audioSources, OnAudioSourceClickListener audioSourceClickListener) {
        mAudioSources = audioSources;
        mAudioSourceClickListener = audioSourceClickListener;
    }

    public void deinit() {
        for (AudioSource source : mAudioSources) {
            source.setStarted(false);
        }
    }

    @NonNull
    @NotNull
    @Override
    public Holder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audio_source_control, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull Holder holder, int position) {
        AudioSource audioSource = mAudioSources.get(position);
        holder.mSourceNameText.setText(audioSource.getName());
        holder.mStartButton.setText(audioSource.isStarted() ? R.string.stop_audio_mix : R.string.start_audio_mix);
        holder.mStartButton.setOnClickListener(v -> {
            audioSource.setStarted(!audioSource.isStarted());
            holder.mStartButton.setText(audioSource.isStarted() ? R.string.stop_audio_mix : R.string.start_audio_mix);
        });
        holder.mPublishButton.setOnClickListener(view -> {
            if (mAudioSourceClickListener != null) {
                audioSource.setPublish(!audioSource.isPublish());
                holder.mPublishButton.setText(audioSource.isPublish() ? R.string.no_publish_audio_mix : R.string.publish_audio_mix);
                mAudioSourceClickListener.onPublishClicked(audioSource.getSource().getID(), audioSource.isPublish());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mAudioSources.size();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        public TextView mSourceNameText;
        public Button mStartButton;
        public Button mPublishButton;

        public Holder(View view) {
            super(view);
            mSourceNameText = view.findViewById(R.id.source_name_text);
            mStartButton = view.findViewById(R.id.start_audio_mix_button);
            mPublishButton = view.findViewById(R.id.publish_audio_mix_button);
        }
    }
}

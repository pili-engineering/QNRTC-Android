package com.qiniu.droid.rtc.api.examples.model;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.qiniu.droid.rtc.QNAudioFrame;
import com.qiniu.droid.rtc.QNAudioSource;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AudioSource {
    private static final String TAG = "AudioSource";

    QNAudioSource mAudioSource;
    AudioSourceListener mSourceListener;
    boolean mIsStarted = false;
    boolean mIsPublish = true;

    File mSourceFile;
    MediaExtractor mExtractor;
    MediaCodec mCodec;
    MediaFormat mOutFormat;
    boolean mSawInputEOS;
    boolean mSawOutputEOS;

    final Object mWaitDone = new Object();
    boolean mTheadActive;

    public interface AudioSourceListener {
        void onFrameAvailable(int sourceID, QNAudioFrame frame);
    }

    public AudioSource(File sourceFile, QNAudioSource audioSource, AudioSourceListener sourceListener) {
        mSourceFile = sourceFile;
        mAudioSource = audioSource;
        mSourceListener = sourceListener;
    }

    public String getName() {
        return mSourceFile.getName();
    }

    public QNAudioSource getSource() {
        return mAudioSource;
    }

    public void setPublish(boolean publish) {
        mIsPublish = publish;
    }

    public boolean isPublish() {
        return mIsPublish;
    }

    public boolean isStarted() {
        return mIsStarted;
    }

    public void setStarted(boolean started) {
        mIsStarted = started;
        if (mIsStarted) {
            initDecoder();
            new Thread(this::doDecode).start();
        } else {
            deinitDecoder();
        }
    }

    private void initDecoder() {
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(mSourceFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        MediaFormat useFormat = null;
        String useMine = null;
        for (int trackID = 0; trackID < mExtractor.getTrackCount(); trackID++) {
            MediaFormat format = mExtractor.getTrackFormat(trackID);
            if (format.getString(MediaFormat.KEY_MIME).contains("audio/")) {
                mExtractor.selectTrack(trackID);
                useMine = format.getString(MediaFormat.KEY_MIME);
                useFormat = format;
                break;
            }
        }
        if (useFormat == null) {
            Log.i(TAG, "file " + mSourceFile + " has not valid audio format");
            return;
        }

        try {
            mCodec = MediaCodec.createDecoderByType(useMine);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        mCodec.configure(useFormat, null, null, 0);
        mExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        mCodec.start();
        Log.i(TAG, "decoder start ok");
    }

    private void deinitDecoder() {
        if (mCodec == null) {
            return;
        }
        synchronized (mWaitDone) {
            if (mTheadActive) {
                try {
                    mWaitDone.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        waitAndQueueEOS();
        waitForAllOutputs();
        mCodec.stop();
        mCodec.release();
        mExtractor.release();
        mCodec = null;
        mExtractor = null;
        mSawInputEOS = false;
        mSawOutputEOS = false;
    }

    private void doDecode() {
        synchronized (mWaitDone) {
            mTheadActive = true;
        }
        MediaCodec.BufferInfo outInfo = new MediaCodec.BufferInfo();
        while (!mSawInputEOS && mIsStarted) {
            int outputBufferId = mCodec.dequeueOutputBuffer(outInfo, 5000);
            if (outputBufferId >= 0) {
                dequeueOutput(outputBufferId, outInfo);
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mOutFormat = mCodec.getOutputFormat();
                Log.i(TAG, "source format " + mOutFormat);
            }
            int inputBufferId = mCodec.dequeueInputBuffer(5000);
            if (inputBufferId != -1) {
                enqueueInput(inputBufferId);
            }
        }
        synchronized (mWaitDone) {
            mTheadActive = false;
            mWaitDone.notifyAll();
        }
        Log.i(TAG, "doDecoder over");
    }

    private void enqueueInput(int bufferIndex) {
        ByteBuffer inputBuffer = mCodec.getInputBuffer(bufferIndex);
        int size = mExtractor.readSampleData(inputBuffer, 0);
        if (size < 0) {
            enqueueEOS(bufferIndex);
        } else {
            long pts = mExtractor.getSampleTime();
            int extractorFlags = mExtractor.getSampleFlags();
            int codecFlags = 0;
            if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                codecFlags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
            }
            mCodec.queueInputBuffer(bufferIndex, 0, size, pts, codecFlags);
            mExtractor.advance();
        }
    }

    private void dequeueOutput(int bufferIndex, MediaCodec.BufferInfo info) {
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mSawOutputEOS = true;
        }
        final ByteBuffer buffer = mCodec.getOutputBuffer(bufferIndex);
        final byte[] audio = new byte[info.size];
        buffer.clear(); // prepare buffer for reading
        buffer.get(audio);

        ByteBuffer qnbuffer = ByteBuffer.allocateDirect(info.size);
        qnbuffer.rewind();
        qnbuffer.put(audio);
        if (mSourceListener != null && mIsStarted) {
            mSourceListener.onFrameAvailable(mAudioSource.getID(),
                    new QNAudioFrame(qnbuffer, info.size, 16,
                            mOutFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                            mOutFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)));
        }
        mCodec.releaseOutputBuffer(bufferIndex, false);
    }

    private void waitForAllOutputs() {
        MediaCodec.BufferInfo outInfo = new MediaCodec.BufferInfo();
        while (!mSawOutputEOS) {
            int outputBufferId = mCodec.dequeueOutputBuffer(outInfo, 5000);
            if (outputBufferId >= 0) {
                dequeueOutput(outputBufferId, outInfo);
            }
        }
    }

    private void waitAndQueueEOS() {
        MediaCodec.BufferInfo outInfo = new MediaCodec.BufferInfo();
        while (!mSawInputEOS) {
            int outputBufferId = mCodec.dequeueOutputBuffer(outInfo, 5000);
            if (outputBufferId >= 0) {
                dequeueOutput(outputBufferId, outInfo);
            }
            int inputBufferId = mCodec.dequeueInputBuffer(5000);
            if (inputBufferId != -1) {
                enqueueEOS(inputBufferId);
            }
        }
    }

    private void enqueueEOS(int bufferIndex) {
        if (!mSawInputEOS) {
            mCodec.queueInputBuffer(bufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            mSawInputEOS = true;
        }
    }
}

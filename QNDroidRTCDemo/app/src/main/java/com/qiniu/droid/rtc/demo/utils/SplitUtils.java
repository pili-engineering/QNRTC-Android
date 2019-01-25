package com.qiniu.droid.rtc.demo.utils;

import android.util.Log;

import com.qiniu.droid.rtc.model.QNMergeTrackOption;

import java.util.ArrayList;
import java.util.List;

public class SplitUtils {

    private static final String TAG = "SplitUtils";
    private static final boolean LOG_SPLIT_POINT = false;
    private static final boolean LOG_LAYOUT_OPTIONS = false;

    /**
     * generate merge track options to fill the whole layout.
     *
     * @param count  video track count
     * @param width  merge layout width
     * @param height merge layout height
     * @return merge track options
     */
    public static List<QNMergeTrackOption> split(int count, int width, int height) {
        List<QNMergeTrackOption> layoutOptions = new ArrayList<>();
        split(layoutOptions, count, 0, 0, width, height);
        return layoutOptions;
    }

    private static void split(List<QNMergeTrackOption> layoutOptions, int count, int x, int y, int width, int height) {
        if (count == 0) {
            return;
        }
        if (count == 1) {
            QNMergeTrackOption mergeTrackOption = new QNMergeTrackOption();
            mergeTrackOption.setX(x);
            mergeTrackOption.setY(y);
            mergeTrackOption.setWidth(width);
            mergeTrackOption.setHeight(height);
            layoutOptions.add(mergeTrackOption);
            if (LOG_LAYOUT_OPTIONS) {
                Log.d(TAG, "layout ops, (x: " + x + ", y: " + y + "; width : " + width + ", height: " + height + ") + ");
            }
            return;
        }
        int splitPoint = calculateSplitPoint(count);
        boolean verticalSplit = (width > height);
        if (verticalSplit) {
            int splitWidth = width / 2;
            int splitInXPos = (x + splitWidth);
            split(layoutOptions, splitPoint, x, y, splitWidth, height);
            if (LOG_SPLIT_POINT) {
                Log.d(TAG, "split ver, (x: " + splitInXPos + ", y: " + y + "; width : " + splitWidth + ", height: " + height + ")");
            }
            split(layoutOptions, count - splitPoint, splitInXPos, y, splitWidth, height);
        } else {
            int splitHeight = height / 2;
            int splitInYPos = (y + splitHeight);
            split(layoutOptions, splitPoint, x, y, width, splitHeight);
            if (LOG_SPLIT_POINT) {
                Log.d(TAG, "split hor, (x: " + x + ", y: " + splitInYPos + "; width : " + width + ", height: " + splitHeight + ")");
            }
            split(layoutOptions, count - splitPoint, x, splitInYPos, width, splitHeight);
        }
    }

    private static int calculateSplitPoint(int count) {
        double log2 = calculateLog2(count);
        double logRound = Math.round(log2);
        double logFloor = Math.floor(log2);
        if (logRound == logFloor) {
            return count - (int) Math.pow(2, logRound - 1);
        } else {
            return (int) Math.pow(2, logRound - 1);
        }
    }

    private static double calculateLog2(int count) {
        return Math.log(count) / Math.log(2);
    }
}


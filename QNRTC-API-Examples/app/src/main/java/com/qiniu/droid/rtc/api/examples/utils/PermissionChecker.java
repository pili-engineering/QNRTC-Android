package com.qiniu.droid.rtc.api.examples.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public class PermissionChecker {
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private Activity mActivity;

    public PermissionChecker(Activity activity) {
        mActivity = activity;
    }

    /**
     * Check that all given permissions have been granted by verifying that each entry in the
     * given array is of the value {@link PackageManager#PERMISSION_GRANTED}.
     *
     * @see Activity#onRequestPermissionsResult(int, String[], int[])
     */
    private boolean verifyPermissions(int[] grantResults) {
        // At least one result must be checked.
        if (grantResults.length < 1){
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean checkPermission() {
        boolean ret = true;

        List<String> permissionsNeeded = new ArrayList<String>();
        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.CAMERA)) {
            permissionsNeeded.add("CAMERA");
        }
        if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO)) {
            permissionsNeeded.add("MICROPHONE");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!addPermission(permissionsList, Manifest.permission.READ_MEDIA_AUDIO)) {
                permissionsNeeded.add("Read media audio");
            }
        } else {
            if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionsNeeded.add("Write external storage");
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            // Need Rationale
            String message = "You need to grant access to " + permissionsNeeded.get(0);
            for (int i = 1; i < permissionsNeeded.size(); i++) {
                message = message + ", " + permissionsNeeded.get(i);
            }
            // Check for Rationale Option
            if (!mActivity.shouldShowRequestPermissionRationale(permissionsList.get(0))) {
                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mActivity.requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                            }
                        });
            }
            else {
                mActivity.requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            }
            ret = false;
        }

        return ret;
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(mActivity)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean addPermission(List<String> permissionsList, String permission) {
        boolean ret = true;
        if (mActivity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            ret = false;
        }
        return ret;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS) {
            if (verifyPermissions(grantResults)) {
                // all permissions granted
            } else {
                // some permissions denied
                ToastUtils.showShortToast(mActivity, "some permissions denied");
            }
        }
    }
}

package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class RequestPermission {

    public static boolean isRequestingPermission(Activity context, String permission) {
        if (!checkPermission(context, permission)) {
            ActivityCompat.requestPermissions(context,
                    new String[]{permission}, 0);
            return true;
        }
        return false;
    }

    public static boolean checkPermission(Context ctx, String permission) {
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean isRequestingPermissions(Activity context, String... permissions) {
        for (String permission : permissions) {
            if (isRequestingPermission(context, permission)) return true;
        }
        return false;
    }

}

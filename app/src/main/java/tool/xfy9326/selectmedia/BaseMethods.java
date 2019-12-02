package tool.xfy9326.selectmedia;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

class BaseMethods {
    static final int STORAGE_PERMISSION_REQUEST_CODE = 2;

    static long getSystemAvailableMemorySize(Context context) {
        ActivityManager mActivityManager = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if (mActivityManager != null) {
            mActivityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.availMem;
        }
        return 0;
    }

    @TargetApi(Build.VERSION_CODES.M)
    static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    static void requestStoragePermission(Activity activity) {
        activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
    }
}

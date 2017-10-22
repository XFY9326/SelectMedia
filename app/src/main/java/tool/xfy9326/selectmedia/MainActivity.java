package tool.xfy9326.selectmedia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

//Made By XFY9326
//2017-10-12

public class MainActivity extends Activity {
    private static final int RESULT_CODE = 1;
    //false:send uri  true:copy file
    private boolean file_mode = false;
    private File select_file;
    private AlertDialog load;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startSelect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == RESULT_CODE && data != null) {
                if (file_mode) {
                    loading(data);
                    saveMediaToFile(data);
                } else {
                    exit(data);
                }
            }
        } else {
            onBackPressed();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
        System.gc();
        super.onBackPressed();
    }

    //Open picture or video selector
    private void startSelect() {
        Intent base_intent = getIntent();
        Intent new_intent = new Intent();
        Uri uri = base_intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        if (uri != null) {
            file_mode = true;
            try {
                String path = UriMethod.getUriAbsolutePath(this, uri);
                select_file = new File(path);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                file_mode = false;
            }
        }
        if (base_intent.getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE) || base_intent.getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE_SECURE)) {
            new_intent.setType("image/*");
        } else if (base_intent.getAction().equals(MediaStore.ACTION_VIDEO_CAPTURE)) {
            new_intent.setType("video/*");
        }
        new_intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(new_intent, RESULT_CODE);
    }

    private void loading(final Intent asset_intent) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.loading);
        dialog.setMessage(getString(R.string.loading_msg) + select_file.getAbsolutePath());
        dialog.setCancelable(false);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                exit(asset_intent);
            }
        });
        load = dialog.show();
    }

    private void saveMediaToFile(final Intent asset_intent) {
        new Thread(new Runnable() {
            public void run() {
                Uri uri = asset_intent.getData();
                try {
                    AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(uri, "r");
                    if (afd != null && select_file != null) {
                        FileInputStream in = afd.createInputStream();
                        FileOutputStream out = new FileOutputStream(select_file);
                        byte[] buff = new byte[1024];
                        int len;
                        while ((len = in.read(buff)) > 0) {
                            out.write(buff, 0, len);
                        }
                        in.close();
                        out.flush();
                        out.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                load.cancel();
            }
        }).start();
    }

    private void exit(Intent asset_intent) {
        setResult(RESULT_OK, asset_intent);
        finish();
        System.gc();
    }

}

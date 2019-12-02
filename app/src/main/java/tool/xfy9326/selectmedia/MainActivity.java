package tool.xfy9326.selectmedia;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;

/**
 * @author xfy9326
 */

public class MainActivity extends Activity {
    private static final String MIME_IMAGE = "image/*";
    private static final String MIME_VIDEO = "video/*";
    private static final int MEDIA_SELECTOR_RESULT_CODE = 1;

    private Uri outputMediaUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BaseMethods.hasStoragePermission(this)) {
            launchMediaSelector();
        } else {
            BaseMethods.requestStoragePermission(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == MEDIA_SELECTOR_RESULT_CODE && data != null) {
                if (outputMediaUri != null) {
                    saveMediaToExtraFile(data.getData(), showFileTransferLoadingDialog(data));
                } else {
                    exitWithResult(data);
                }
            }
        } else {
            onBackPressed();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == BaseMethods.STORAGE_PERMISSION_REQUEST_CODE) {
            boolean isGrantSuccess = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    isGrantSuccess = false;
                    break;
                }
            }
            if (isGrantSuccess) {
                launchMediaSelector();
            } else {
                Toast.makeText(this, R.string.permission_grant_failed, Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
        super.onBackPressed();
    }

    private void launchMediaSelector() {
        Intent contentIntent = getIntent();
        String contentAction = contentIntent.getAction();
        this.outputMediaUri = contentIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        if (contentAction != null) {
            Intent mediaSelectIntent = new Intent(Intent.ACTION_GET_CONTENT);
            if (contentAction.equals(MediaStore.ACTION_IMAGE_CAPTURE) || contentAction.equals(MediaStore.ACTION_IMAGE_CAPTURE_SECURE)) {
                mediaSelectIntent.setType(MIME_IMAGE);
            } else if (contentAction.equals(MediaStore.ACTION_VIDEO_CAPTURE)) {
                mediaSelectIntent.setType(MIME_VIDEO);
            }
            mediaSelectIntent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(mediaSelectIntent, MEDIA_SELECTOR_RESULT_CODE);
        }
    }

    private Dialog showFileTransferLoadingDialog(final Intent resultIntent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.loading);
        builder.setMessage(getString(R.string.loading_msg, outputMediaUri.toString()));
        builder.setCancelable(false);
        builder.setOnCancelListener(dialog1 -> exitWithResult(resultIntent));
        return builder.show();
    }

    private void saveMediaToExtraFile(Uri mediaUri, Dialog loadingDialog) {
        if (mediaUri != null && outputMediaUri != null) {
            int maximumBufferSize = (int) (BaseMethods.getSystemAvailableMemorySize(this) / 10);
            if (maximumBufferSize < 1024) {
                maximumBufferSize = 1024;
            }
            new TransferMediaAsync(getContentResolver(), loadingDialog, maximumBufferSize).execute(mediaUri, outputMediaUri);
        }
    }

    private void exitWithResult(Intent resultIntent) {
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}

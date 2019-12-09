package tool.xfy9326.selectmedia;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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

    private static final String SELECT_FROM_ALBUM_COMPONENT_NAME = ".MainActivity.Album";
    private static final String SELECT_FROM_FILE_EXPLORER_COMPONENT_NAME = ".MainActivity.FileExplorer";
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
                if (data.getData() != null) {
                    if (outputMediaUri != null) {
                        saveMediaToExtraFile(data.getData(), showFileTransferLoadingDialog(data.getData()));
                    } else {
                        exitWithResult(data.getData());
                    }
                } else {
                    Toast.makeText(this, R.string.no_select_media, Toast.LENGTH_SHORT).show();
                    finish();
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
    }

    private MediaSelectType getMediaSelectType() {
        switch (getComponentName().getShortClassName()) {
            case SELECT_FROM_ALBUM_COMPONENT_NAME:
                return MediaSelectType.FROM_ALBUM;
            default:
            case SELECT_FROM_FILE_EXPLORER_COMPONENT_NAME:
                return MediaSelectType.FROM_FILE_EXPLORER;
        }
    }

    private void launchMediaSelector() {
        Intent contentIntent = getIntent();
        String contentAction = contentIntent.getAction();
        this.outputMediaUri = contentIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        if (contentAction != null) {
            Intent mediaSelectIntent = new Intent();
            MediaSelectType selectType = getMediaSelectType();
            if (selectType == MediaSelectType.FROM_ALBUM) {
                mediaSelectIntent.setAction(Intent.ACTION_PICK);
            } else if (selectType == MediaSelectType.FROM_FILE_EXPLORER) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mediaSelectIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                } else {
                    mediaSelectIntent.setAction(Intent.ACTION_GET_CONTENT);
                    mediaSelectIntent.addCategory(Intent.CATEGORY_OPENABLE);
                }
            } else {
                finish();
                return;
            }
            if (contentAction.equals(MediaStore.ACTION_IMAGE_CAPTURE) || contentAction.equals(MediaStore.ACTION_IMAGE_CAPTURE_SECURE)) {
                mediaSelectIntent.setType(MIME_IMAGE);
                if (selectType == MediaSelectType.FROM_ALBUM) {
                    mediaSelectIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                }
            } else if (contentAction.equals(MediaStore.ACTION_VIDEO_CAPTURE)) {
                mediaSelectIntent.setType(MIME_VIDEO);
                if (selectType == MediaSelectType.FROM_ALBUM) {
                    mediaSelectIntent.setData(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                }
            }
            startActivityForResult(mediaSelectIntent, MEDIA_SELECTOR_RESULT_CODE);
        }
    }

    private Dialog showFileTransferLoadingDialog(final Uri mediaUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.loading);
        builder.setMessage(getString(R.string.loading_msg, outputMediaUri.toString()));
        builder.setCancelable(false);
        builder.setOnCancelListener(dialog1 -> exitWithResult(mediaUri));
        return builder.show();
    }

    private void saveMediaToExtraFile(Uri mediaUri, Dialog loadingDialog) {
        if (mediaUri != null && outputMediaUri != null) {
            new TransferMediaAsync(getContentResolver(), loadingDialog).execute(mediaUri, outputMediaUri);
        }
    }

    private void exitWithResult(Uri mediaUri) {
        Intent intent = new Intent();
        intent.setData(mediaUri);
        setResult(RESULT_OK, intent);
        finish();
    }

    private enum MediaSelectType {
        FROM_ALBUM,
        FROM_FILE_EXPLORER
    }
}

package tool.xfy9326.selectmedia;

import android.app.Activity;
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
                        saveMediaToExtraFile(data);
                    } else {
                        exitWithResult(data);
                    }
                } else {
                    Toast.makeText(this, R.string.no_select_media, Toast.LENGTH_SHORT).show();
                    onBackPressed();
                }
            } else {
                onBackPressed();
            }
        } else {
            onBackPressed();
        }
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
                }
                mediaSelectIntent.addCategory(Intent.CATEGORY_OPENABLE);
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

    private void saveMediaToExtraFile(final Intent resultIntent) {
        if (resultIntent.getData() != null && outputMediaUri != null) {
            new TransferMediaAsync(getContentResolver(), isSuccess -> {
                if (isSuccess) {
                    exitWithResult(resultIntent);
                } else {
                    Toast.makeText(MainActivity.this, R.string.media_file_transfer_error, Toast.LENGTH_SHORT).show();
                    onBackPressed();
                }

            }).execute(resultIntent.getData(), outputMediaUri);
        }
    }

    private void exitWithResult(Intent resultIntent) {
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private enum MediaSelectType {
        FROM_ALBUM,
        FROM_FILE_EXPLORER
    }
}

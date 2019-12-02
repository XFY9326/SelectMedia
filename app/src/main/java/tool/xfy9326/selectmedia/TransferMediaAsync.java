package tool.xfy9326.selectmedia;

import android.app.Dialog;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

class TransferMediaAsync extends AsyncTask<Uri, Void, Void> {
    private static final String FILE_MODE_READ = "r";
    private static final String FILE_MODE_WRITE = "w";
    private final int maximumBufferSize;
    private final ContentResolver resolver;
    private final Dialog loadingDialog;
    private ParcelFileDescriptor inputFileDescriptor = null;
    private ParcelFileDescriptor outputFileDescriptor = null;
    private FileInputStream fileInputStream = null;
    private FileOutputStream fileOutputStream = null;

    TransferMediaAsync(@NonNull ContentResolver resolver, @NonNull Dialog loadingDialog, int maximumBufferSize) {
        this.maximumBufferSize = maximumBufferSize;
        this.resolver = resolver;
        this.loadingDialog = loadingDialog;
    }

    @Override
    protected Void doInBackground(Uri... uris) {
        if (uris.length >= 2) {
            int length;
            try {
                inputFileDescriptor = resolver.openFileDescriptor(uris[0], FILE_MODE_READ);
                outputFileDescriptor = resolver.openFileDescriptor(uris[1], FILE_MODE_WRITE);
                if (inputFileDescriptor != null && outputFileDescriptor != null) {
                    fileInputStream = new FileInputStream(inputFileDescriptor.getFileDescriptor());
                    fileOutputStream = new FileOutputStream(outputFileDescriptor.getFileDescriptor());

                    byte[] buffer = new byte[fileInputStream.available() > maximumBufferSize ? maximumBufferSize : fileInputStream.available()];

                    while ((length = fileInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, length);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    }


                    if (inputFileDescriptor != null) {
                        inputFileDescriptor.close();
                    }
                    if (outputFileDescriptor != null) {
                        outputFileDescriptor.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        closeLoadingDialog();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        closeLoadingDialog();
    }

    private void closeLoadingDialog() {
        if (loadingDialog.isShowing()) {
            loadingDialog.cancel();
        }
    }
}

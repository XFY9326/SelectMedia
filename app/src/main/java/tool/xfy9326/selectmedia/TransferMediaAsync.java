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
import java.nio.channels.FileChannel;

class TransferMediaAsync extends AsyncTask<Uri, Void, Void> {
    private static final String FILE_MODE_READ = "r";
    private static final String FILE_MODE_WRITE = "w";
    private final ContentResolver resolver;
    private final Dialog loadingDialog;

    TransferMediaAsync(@NonNull ContentResolver resolver, @NonNull Dialog loadingDialog) {
        this.resolver = resolver;
        this.loadingDialog = loadingDialog;
    }

    @Override
    protected Void doInBackground(Uri... uris) {
        if (uris.length >= 2) {
            try (ParcelFileDescriptor inputFileDescriptor = resolver.openFileDescriptor(uris[0], FILE_MODE_READ);
                 ParcelFileDescriptor outputFileDescriptor = resolver.openFileDescriptor(uris[1], FILE_MODE_WRITE)) {
                if (inputFileDescriptor != null && outputFileDescriptor != null) {
                    try (FileChannel inputChannel = new FileInputStream(inputFileDescriptor.getFileDescriptor()).getChannel();
                         FileChannel outputChannel = new FileOutputStream(outputFileDescriptor.getFileDescriptor()).getChannel()) {
                        long size = inputChannel.size();
                        while (size > 0) {
                            long count = outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                            if (count > 0) {
                                size -= count;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
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

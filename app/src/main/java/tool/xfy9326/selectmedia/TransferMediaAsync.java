package tool.xfy9326.selectmedia;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import java.nio.channels.FileChannel;
import java.util.Objects;

class TransferMediaAsync extends AsyncTask<Uri, Void, Boolean> {
    private static final String FILE_MODE_READ = "r";
    private static final String FILE_MODE_WRITE = "w";
    private final ContentResolver resolver;
    private final TransferResultCallback callback;

    TransferMediaAsync(@NonNull ContentResolver resolver, @NonNull TransferResultCallback callback) {
        this.resolver = resolver;
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Uri... uris) {
        if (uris.length >= 2) {
            Uri mediaUri = uris[0];
            Uri mediaOutputUri = uris[1];
            try (FileChannel inputChannel = Objects.requireNonNull(resolver.openAssetFileDescriptor(mediaUri, FILE_MODE_READ)).createInputStream().getChannel();
                 FileChannel outputChannel = Objects.requireNonNull(resolver.openAssetFileDescriptor(mediaOutputUri, FILE_MODE_WRITE)).createOutputStream().getChannel()) {
                long size = inputChannel.size();
                while (size > 0) {
                    long count = outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                    if (count > 0) {
                        size -= count;
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean bool) {
        callback.onFinished(bool);
    }

    interface TransferResultCallback {
        void onFinished(boolean isSuccess);
    }

}

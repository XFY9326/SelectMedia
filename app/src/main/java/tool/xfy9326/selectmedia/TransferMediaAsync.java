package tool.xfy9326.selectmedia;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import java.nio.channels.FileChannel;

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
            try (AssetFileDescriptor inputFileDescriptor = resolver.openAssetFileDescriptor(uris[0], FILE_MODE_READ);
                 AssetFileDescriptor outputFileDescriptor = resolver.openAssetFileDescriptor(uris[1], FILE_MODE_WRITE)) {
                if (inputFileDescriptor != null && outputFileDescriptor != null) {
                    try (FileChannel inputChannel = inputFileDescriptor.createInputStream().getChannel();
                         FileChannel outputChannel = outputFileDescriptor.createOutputStream().getChannel()) {
                        long size = inputChannel.size();
                        while (size > 0) {
                            long count = outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                            if (count > 0) {
                                size -= count;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
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

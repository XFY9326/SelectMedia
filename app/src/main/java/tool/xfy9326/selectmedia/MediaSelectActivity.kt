package tool.xfy9326.selectmedia

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import kotlin.concurrent.thread

class MediaSelectActivity : Activity() {
    companion object {
        private const val FILE_MODE_READ = "r"
        private const val FILE_MODE_WRITE = "w"

        private const val MIME_IMAGE = "image/*"
        private const val MIME_VIDEO = "video/*"

        private const val MEDIA_SELECT_REQUEST_CODE = 1
        private const val STORAGE_PERMISSION_REQUEST_CODE = 2

        enum class MediaSelectType {
            IMAGE,
            VIDEO
        }
    }

    private var outputUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkStoragePermission()) {
            selectMedia()
        } else {
            requestStoragePermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == MEDIA_SELECT_REQUEST_CODE && data != null) {
            val inputUri = data.data
            if (inputUri != null) {
                val output = outputUri
                if (output == null) {
                    exitWithIntent(data)
                } else {
                    transferMedia(inputUri, output, data)
                }
            } else {
                Toast.makeText(this, R.string.no_media_selected, Toast.LENGTH_SHORT).show()
                onBackPressed()
            }
        } else {
            onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (PackageManager.PERMISSION_DENIED in grantResults) {
                Toast.makeText(this, R.string.storage_permission_grant_failed, Toast.LENGTH_SHORT).show()
                onBackPressed()
            } else {
                selectMedia()
            }
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    private fun checkStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_REQUEST_CODE)
        }
    }

    private fun selectMedia() {
        outputUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)

        Intent().apply {
            action = Intent.ACTION_OPEN_DOCUMENT
            addCategory(Intent.CATEGORY_OPENABLE)

            type = when (getMediaSelectType()) {
                MediaSelectType.IMAGE -> MIME_IMAGE
                MediaSelectType.VIDEO -> MIME_VIDEO
            }
        }.let {
            startActivityForResult(Intent.createChooser(it, getString(R.string.app_name)), MEDIA_SELECT_REQUEST_CODE)
        }
    }

    private fun transferMedia(inputUri: Uri, outputUri: Uri, originIntent: Intent) {
        thread {
            runCatching {
                contentResolver.openAssetFileDescriptor(inputUri, FILE_MODE_READ)?.createInputStream()?.channel?.use { inputChannel ->
                    contentResolver.openAssetFileDescriptor(outputUri, FILE_MODE_WRITE)?.createOutputStream()?.channel?.use { outputChannel ->
                        var size = inputChannel.size()
                        while (size > 0) {
                            val count = outputChannel.transferFrom(inputChannel, 0, inputChannel.size())
                            if (count > 0) {
                                size -= count
                            }
                        }
                    }
                }
            }.onSuccess {
                exitWithIntent(originIntent)
            }.onFailure {
                runOnUiThread {
                    Toast.makeText(this, R.string.media_transfer_failed, Toast.LENGTH_SHORT).show()
                    onBackPressed()
                }
            }
        }
    }

    private fun exitWithIntent(intent: Intent) {
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun getMediaSelectType() =
        when (intent.action) {
            MediaStore.ACTION_IMAGE_CAPTURE,
            MediaStore.ACTION_IMAGE_CAPTURE_SECURE,
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA,
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE -> MediaSelectType.IMAGE
            MediaStore.ACTION_VIDEO_CAPTURE,
            MediaStore.INTENT_ACTION_VIDEO_CAMERA -> MediaSelectType.VIDEO
            null -> error("Null media select action!")
            else -> error("Unknown media select action ${intent.action}!")
        }
}
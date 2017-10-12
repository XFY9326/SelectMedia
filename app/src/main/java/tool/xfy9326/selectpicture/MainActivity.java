package tool.xfy9326.selectpicture;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

//Made By XFY9326
//2017-10-12

public class MainActivity extends Activity 
{
	private static final int RESULT_CODE = 1;
	private static final int MSG = 2;
	private boolean file_mode = false;
	private File file;
	private int file_type = 0;
	private AlertDialog load;
	private Intent asset_intent;
	private Handler process = new Handler(){

		@Override
		public void handleMessage(Message msg)
		{
			if (msg.what == MSG)
			{
				load.cancel();
				setResult(RESULT_OK, asset_intent);
				finish();
			}
			super.handleMessage(msg);
		}

	};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		getData();
		startSelect();
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == RESULT_CODE)
		{
			if (data != null)
			{
				asset_intent = data;
				if (file_mode)
				{
					loading();
					if (file_type == 0)
					{
						savePicToFile();
					}
					else if (file_type == 1)
					{
						saveRecToFile();
					}
				}
				else
				{
					setResult(RESULT_OK, data);
					finish();
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void getData()
	{
		Intent intent = getIntent();
		Uri uri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
		if (uri != null)
		{
			file_mode = true;
			file = new File(UriMethod.getUriAbsolutePath(this, uri));
		}
		if (intent.getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE) || intent.getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE_SECURE))
		{
			file_type = 0;
		}
		else if (intent.getAction().equals(MediaStore.ACTION_VIDEO_CAPTURE))
		{
			file_type = 1;
		}
	}

	private void startSelect()
	{
		Intent intent = new Intent();
		if (file_type == 0)
		{
			intent.setType("image/*");
		}
		else if (file_type == 1)
		{
			intent.setType("video/*");
		}
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(intent, RESULT_CODE);
	}

	private void loading()
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(R.string.loading);
		dialog.setMessage(getString(R.string.loading_msg) + file.getAbsolutePath().toString());
		dialog.setCancelable(false);
		load = dialog.show();
	}

	private void savePicToFile()
	{
		Thread t = new Thread(new Runnable(){
				public void run()
				{
					Uri uri = asset_intent.getData();
					try
					{
						Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
						if (file != null)
						{
							bitmap = rotaingImageView(readPictureDegree(UriMethod.getUriAbsolutePath(MainActivity.this, uri)), bitmap);
							FileOutputStream out = new FileOutputStream(file);
							bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
							out.flush();
							out.close();
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					process.sendEmptyMessage(MSG);
				}
			});
		t.start();
	}

	private void saveRecToFile()
	{
		Thread t = new Thread(new Runnable(){
				public void run()
				{
					Uri uri = asset_intent.getData();
					try
					{
						AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(uri, "r");
						if (file != null)
						{
							FileInputStream in = afd.createInputStream();
							FileOutputStream out = new FileOutputStream(file);
							byte[] buff = new byte[1024];
							int len;
							while ((len = in.read(buff)) > 0)
							{
								out.write(buff, 0, len);
							}
							in.close();
							out.flush();
							out.close();
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					process.sendEmptyMessage(MSG);
				}
			});
		t.start();
	}

	private static int readPictureDegree(String path)
	{
        int degree  = 0;
        try
		{
			ExifInterface exifInterface = new ExifInterface(path);
			int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			switch (orientation)
			{
                case ExifInterface.ORIENTATION_ROTATE_90:
					degree = 90;
					break;
                case ExifInterface.ORIENTATION_ROTATE_180:
					degree = 180;
					break;
                case ExifInterface.ORIENTATION_ROTATE_270:
					degree = 270;
					break;
			}
        }
		catch (IOException e)
		{
			e.printStackTrace();
        }
        return degree;
	}

	private static Bitmap rotaingImageView(int angle , Bitmap bitmap)
	{
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
	}

}

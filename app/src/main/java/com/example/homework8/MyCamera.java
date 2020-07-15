package com.example.homework8;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.PathUtils;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyCamera extends AppCompatActivity {

    private Camera mCamera;
    private Button mPicBtn;
    private Button mVideoBtn;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private ImageView mImageView;
    private VideoView mVideoView;
    private Camera.PictureCallback mPicktureCallback;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording = false;
    private String mp4Path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("自定义录制");
        setContentView(R.layout.activity_my_camera);

        initButton();
        initCamera();
        initClick();
    }

    private void initButton(){
        mPicBtn = findViewById(R.id.takePic);
        mVideoBtn = findViewById(R.id.takeVideo);
        mSurfaceView = findViewById(R.id.mCameraSurf);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(new PlayerCallBack());
        mImageView = findViewById(R.id.cameraImgView);
        mVideoView = findViewById(R.id.cameraVideoView);
    }

    private void initCamera(){
        mCamera = Camera.open();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.set("orientation","portrait");
        parameters.set("rotation",90);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);

        mPicktureCallback = new Camera.PictureCallback(){
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream fos = null;
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
                Date curDate = new Date(System.currentTimeMillis());
                String str = formatter.format(curDate);
                String filePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath()+ File.separator+str+".jpg";
                File file = new File(filePath);
                try {
                    fos = new FileOutputStream(file);
                    fos.write(data);
                    fos.flush();
                    //最后显示图片
                    int targetWidth = mImageView.getWidth();
                    int targetHeight = mImageView.getHeight();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(filePath,options);
                    int photoWidth = options.outWidth;
                    int photoHeight = options.outHeight;
                    int scaleFactor = Math.min(photoWidth/targetWidth,photoHeight/targetHeight);
                    options.inJustDecodeBounds = false;
                    options.inSampleSize =scaleFactor;
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
                    Bitmap rotationBitmap = rotateImage(bitmap, filePath);
                    mImageView.bringToFront();
                    mImageView.setVisibility(View.VISIBLE);
                    mVideoView.setVisibility(View.GONE);
                    mImageView.setImageBitmap(rotationBitmap);
                } catch (Exception e){
                    e.printStackTrace();
                }finally {
                    mCamera.startPreview();
                    if(fos != null){
                        try {
                            fos.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    //旋转图片的函数
    private Bitmap rotateImage(Bitmap bitmap, String path){
        try {
            ExifInterface srcExif = new ExifInterface(path);
            Matrix matrix = new Matrix();
            int angle = 0;
            int orientation = srcExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:{
                    angle = 90;
                    break;
                }
                case ExifInterface.ORIENTATION_ROTATE_180:{
                    angle = 180;
                    break;
                }
                case ExifInterface.ORIENTATION_ROTATE_270:{
                    angle = 270;
                    break;
                }
                default:
                    break;
            }
            matrix.postRotate(angle);
            return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    private class PlayerCallBack implements SurfaceHolder.Callback{
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            try {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            if(surfaceHolder.getSurface() == null){
                return;
            }
            mCamera.stopPreview();
            try {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    //点击按钮
    private void initClick(){
        mPicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.takePicture(null, null, mPicktureCallback);
            }
        });

        mVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isRecording){
                    mVideoBtn.setText("录制");
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                    mMediaRecorder.release();
                    mMediaRecorder = null;
                    mCamera.lock();

                    mImageView.setVisibility(View.GONE);
                    mVideoView.bringToFront();
                    mVideoView.setVisibility(View.GONE);
                    mVideoView.setVisibility(View.VISIBLE);
                    mVideoView.setVideoPath(mp4Path);
                    mVideoView.start();
                }
                else {
                    if(prepareVideoRecorder()){
                        mVideoBtn.setText("暂停");
                        mMediaRecorder.start();
                    }
                }
                isRecording = !isRecording;
            }
        });
    }


    //生命周期
    @Override
    protected void onResume() {
        super.onResume();
        if(mCamera == null){
            initCamera();
        }
        mCamera.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.stopPreview();
    }


    //录制操作
    private boolean prepareVideoRecorder(){
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        //获得相应文件路径
        File mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir, "VIDEO_"+timeStamp+".mp4");
        if(!mediaFile.exists()){
            mediaFile.getParentFile().mkdir();
        }
        mp4Path = mediaFile.getAbsolutePath();
        Log.i("the path is: ",mp4Path);
        mMediaRecorder.setOutputFile(mp4Path);
        mMediaRecorder.setPreviewDisplay(mHolder.getSurface());
        mMediaRecorder.setOrientationHint(90);
        try {
            mMediaRecorder.prepare();
        }catch (Exception e){
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            return false;
        }
        return true;
    }
}

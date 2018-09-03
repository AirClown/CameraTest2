package com.example.yushichao.cameratest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by yushi on 2018/9/3.
 */

public class MyCamera2 {
    private android.hardware.Camera camera;

    private ImageView iv1,iv2;
    private SurfaceView surfaceView;
    private SurfaceHolder holder;

    private Activity activity;

    private boolean TAKING=false;

    private int count=0;

    public MyCamera2(Activity activity,SurfaceView surfaceView,ImageView iv1,ImageView iv2){
        this.activity=activity;
        this.surfaceView=surfaceView;
        this.iv1=iv1;
        this.iv2=iv2;
    }

    public void openCamera(){
        holder=surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                StartCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    private void StartCamera(){
        try {
            camera= android.hardware.Camera.open();
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(90);
            camera.setPreviewCallback(previewCallback);
            camera.startPreview();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void TakePhoto(){
        if (!TAKING&&camera!=null) {
            TAKING=true;
            camera.takePicture(null, null, pictureCallback);
        }
    }

    private Camera.PictureCallback pictureCallback=new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Camera.Size size = camera.getParameters().getPreviewSize();
//            try {
////                YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
////                if (image != null) {
////                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
////                    image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
////                    Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
////                    iv2.setImageBitmap(bitmap);
////                    iv2.setRotationX(iv2.getWidth());
////                    iv2.setRotationY(iv2.getHeight());
////                    iv2.setRotation(90);
////                    Log.e("getPhoto2", System.currentTimeMillis() + "," + bitmap.getWidth()+"*"+bitmap.getHeight());
////                    stream.close();
////                }
//
//                //获得图片的像素
//                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                iv2.setImageBitmap(bitmap);
//            } catch (Exception ex) {
//                Log.e("Sys", "Error:" + ex.getMessage());
//            }
            camera.stopPreview();
            camera.release();
            camera=null;
            StartCamera();
            TAKING=false;

            Log.e("10second",count+","+size.width+"*"+size.height);
            count=0;
        }
    };

    private android.hardware.Camera.PreviewCallback previewCallback=new android.hardware.Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
//            Camera.Size size = camera.getParameters().getPreviewSize();
//            try {
//                YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
//                if (image != null) {
//                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                    image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
//                    Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
//                    iv1.setImageBitmap(bitmap);
//                    iv1.setRotationX(iv1.getWidth());
//                    iv1.setRotationY(iv1.getHeight());
//                    iv1.setRotation(90);
//                    //Log.e("getPhoto", System.currentTimeMillis() + "," + bitmap.getWidth()+"*"+bitmap.getHeight());
//                    stream.close();
//                }
//            } catch (Exception ex) {
//                Log.e("Sys", "Error:" + ex.getMessage());
//            }
            count++;
        }
    };
}

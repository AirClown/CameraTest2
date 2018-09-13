package com.example.yushichao.cameratest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by yushi on 2018/9/3.
 */

public class MyCamera1 {
    private Camera camera;

    private ImageView iv1,iv2;
    private TextView tv;
    private SurfaceView surfaceView;
    private SurfaceHolder holder;

    private Activity activity;

    private boolean TAKING=false;

    public MyCamera1(Activity activity, SurfaceView surfaceView, ImageView iv1, ImageView iv2, TextView tv){
        this.activity=activity;
        this.surfaceView=surfaceView;
        this.iv1=iv1;
        this.iv2=iv2;
        this.tv=tv;
        iv2.setVisibility(View.GONE);
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
            camera= Camera.open();

            Camera.Parameters parameters=camera.getParameters();
            parameters.set("zsl","off");
            parameters.set("auto-exposure-lock","true");
            parameters.set("hw-professional-mode","on");

            //ISO
            parameters.set("hw-sensor-iso","1600");
            //快门
            parameters.set("hw-sensor-exposure-time","0.0008");
            List<Camera.Size> list= parameters.getSupportedPictureSizes();
            Camera.Size max=null;
            for(int i=0,x=0;i<list.size();i++){
                if(x<list.get(i).width){
                    max=list.get(i);
                    x=max.width;
                }
            }
            parameters.setPictureSize(max.width,max.height);
            camera.setParameters(parameters);

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
            try {

                //SavePicture(data);
                Log.e("Size",data.length+"");
                //获得图片的像素
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                bitmap=rotateBitmap(bitmap,90);

                getXY(turnToGrey(bitmap),bitmap.getWidth(),bitmap.getHeight());

                iv1.setImageBitmap(bitmap);
            } catch (Exception ex) {
                Log.e("Sys", "Error:" + ex.getMessage());
            }
            RestartCamera();
            TAKING=false;
        }
    };

    //彩色图转换bitmap
    private byte[] turnToGrey(Bitmap bitmap){
        int width=bitmap.getWidth();
        int height=bitmap.getHeight();

        int[] pixels=new int[width*height];
        bitmap.getPixels(pixels,0,width,0,0,width,height);
        int alpha = 0xFF << 24;
        final byte[] b=new byte[pixels.length];
        for(int i = 0; i < height; i++)  {
            for(int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                int red = ((grey  & 0x00FF0000 ) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
                b[width * i + j]=(byte) (grey & 0xFF);
            }
        }
        //bitmap.recycle();
        return b;
    }

    private Bitmap rotateBitmap(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        // 围绕原地进行旋转
        origin = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        return origin;
    }

    private Camera.PreviewCallback previewCallback=new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
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
        }
    };

    private void RestartCamera(){
        camera.stopPreview();
        camera.release();
        camera=null;
        StartCamera();
    }

    private void SavePicture(byte[] bytes){
        String addr=activity.getExternalFilesDir(null)+"/";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        addr+=  formatter.format(System.currentTimeMillis());
        File file=new File(addr+".jpg");

        try{
            FileOutputStream output = new FileOutputStream(file);
            output.write(bytes);
            Toast.makeText(activity, "图片保存在："+addr, Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private void getXY(byte[] data,int width,int height){
        int XY=0;

        //
        tv.setText("Text"+System.currentTimeMillis());
    }
}

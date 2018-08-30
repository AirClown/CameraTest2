package com.example.yushichao.cameratest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class CameraControl {

    //相机的方向
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //相机所需组件
    private CameraManager manager;
    private CameraDevice camera;
    private CameraCharacteristics characteristics;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequest;
    private CaptureRequest.Builder captureRequestBuilder;

    //Ui控件
    private SurfaceHolder holder;
    private SurfaceView surfaceView;
    private ImageView iv;
    private ImageReader reader;

    //相机线程
    private Handler handler;
    private HandlerThread background;

    //相机的预览和拍摄尺寸
    private Size picturesize;
    private Size previewsize;

    private Activity context;
    private boolean TakePictureLock;

    public float maxPower;//linds
    public long maxTime;
    public boolean inLedArea;
    private float[] offsetPos = new float[2];

    public CameraControl(Activity context, ImageView iv, SurfaceView surfaceView){
        this.context=context;
        this.iv=iv;                       //显示图片
        this.surfaceView=surfaceView;     //预览

        timer=new Timer();
        //初始化相机参数
        manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            //获得相机后置摄像头的信息
            characteristics = manager.getCameraCharacteristics(""+CameraCharacteristics.LENS_FACING_FRONT);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            //此处获取预览尺寸和图像尺寸
            picturesize = map.getOutputSizes(MediaRecorder.class)[4];
            previewsize=map.getOutputSizes(MediaRecorder.class)[12];//大小随意

            AvePixels=new float[3][picturesize.getWidth()];
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void StartCamera(){
        //开启线程
        startBackgroundThread();
        TakePictureLock=true;

        //配置surfaceView的界面尺寸，也可以不管
        RelativeLayout.LayoutParams params  =
                new RelativeLayout.LayoutParams(previewsize.getWidth(), previewsize.getHeight());
        params.leftMargin = 0;
        params.topMargin  = 0;
        surfaceView.setLayoutParams(params);

        //对surfanceView进行配置
        holder=surfaceView.getHolder();
        holder.setKeepScreenOn(true);
        holder.addCallback(new SurfaceHolder.Callback(){
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //surface准备完毕，开启相机
                OpenCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                OpenCamera();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.e("pass","surfaceDestroy");
            }
        });
    }

    public void StopCamera(){

        TakePictureLock=false;
        StopTakePicture();
        stopBackgroundThread();
    }

    private void OpenCamera(){

        //初始化ImageReader，设置获取相片的格式和大小
        reader= ImageReader.newInstance(picturesize.getWidth(),picturesize.getHeight()
                , ImageFormat.JPEG,1);
        reader.setOnImageAvailableListener(new ImageListener(),new Handler(Looper.getMainLooper()));//new Handler(Looper.getMainLooper())

        //打开摄像头
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // 权限未被授予
            Toast.makeText(context,"相机权限未开启", Toast.LENGTH_SHORT).show();
        } else {
            //已获取权限
            try {
                //利用openCamera来打开镜头
                Log.e("pass","100");
                if (context.checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) {
                    manager.openCamera("" + CameraCharacteristics.LENS_FACING_FRONT,
                            mstateCallback, handler);
                }
            }catch (CameraAccessException e){
                e.printStackTrace();
            }
        }
    }

    private CameraDevice.StateCallback mstateCallback=new
            CameraDevice.StateCallback() {
                @Override
                public void onOpened(final CameraDevice camera0) {
                    camera = camera0;
                    try {
                        //为相机配置surface和CameraCaptureSession
                        camera.createCaptureSession(Arrays.asList(holder.getSurface(),reader.getSurface()), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                captureSession=session;
                                //配置成功，开始预览
                                UpdatePreview();
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                                Toast.makeText(context,"配置失败", Toast.LENGTH_SHORT)
                                        .show();
                                OpenCamera();
                            }
                        }, handler);

                    }catch (CameraAccessException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    OpenCamera();
                }

                @Override
                public void onError(CameraDevice camera, int error) {

                }
            };

    //预览
    private void UpdatePreview(){
        final CameraCaptureSession mcaptureSession=captureSession;
        try {
            if (previewRequest==null) {
                // 创建预览需要的CaptureRequest.Builder
                previewRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                // 将SurfaceView的surface作为CaptureRequest.Builder的目标
                previewRequest.addTarget(holder.getSurface());
                // 自动对焦
                previewRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 关闭自动曝光
                previewRequest.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);

                Long max3 = characteristics.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION);
                Range<Integer> range1 = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);

                //Toast.makeText(context,""+range1.getUpper(),Toast.LENGTH_SHORT).show();

                previewRequest.set(CaptureRequest.SENSOR_SENSITIVITY,range1.getUpper());//
                previewRequest.set(CaptureRequest.SENSOR_EXPOSURE_TIME,83333L );//1/1200L
                previewRequest.set(CaptureRequest.SENSOR_FRAME_DURATION, max3 / 600);

            }
            // 显示预览
            CaptureRequest mpreviewRequest =previewRequest.build();
            mcaptureSession.setRepeatingRequest(mpreviewRequest, null, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.i("MainActivity","UpdataFail");
        }
    }

    //拍照，rotation 来自 "getWindowManager().getDefaultDisplay().getRotation()"
    public void TakePicture(int rotation) {
        if (camera == null&&captureSession==null&&TakePictureLock) {
            Toast.makeText(context,"设备还未准备就绪", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        //配置captureRequestBulder
        try {
            if (captureRequestBuilder==null) {

                captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);

                Long max = characteristics.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION);
                Range<Integer> range1 = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);

                captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, range1.getUpper());  //取ISO最大值
                captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 83333L);          //快门设置为1s/1200，单位为ns
                captureRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, max / 600);     //临时设置的参数，可调整

                captureRequestBuilder.addTarget(reader.getSurface());
            }
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            CaptureRequest mCaptureRequest1 = captureRequestBuilder.build();
            captureSession.capture(mCaptureRequest1,null,handler);
            /*
            CaptureRequest mCaptureRequest2 = captureRequestBuilder.build();
            CaptureRequest mCaptureRequest3= captureRequestBuilder.build();
            //开始拍照
            List<CaptureRequest> list=new ArrayList<>();
            list.add(mCaptureRequest1);
            list.add(mCaptureRequest2);
            list.add(mCaptureRequest3);
            captureSession.captureBurst(list,null,handler);
            */
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        background = new HandlerThread("CameraBackground");
        background.start();
        handler = new Handler(background.getLooper());
    }

    private void stopBackgroundThread() {
        background.quitSafely();
        try {
            background.join();
            background = null;
            handler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ImageListener implements ImageReader.OnImageAvailableListener{

        private Bitmap bitmap;

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image =reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes= new byte[buffer.remaining()];
            buffer.get(bytes);//由缓冲区存入字节数组

            //获得图片的像素
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);


            bitmap.recycle();
            image.close();
        }
    }

    private float[][] AvePixels;
    private int pixelcount;
    private boolean PhotoLock;
    private boolean PixelLock;

    private int Rotation;
    private int LightSign;

    private Timer timer;
    private TimerTask task;
    public void StartTakePicture(){
        Rotation=0;
        LightSign=0;
        pixelcount=0;
        PhotoLock=true;
        PixelLock=true;

        task=new TimerTask() {
            @Override
            public void run() {
                if (PhotoLock){
                    handler0.sendEmptyMessage(0);
                    TakePicture(Rotation);
                }
            }
        };
        timer.schedule(task,5000,1000);
    }

    private void StopTakePicture(){
        if (task!=null) {
            task.cancel();
            task = null;
        }
    }

    private Handler handler0=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    Rotation=context.getWindowManager().getDefaultDisplay().getRotation();
                    break;
                case 1:

                    break;
            }
        }
    };
}

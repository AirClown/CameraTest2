package com.example.yushichao.cameratest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private SurfaceView surfaceView;
    private Button bt;

    private ImageView iv1,iv2;
    private TextView tv;
    private MyCamera1 camera;

    private Timer timer;
    private TimerTask task;
    private Handler handler;

    private ImageView iv;

    private boolean record=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if(!hasPermissionsGranted(PERMISSIONS)) {
            if(Build.VERSION.SDK_INT>24) {
                requestPermissions(PERMISSIONS, 1);
            }
        }

        Init();
    }

    private void Init(){
        iv1=(ImageView)findViewById(R.id.imageView);
        iv2=(ImageView)findViewById(R.id.imageView2);
        tv=(TextView)findViewById(R.id.textView);

        bt=(Button)findViewById(R.id.button);
        surfaceView=(SurfaceView)findViewById(R.id.surfaceView);

        camera=new MyCamera1(this,surfaceView,iv1,iv2,tv);
        camera.openCamera();

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.TakePhoto();
            }
        });

//        task=new TimerTask() {
//            @Override
//            public void run() {
//                camera.TakePhoto();
//            }
//        };
//        timer=new Timer();
//        timer.schedule(task,2000,10000);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(flag);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length == PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this,"请给权限",Toast.LENGTH_SHORT);
                        return;
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT>24) {
                    requestPermissions(permissions, requestCode);
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (Build.VERSION.SDK_INT>24&&this.checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}

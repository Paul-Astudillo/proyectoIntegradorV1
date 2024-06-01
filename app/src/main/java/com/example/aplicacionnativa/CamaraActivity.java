package com.example.aplicacionnativa;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import android.Manifest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.opencv.imgproc.Imgproc;

import java.util.Collections;
import java.util.Currency;
import java.util.List;

public class CamaraActivity extends CameraActivity {
    private CameraBridgeViewBase cameraBridgeViewBase;
    private Mat mframe;
    private Mat rotarimagen;
    private ImageView imageView;
    private Button btnCapturar;
    private TextView fpsTextView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private long frameCount = 0;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camara);


        getPermission();
        cameraBridgeViewBase=findViewById(R.id.cameraView);
        imageView=findViewById(R.id.imageView);
        fpsTextView = findViewById(R.id.fpsTextView);

        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                mframe= new Mat();
                startTime = System.currentTimeMillis();
                handler.post(updateFpsRunnable);

            }

            @Override
            public void onCameraViewStopped() {
                mframe.release();
                handler.removeCallbacks(updateFpsRunnable);

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                mframe = inputFrame.rgba();
                frameCount++;
                return mframe;
            }
        });


        btnCapturar =findViewById(R.id.button2);
        btnCapturar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();

                long addr = rotarimagen.getNativeObjAddr();
                Intent intent = new Intent(CamaraActivity.this, MainActivity.class);
                intent.putExtra( "myImg", addr );
                Log.e("Matriz", "ENTROOOO");
                startActivity( intent );

            }
        });

        if(OpenCVLoader.initDebug()){
            cameraBridgeViewBase.enableView();
        }
        }

    private Runnable updateFpsRunnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;
            if (elapsedTime > 0) {
                int fps = (int) (frameCount / (elapsedTime / 1000.0));
                fpsTextView.setText(String.format("FPS: %d", fps));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraBridgeViewBase.enableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraBridgeViewBase.disableView();
    }

    void getPermission(){
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA
            }, 101);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0]!= PackageManager.PERMISSION_GRANTED){
            getPermission();
        }
    }

    private void captureImage() {
        if (mframe != null && !mframe.empty()) {
            // romatos la imagen
            rotarimagen = new Mat();
            Core.rotate(mframe, rotarimagen, Core.ROTATE_90_CLOCKWISE);

            // cambiamos la escala de la imagen
            int scaleFactor = 2;
            int newWidth = rotarimagen.cols() * scaleFactor;
            int newHeight = rotarimagen.rows() * scaleFactor;
            Mat scaledMat = new Mat();
            Imgproc.resize(rotarimagen, scaledMat, new org.opencv.core.Size(newWidth, newHeight));

            //bitmap del tamaño de la imagen escalada
            Bitmap bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(scaledMat, bitmap);

            // bitmap en el ImageView
            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            Toast.makeText(this, "Error al capturar la imagen", Toast.LENGTH_SHORT).show();
        }
    }



    }


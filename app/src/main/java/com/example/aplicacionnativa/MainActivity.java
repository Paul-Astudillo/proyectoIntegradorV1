package com.example.aplicacionnativa;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aplicacionnativa.databinding.ActivityMainBinding;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import android.Manifest;


public class MainActivity extends AppCompatActivity{

    // Used to load the 'aplicacionnativa' library on application startup.
    static {
        System.loadLibrary("aplicacionnativa");
    }

    //camara
    private Mat mframe;
    private ImageView imageView;

    private ActivityMainBinding binding;

    private android.widget.Button botonCombinarFuego ,btnCapturar  , btnEnviar , btnQuitar ;
    private android.widget.ImageView original, fuego , resultado  , noFondo;

    private EditText ip ;
    private TextView textViewSystemInfo;
    private Handler handler;
    private Runnable runnable;


    private static final int REQUEST_CODE_PERMISSIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        textViewSystemInfo = findViewById(R.id.textViewSystemInfo);
        handler = new Handler(Looper.getMainLooper());

        runnable = new Runnable() {
            @Override
            public void run() {
                updateSystemInfo();
                handler.postDelayed(this, 1000); // Actualizar cada segundo
            }
        };

        handler.post(runnable);
        //  Quitar fondo
        noFondo= findViewById(R.id.imageView5);
        btnQuitar = findViewById(R.id.button6);
        btnQuitar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bitmap tomada = ((BitmapDrawable) original.getDrawable()).getBitmap();

                Bitmap resultado = Bitmap.createBitmap(tomada.getWidth(), tomada.getHeight(), Bitmap.Config.ARGB_8888);

                quitarFondo(tomada, resultado);

                noFondo.setImageBitmap(resultado);

            }
        });

//Fuego con el AND
        original = findViewById(R.id.imageView);
        fuego=findViewById(R.id.imageView2);


        resultado=findViewById(R.id.imageView3);

        botonCombinarFuego = findViewById(R.id.button);
        botonCombinarFuego.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //tv.setText(stringFromJNI());


                Bitmap sinFondo = ((BitmapDrawable) noFondo.getDrawable()).getBitmap();
                Bitmap trasformacion = ((BitmapDrawable) fuego.getDrawable()).getBitmap();

                Bitmap resultadoBitmap = Bitmap.createBitmap(sinFondo.getWidth(), sinFondo.getHeight(), sinFondo.getConfig());

                // metodo fuego
                fuego(sinFondo, trasformacion, resultadoBitmap);

                resultado.setImageBitmap(resultadoBitmap);



            }
        });

//capturar imagen
        btnCapturar=findViewById(R.id.button3);
        btnCapturar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,CamaraActivity.class);
                startActivity(intent);
            }
        });


        long frameAddress = getIntent().getLongExtra("myImg", 0);

        // Verificar si la dirección de memoria es válida y crear la matriz Mat correspondiente
        if (frameAddress != 0) {
            mframe = new Mat(frameAddress);

            Log.e("Matriz", "SII+++++++++++");

            captureImage();

        } else {
            // Manejar el caso en el que la dirección de memoria es nula
            Log.e("Matriz", "La dirección de memoria es nula");
        }

        // Solicita permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
            }
        }


//enviar

        btnEnviar =findViewById(R.id.button5);
        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ip = findViewById(R.id.editTextText);
                String editTextIP = ip.getText().toString();

                Bitmap bitmap = ((BitmapDrawable) resultado.getDrawable()).getBitmap();
               guardarImagenPNG(bitmap);

//Necesitamos un hilo secundario ya que no se puede ejecutar en el hilo principal
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String serverURL = "http://" + editTextIP + ":5000/recepcion";
                        String rutaImagen = getExternalFilesDir(null) + "/image.png";
                        Log.e("Matriz", serverURL);
                        try {
                            URL url = new URL(serverURL);
                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
                            conexion.setRequestMethod("POST");
                            conexion.setDoOutput(true);
                            conexion.setRequestProperty("Content-Type", "image/png");

                            OutputStream outputStream = conexion.getOutputStream();
                            FileInputStream fileInputStream = new FileInputStream(new File(rutaImagen));

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }

                            fileInputStream.close();
                            outputStream.flush();
                            outputStream.close();

                            final int responseCode = conexion.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                Log.e("Matriz", "Imagen enviada correctamente");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Imagen enviada correctamente", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Log.e("Matriz", "Error al enviar la imagen, código: " + responseCode);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Error al enviar la imagen, código: " + responseCode, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Error al enviar la imagen", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();





            }
        });


        //tv.setText(stringFromJNI());
    }

    /**
     * A native method that is implemented by the 'aplicacionnativa' native library,
     * which is packaged with this application.
     */

    private void captureImage() {
        if (mframe != null && !mframe.empty()) {

            Bitmap bitmap = Bitmap.createBitmap(mframe.cols(), mframe.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mframe, bitmap);

            original.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "Error al capturar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

//guardar imagen
    private void guardarImagenPNG(Bitmap imagen) {
        Bitmap bitmap = imagen;
        try {
            File file = new File(getExternalFilesDir(null), "image.png");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            Toast.makeText(this, "Imagen guardada en: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.e("Matriz", "Imagen Guardada <><><><><><><><<><><>");
            Log.e("Matriz", "Imagen Guardada en: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si Permisos
            } else {
                Toast.makeText(this, "Permisos no concedidos", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void updateSystemInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memoryInfo);
        long availableMemory = memoryInfo.availMem / 1048576L; // MB
        long totalMemory = memoryInfo.totalMem / 1048576L; // MB

        Debug.MemoryInfo appMemoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(appMemoryInfo);
        int appTotalMemory = appMemoryInfo.getTotalPss() / 1024; // megaBytes

        String systemInfo = String.format("RAM: %d MB libres / %d MB totales\nMemoria App: %d MB", availableMemory, totalMemory, appTotalMemory);
        textViewSystemInfo.setText(systemInfo);
    }


    public native void fuego(android.graphics.Bitmap foto, android.graphics.Bitmap imagen, android.graphics.Bitmap resultado);

    public native void quitarFondo(android.graphics.Bitmap foto, android.graphics.Bitmap resultado);




}
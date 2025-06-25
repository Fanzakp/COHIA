package com.example.smartwaste;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts; // Pastikan ini diimpor dengan benar
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.example.smartwaste.api.RoboflowAPI;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements RoboflowAPI.ApiCallback {

    private static final String TAG = "SmarwasteApp";

    // UI Elements
    private PreviewView previewView;
    private ImageView ivResult;
    private TextView tvLiveResult;
    private Button btnCapture, btnUpload;
    private FloatingActionButton btnCloseResult;
    private ConstraintLayout cameraControls;
    private ConstraintLayout progressOverlay;
    private TextView tvProgressStatus;

    // CameraX
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;

    // Roboflow API
    private RoboflowAPI roboflowAPI;

    // State variables
    private long lastAnalysisTime = 0;
    private Bitmap originalBitmapForDetection;
    private boolean isLiveDetection = true;
    private boolean isCaptureMode = false;

    // Activity Result Launchers
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        previewView = findViewById(R.id.preview_view);
        ivResult = findViewById(R.id.iv_result);
        tvLiveResult = findViewById(R.id.tv_live_result);
        btnCapture = findViewById(R.id.btn_capture);
        btnUpload = findViewById(R.id.btn_upload);
        btnCloseResult = findViewById(R.id.btn_close_result);
        cameraControls = findViewById(R.id.camera_controls);
        progressOverlay = findViewById(R.id.progress_overlay);
        tvProgressStatus = findViewById(R.id.tv_progress_status);

        // Initialize Roboflow API
        roboflowAPI = new RoboflowAPI();
        cameraExecutor = Executors.newSingleThreadExecutor();

        setupLaunchers();
        setupClickListeners();

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (ivResult.getVisibility() == View.VISIBLE) {
                    hideResultView();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        requestCameraPermission();
    }

    private void setupLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                startCamera();
            } else {
                Toast.makeText(this, "Izin kamera ditolak.", Toast.LENGTH_LONG).show();
            }
        });

        // PERBAIKI NAMA PAKET DI SINI: ActivityResultContracts
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> { // KOREKSI: ActivityResultContracts
            if (uri != null) {
                try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                    Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream);
                    if (selectedBitmap != null) {
                        // Untuk gambar dari galeri, rotasi mungkin diperlukan
                        // Anda mungkin perlu mendapatkan orientasi EXIF dari URI untuk rotasi yang akurat
                        // Untuk sementara, coba rotasi 90 jika masih miring, atau hapus jika tidak
                        // int rotation = getImageRotationFromUri(uri); // Ini memerlukan fungsi pembantu
                        // selectedBitmap = ImageUtil.rotateBitmap(selectedBitmap, rotation);
                        processBitmapForDetection(selectedBitmap);
                    } else {
                        hideProgress();
                        Toast.makeText(this, "Gagal memuat gambar dari galeri", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Gagal membuka gambar dari URI", e);
                    hideProgress();
                }
            } else {
                hideProgress();
            }
        });
    }

    private void setupClickListeners() {
        btnCapture.setOnClickListener(v -> capturePhoto());
        btnUpload.setOnClickListener(v -> uploadImage());
        btnCloseResult.setOnClickListener(v -> hideResultView());
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

                Log.d(TAG, "Camera started successfully");
            } catch (Exception e) {
                Log.e(TAG, "Gagal memulai kamera", e);
                Toast.makeText(this, "Gagal memulai kamera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeFrame(ImageProxy imageProxy) {
        if (System.currentTimeMillis() - lastAnalysisTime < 1500) {
            imageProxy.close();
            return;
        }

        try {
            Bitmap bitmap = ImageUtil.imageToBitmap(imageProxy.getImage());
            if (bitmap != null) {
                isLiveDetection = true;
                String base64Image = BitmapUtils.toBase64(bitmap);
                roboflowAPI.detectGarbage(base64Image, this);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in analyzeFrame", e);
        } finally {
            lastAnalysisTime = System.currentTimeMillis();
            imageProxy.close();
        }
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Kamera belum siap", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Starting photo capture...");
        showProgress("Mengambil foto...");
        isCaptureMode = true;

        String name = "SmartWaste_" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(new Date());

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SmartWaste");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                .build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = output.getSavedUri();
                        Log.d(TAG, "Photo saved successfully: " + savedUri);

                        Bitmap finalBitmap = null;

                        try (InputStream inputStream = getContentResolver().openInputStream(savedUri)) {
                            Bitmap loadedBitmap = BitmapFactory.decodeStream(inputStream);
                            if (loadedBitmap != null) {
                                finalBitmap = ImageUtil.rotateBitmap(loadedBitmap, 90);
                                Toast.makeText(MainActivity.this, "Foto berhasil diambil", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Gagal memuat foto", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error loading saved photo", e);
                            Toast.makeText(MainActivity.this, "Gagal memuat foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            final Bitmap bitmapToProcess = finalBitmap;
                            runOnUiThread(() -> {
                                if (bitmapToProcess != null) {
                                    showProgress("Menganalisis foto...");
                                    processBitmapForDetection(bitmapToProcess);
                                } else {
                                    hideProgress();
                                    isCaptureMode = false;
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed", exception);
                        runOnUiThread(() -> {
                            hideProgress();
                            isCaptureMode = false;
                            Toast.makeText(MainActivity.this, "Gagal mengambil foto: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void uploadImage() {
        showProgress("Membuka galeri...");
        isCaptureMode = false;
        pickImageLauncher.launch("image/*");
    }

    private void processBitmapForDetection(Bitmap bitmap) {
        if (bitmap == null) {
            hideProgress();
            return;
        }

        showProgress("Menganalisis gambar...");
        this.originalBitmapForDetection = bitmap;
        this.isLiveDetection = false;

        cameraExecutor.execute(() -> {
            String base64Image = BitmapUtils.toBase64(originalBitmapForDetection);
            roboflowAPI.detectGarbage(base64Image, this);
        });
    }

    @Override
    public void onSuccess(List<RoboflowAPI.Prediction> predictions) {
        runOnUiThread(() -> {
            hideProgress();

            if (isLiveDetection) {
                if (predictions.isEmpty()) {
                    tvLiveResult.setText("Tidak ada sampah terdeteksi");
                } else {
                    String detectedClasses = predictions.stream()
                            .map(p -> String.format("%s (%.1f%%)", p.className, p.confidence * 100))
                            .collect(Collectors.joining(", "));
                    tvLiveResult.setText(String.format("Terdeteksi: %s", detectedClasses));
                }
            } else {
                if (originalBitmapForDetection != null) {
                    Bitmap resultBitmap = BitmapUtils.drawBoundingBoxes(originalBitmapForDetection, predictions);

                    AppDataHolder.getInstance().setResultBitmap(resultBitmap);
                    AppDataHolder.getInstance().setPredictions(predictions);

                    openResultActivity();
                }

                originalBitmapForDetection = null;
                isCaptureMode = false;
            }
        });
    }

    private void openResultActivity() {
        try {
            Intent intent = new Intent(this, ResultActivity.class);

            if (AppDataHolder.getInstance().getPredictions() != null) {
                intent.putExtra(ResultActivity.EXTRA_OBJECT_COUNT, AppDataHolder.getInstance().getPredictions().size());
            } else {
                intent.putExtra(ResultActivity.EXTRA_OBJECT_COUNT, 0);
            }

            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error opening ResultActivity", e);
            Toast.makeText(this, "Gagal membuka hasil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            hideProgress();
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
            Log.e(TAG, "API Error: " + error);
            if (!isLiveDetection) {
                originalBitmapForDetection = null;
                isCaptureMode = false;
            }
        });
    }

    private void showProgress(String message) {
        Log.d(TAG, "PROGRESS: " + message);
        tvProgressStatus.setText(message);
        progressOverlay.setVisibility(View.VISIBLE);
        cameraControls.setVisibility(View.GONE);
    }

    private void hideProgress() {
        progressOverlay.setVisibility(View.GONE);
        if(ivResult.getVisibility() == View.GONE) {
            cameraControls.setVisibility(View.VISIBLE);
        }
    }

    private void showResultView(Bitmap bitmap) {
        progressOverlay.setVisibility(View.GONE);
        ivResult.setImageBitmap(bitmap);
        ivResult.setVisibility(View.VISIBLE);
        btnCloseResult.setVisibility(View.VISIBLE);
        cameraControls.setVisibility(View.GONE);
        if(imageAnalysis != null) imageAnalysis.clearAnalyzer();
    }

    private void hideResultView() {
        ivResult.setVisibility(View.GONE);
        btnCloseResult.setVisibility(View.GONE);
        cameraControls.setVisibility(View.VISIBLE);
        if(imageAnalysis != null) imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
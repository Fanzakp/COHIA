package com.example.smartwaste;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartwaste.api.RoboflowAPI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    public static final String EXTRA_OBJECT_COUNT = "extra_object_count";

    private ImageView ivResultImage;
    private TextView tvDetectionResult;
    private TextView tvConfidence;
    private TextView tvObjectCount;
    private TextView tvRecommendation;
    private TextView tvAllDetections;
    private Button btnSaveImage;
    private Button btnBackToMain; // UBAH INI dari btnShare
    private ImageButton btnBack; // Tombol back di header

    private Bitmap resultBitmap;
    private List<RoboflowAPI.Prediction> predictions;
    private int objectCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        initViews();
        loadDataFromIntent();
        setupClickListeners();
        displayResults();
    }

    private void initViews() {
        ivResultImage = findViewById(R.id.iv_result_image);
        tvDetectionResult = findViewById(R.id.tv_detection_result);
        tvConfidence = findViewById(R.id.tv_confidence);
        tvObjectCount = findViewById(R.id.tv_object_count);
        tvRecommendation = findViewById(R.id.tv_recommendation);
        tvAllDetections = findViewById(R.id.tv_all_detections);
        btnSaveImage = findViewById(R.id.btn_save_image);
        btnBackToMain = findViewById(R.id.btn_back_to_main); // UBAH INI dari R.id.btn_share
        btnBack = findViewById(R.id.btn_back);
    }

    private void loadDataFromIntent() {
        resultBitmap = AppDataHolder.getInstance().getResultBitmap();
        predictions = AppDataHolder.getInstance().getPredictions();

        if (predictions == null) {
            predictions = new ArrayList<>();
        }

        objectCount = getIntent().getIntExtra(EXTRA_OBJECT_COUNT, 0);

        AppDataHolder.getInstance().clearData();
    }

    private void setupClickListeners() {
        // Listener untuk tombol back di header (sudah ada)
        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Header back button clicked!");
            finish();
        });

        btnSaveImage.setOnClickListener(v -> saveImageToGallery());

        // Listener untuk tombol "Kembali" yang baru
        btnBackToMain.setOnClickListener(v -> { // UBAH INI dari btnShare.setOnClickListener
            Log.d(TAG, "Bottom back button clicked!");
            finish(); // Menutup ResultActivity dan kembali ke MainActivity
        });
    }

    private void displayResults() {
        if (resultBitmap != null) {
            ivResultImage.setImageBitmap(resultBitmap);
        }

        tvObjectCount.setText(predictions.size() + " objek");

        if (!predictions.isEmpty()) {
            RoboflowAPI.Prediction bestPrediction = Collections.max(predictions,
                    (p1, p2) -> Float.compare(p1.confidence, p2.confidence));

            tvDetectionResult.setText(bestPrediction.className);
            tvConfidence.setText(String.format(Locale.US, "Tingkat kepercayaan: %.1f%%",
                    bestPrediction.confidence * 100));
            tvRecommendation.setText(getRecommendation(bestPrediction.className));

            String allDetectionsText = predictions.stream()
                    .map(p -> String.format(Locale.US, "%s (%.1f%%)", p.className, p.confidence * 100))
                    .collect(Collectors.joining(", "));
            tvAllDetections.setText(allDetectionsText);

        } else {
            tvDetectionResult.setText("Tidak ada sampah terdeteksi");
            tvConfidence.setText("Tingkat kepercayaan: 0%");
            tvRecommendation.setText("Pastikan gambar jelas dan objek sampah terlihat dengan baik.");
            tvAllDetections.setText("-");
        }
    }

    private String getRecommendation(String wasteType) {
        switch (wasteType.toLowerCase()) {
            case "sampah organik":
            case "organik":
                return "Sampah organik dapat diolah menjadi kompos. Pisahkan dari sampah lainnya dan masukkan ke tempat sampah organik (warna hijau).";

            case "sampah anorganik":
            case "anorganik":
                return "Sampah anorganik seperti plastik, kaleng, dan kertas dapat didaur ulang. Bersihkan terlebih dahulu sebelum dimasukkan ke tempat sampah daur ulang (warna kuning).";

            case "sampah b3":
            case "b3":
                return "⚠️ Sampah B3 (Bahan Berbahaya dan Beracun) memerlukan penanganan khusus. Jangan dibuang sembarangan! Bawa ke tempat pengumpulan sampah B3 terdekat.";

            default:
                return "Pastikan untuk membuang sampah pada tempatnya sesuai dengan jenis dan karakteristiknya.";
        }
    }

    private void saveImageToGallery() {
        if (resultBitmap == null) {
            Toast.makeText(this, "Tidak ada gambar untuk disimpan", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String filename = "SmartWaste_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                    .format(new Date()) + ".jpg";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SmartWaste");
            }

            ContentResolver resolver = getContentResolver();
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (imageUri != null) {
                try (OutputStream outputStream = resolver.openOutputStream(imageUri)) {
                    if (outputStream != null) {
                        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                        Toast.makeText(this, "Gambar berhasil disimpan ke galeri", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Gambar disimpan di: " + imageUri);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Gagal menyimpan gambar", e);
            Toast.makeText(this, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show();
        }
    }

    // Metode shareResult dihapus karena tombolnya diganti
    // private void shareResult() {
    //    // ... kode lama ...
    // }
}
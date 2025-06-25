package com.example.cohia;

import android.graphics.Bitmap;
import com.example.cohia.api.RoboflowAPI;
import java.util.List;

// Kelas Singleton untuk menyimpan data sementara antar Activity
public class AppDataHolder {
    private static final AppDataHolder ourInstance = new AppDataHolder();

    public static AppDataHolder getInstance() {
        return ourInstance;
    }

    private Bitmap resultBitmap;
    private List<RoboflowAPI.Prediction> predictions;

    private AppDataHolder() {
        // Private constructor untuk mencegah instansiasi dari luar
    }

    public Bitmap getResultBitmap() {
        return resultBitmap;
    }

    public void setResultBitmap(Bitmap resultBitmap) {
        this.resultBitmap = resultBitmap;
    }

    public List<RoboflowAPI.Prediction> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<RoboflowAPI.Prediction> predictions) {
        this.predictions = predictions;
    }

    // Metode untuk membersihkan data setelah digunakan
    public void clearData() {
        this.resultBitmap = null;
        this.predictions = null;
    }
}
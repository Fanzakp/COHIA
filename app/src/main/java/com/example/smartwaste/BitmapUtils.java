package com.example.smartwaste;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Base64;

import com.example.smartwaste.api.RoboflowAPI;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

/**
 * Kelas utilitas untuk menangani operasi terkait Bitmap.
 */
public class BitmapUtils {

    // Membuat constructor private untuk mencegah instansiasi kelas utilitas.
    private BitmapUtils() {}

    /**
     * Mengonversi objek Bitmap menjadi string Base64.
     * @param bitmap Bitmap yang akan dikonversi.
     * @return String Base64 yang merepresentasikan gambar.
     */
    public static String toBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // Mengompres bitmap ke format JPEG dengan kualitas 90.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        // Meng-encode byte array menjadi string Base64.
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    /**
     * Menggambar bounding box dan label kelas pada Bitmap berdasarkan daftar prediksi.
     * @param bitmap Bitmap asli tempat menggambar.
     * @param predictions Daftar objek Prediksi yang berisi data bounding box dan kelas.
     * @return Bitmap baru dengan bounding box dan label yang sudah digambar.
     */
    public static Bitmap drawBoundingBoxes(Bitmap bitmap, List<RoboflowAPI.Prediction> predictions) {
        // Membuat salinan bitmap yang bisa diubah (mutable).
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        // Pengaturan untuk kuas (Paint) kotak.
        Paint boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8f); // Ketebalan garis kotak
        boxPaint.setColor(Color.RED); // Warna garis kotak

        // Pengaturan untuk kuas (Paint) teks/label.
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f); // Ukuran teks label
        textPaint.setStyle(Paint.Style.FILL);

        // Pengaturan untuk latar belakang teks.
        Paint textBackgroundPaint = new Paint();
        textBackgroundPaint.setColor(Color.RED); // Warna latar belakang label
        textBackgroundPaint.setStyle(Paint.Style.FILL);

        // Looping melalui setiap prediksi.
        for (RoboflowAPI.Prediction prediction : predictions) {
            // Koordinat dari Roboflow adalah dalam bentuk normal (0-1).
            // Kita perlu mengubahnya menjadi skala piksel dari bitmap.
            float left = prediction.boundingBox.left * bitmap.getWidth();
            float top = prediction.boundingBox.top * bitmap.getHeight();
            float right = prediction.boundingBox.right * bitmap.getWidth();
            float bottom = prediction.boundingBox.bottom * bitmap.getHeight();

            RectF scaledBox = new RectF(left, top, right, bottom);

            // Menggambar kotak pada canvas.
            canvas.drawRect(scaledBox, boxPaint);

            // Membuat teks label (Kelas + Confidence).
            String label = String.format(Locale.US, "%s %.1f%%",
                    prediction.className, prediction.confidence * 100);

            // Menggambar latar belakang untuk teks agar mudah dibaca.
            // Hitung lebar dan tinggi teks untuk latar belakang yang pas
            float textWidth = textPaint.measureText(label);
            float textHeight = textPaint.descent() - textPaint.ascent(); // Tinggi teks

            // Sesuaikan posisi latar belakang agar tidak terlalu mepet
            // top - textHeight adalah posisi y untuk bagian bawah teks (baseline)
            // top - textHeight - 10f (padding atas)
            // top - 10f (padding bawah)
            RectF textBackgroundRect = new RectF(left, top - textHeight - 10f, left + textWidth + 10f, top - 5f); // Menambahkan sedikit padding
            canvas.drawRect(textBackgroundRect, textBackgroundPaint);

            // Menggambar teks label di atas kotak.
            canvas.drawText(label, left + 5f, top - 10f, textPaint); // Menambahkan sedikit padding ke teks
        }

        return mutableBitmap;
    }
}
package com.example.cohia;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix; // Import ini
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Kelas utilitas untuk konversi format gambar.
 */
public class ImageUtil {
    private static final String TAG = "ImageUtil";

    private ImageUtil() {}

    /**
     * Mengonversi objek android.media.Image (dari format YUV_420_888) menjadi Bitmap.
     * Diperlukan untuk ImageAnalysis dari CameraX.
     *
     * @param image Objek Image yang akan dikonversi.
     * @return Bitmap hasil konversi, atau null jika gagal.
     */
    @Nullable
    public static Bitmap imageToBitmap(@Nullable Image image) {
        if (image == null) {
            Log.w(TAG, "Image is null");
            return null;
        }

        try {
            // Method 1: Jika format JPEG, langsung decode
            if (image.getFormat() == ImageFormat.JPEG) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }

            // Method 2: Untuk format YUV_420_888
            if (image.getFormat() == ImageFormat.YUV_420_888) {
                return yuv420ToBitmap(image);
            }

            Log.w(TAG, "Unsupported image format: " + image.getFormat());
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error converting Image to Bitmap", e);
            return null;
        }
    }

    /**
     * Konversi YUV_420_888 ke Bitmap
     */
    private static Bitmap yuv420ToBitmap(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            // U dan V adalah swapped
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();

            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        } catch (Exception e) {
            Log.e(TAG, "Error in yuv420ToBitmap", e);
            return null;
        }
    }

    /**
     * Metode untuk merotasi Bitmap.
     * @param source Bitmap yang akan dirotasi.
     * @param angle Derajat rotasi (misalnya 90, 180, 270).
     * @return Bitmap yang sudah dirotasi.
     */
    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Alternative method: Konversi Image ke Bitmap dengan pendekatan berbeda
     * Gunakan method ini jika method utama gagal
     */
    @Nullable
    public static Bitmap imageToBitmapAlternative(@Nullable Image image) {
        if (image == null) return null;

        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();

            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * image.getWidth();

            Bitmap bitmap = Bitmap.createBitmap(
                    image.getWidth() + rowPadding / pixelStride,
                    image.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            bitmap.copyPixelsFromBuffer(buffer);

            if (rowPadding != 0) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, image.getWidth(), image.getHeight());
            }

            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error in imageToBitmapAlternative", e);
            return null;
        }
    }

    /**
     * Method paling sederhana untuk testing - hanya untuk format JPEG
     */
    @Nullable
    public static Bitmap imageToBitmapSimple(@Nullable Image image) {
        if (image == null) return null;

        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error in imageToBitmapSimple", e);
            return null;
        }
    }
}
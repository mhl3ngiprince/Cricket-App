package com.finedine.spucricketclub;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ImageProxy;
import androidx.camera.core.ExperimentalGetImage;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    @ExperimentalGetImage
    public static Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) {
            Log.e(TAG, "Null image from imageProxy");
            return null;
        }

        try {
            if (image.getFormat() == ImageFormat.YUV_420_888) {
                ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
                ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
                ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

                int ySize = yBuffer.remaining();
                int uSize = uBuffer.remaining();
                int vSize = vBuffer.remaining();

                byte[] nv21 = new byte[ySize + uSize + vSize];

                // Avoid buffer overflow
                if (ySize > nv21.length) {
                    Log.e(TAG, "Y buffer too large: " + ySize);
                    return null;
                }

                yBuffer.get(nv21, 0, ySize);

                if (vSize > nv21.length - ySize) {
                    Log.e(TAG, "V buffer too large: " + vSize);
                    return null;
                }

                vBuffer.get(nv21, ySize, vSize);

                if (uSize > nv21.length - ySize - vSize) {
                    Log.e(TAG, "U buffer too large: " + uSize);
                    return null;
                }

                uBuffer.get(nv21, ySize + vSize, uSize);

                // Get image dimensions
                int width = image.getWidth();
                int height = image.getHeight();

                if (width <= 0 || height <= 0) {
                    Log.e(TAG, "Invalid image dimensions: " + width + "x" + height);
                    return null;
                }

                YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                boolean success = yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, width, height), 80, out);
                if (!success) {
                    Log.e(TAG, "Failed to compress YuvImage to JPEG");
                    return null;
                }

                byte[] jpegBytes = out.toByteArray();

                try {
                    return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "OOM while decoding bitmap", e);
                    return null;
                } finally {
                    try {
                        out.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Error closing output stream", e);
                    }
                }
            } else {
                Log.e(TAG, "Unsupported image format: " + image.getFormat());
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            return null;
        }
    }
}

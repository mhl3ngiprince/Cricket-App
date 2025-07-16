package com.finedine.spucricketclub.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.Image.Plane;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Utilities for image processing in the app
 */
public class ImageUtils {
    private static final String TAG = "ImageUtils";

    /**
     * Converts a media.Image to a Bitmap
     */
    public static Bitmap imageToBitmap(Image image) {
        if (image.getFormat() == ImageFormat.YUV_420_888) {
            return yuv420ToBitmap(image);
        } else if (image.getFormat() == ImageFormat.JPEG) {
            return jpegToBitmap(image);
        }

        Log.e(TAG, "Unsupported image format: " + image.getFormat());
        return null;
    }

    /**
     * Converts YUV_420_888 format image to Bitmap
     */
    private static Bitmap yuv420ToBitmap(Image image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Only supports YUV_420_888 format");
        }

        // Get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();

        // Get image data
        Plane[] planes = image.getPlanes();
        byte[] yuvBytes = new byte[width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        fillYUVBytes(planes, yuvBytes);

        // Create YuvImage
        YuvImage yuvImage = new YuvImage(
                yuvBytes, ImageFormat.NV21, width, height, null);

        // Convert to JPEG ByteArrayOutputStream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);

        // Convert to Bitmap
        byte[] jpegBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    /**
     * Fills a byte array with YUV data from image planes
     */
    private static void fillYUVBytes(Plane[] planes, byte[] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        int position = 0;

        for (int planeIndex = 0; planeIndex < planes.length; planeIndex++) {
            Plane plane = planes[planeIndex];
            ByteBuffer buffer = plane.getBuffer();
            int rowStride = plane.getRowStride();
            int pixelStride = plane.getPixelStride();
            int width = planeIndex == 0 ? plane.getRowStride() : plane.getRowStride() / 2;
            int height = planeIndex == 0 ? planes[0].getBuffer().capacity() / plane.getRowStride()
                    : planes[0].getBuffer().capacity() / (plane.getRowStride() * 2);

            // Extract pixel data - this is a simplification that works for common formats
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    if (position < yuvBytes.length) {
                        yuvBytes[position++] = buffer.get(row * rowStride + col * pixelStride);
                    }
                }
            }
        }
    }

    /**
     * Converts JPEG format image to Bitmap
     */
    private static Bitmap jpegToBitmap(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Rotate a bitmap to the specified orientation
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int rotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
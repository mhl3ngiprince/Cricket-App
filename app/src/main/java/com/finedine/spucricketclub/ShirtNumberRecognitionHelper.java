package com.finedine.spucricketclub;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class ShirtNumberRecognitionHelper {

    private static final String TAG = "ShirtNumberRecognitionHelper";

    private TextRecognizer textRecognizer;

    public ShirtNumberRecognitionHelper(Context context) {
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public void recognizeShirtNumber(Bitmap bitmap, ShirtNumberRecognitionCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        textRecognizer.process(image)
                .addOnSuccessListener(text -> {
                    String shirtNumber = extractShirtNumber(text);
                    Log.d(TAG, "Shirt number recognized: " + shirtNumber);
                    callback.onShirtNumberRecognized(shirtNumber);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Shirt number recognition failed", e);
                    callback.onFailure(e);
                });
    }

    private String extractShirtNumber(Text text) {
        // Simple heuristic: find the largest numeric text block
        String largestNumber = "";
        for (Text.TextBlock block : text.getTextBlocks()) {
            String blockText = block.getText().replaceAll("\\s+", "");
            if (blockText.matches("\\d+")) {
                if (blockText.length() > largestNumber.length()) {
                    largestNumber = blockText;
                }
            }
        }
        return largestNumber;
    }

    public interface ShirtNumberRecognitionCallback {
        void onShirtNumberRecognized(String shirtNumber);
        void onFailure(@NonNull Exception e);
    }

    public void close() {
        textRecognizer.close();
    }
}
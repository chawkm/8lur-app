package com.c362.group8.bclpd;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.Iterator;

public class ResultActivity extends AppCompatActivity {
    private ImageView licensePlateView;

    private Bitmap licensePlate;
    private static final int width = 270;
    private static final int height = 90;

    private TensorFlowInferenceInterface tensorflow;
    private static final String modelPath = "file:///android_asset/graph_optimized.pb";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Get the blurred license plate from the crop activity.
        Intent intent = getIntent();
        licensePlate = intent.getParcelableExtra(CropActivity.BLURRED_LICENSE_PLATE);

        // Deblur license plate.
        AssetManager assetManager = getAssets();
        tensorflow = new TensorFlowInferenceInterface(assetManager, modelPath);
        licensePlate = deblurLicensePlate(licensePlate);

        // Load license plate to the view.
        licensePlateView = findViewById(R.id.deblurredLicensePlate);
        licensePlateView.setImageBitmap(licensePlate);
    }

    // Runs the model on the image.
    private Bitmap deblurLicensePlate(Bitmap licensePlate) {
        licensePlate = Bitmap.createScaledBitmap(licensePlate, width, height, true);

        int[] byteValues = new int[licensePlate.getHeight() * licensePlate.getWidth()];
        float[] floatValues = new float[64 * byteValues.length * 3];
        licensePlate.getPixels(byteValues, 0, width, 0, 0, width, height);

        for (int i = 0; i < 64 * byteValues.length; i++) {
            int j = i % byteValues.length;
            final int val = byteValues[j];
            floatValues[j * 3 + 0] = (float) (((val >> 16) & 0xff) / 255.0);
            floatValues[j * 3 + 1] = (float) (((val >>  8) & 0xff) / 255.0);
            floatValues[j * 3 + 2] = (float) (((val >>  0) & 0xff) / 255.0);
        }

        tensorflow.feed("corrupted", floatValues, 64, height, width, 3);

        Iterator<Operation> iter = tensorflow.graph().operations();
        while (iter.hasNext()) {
            System.out.println(iter.next().name());
        }

        String outputNode = "Tanh";
        String[] outputNodes = {outputNode};
        tensorflow.run(outputNodes);

        float[] output = new float[floatValues.length];
        tensorflow.fetch(outputNode, output);

        int[] finalImage = new int[floatValues.length];
        for (int i = 0; i < byteValues.length * 3; i++) {
            finalImage[i] = (int) (255 * (output[i]));
        }

        Bitmap deblurredLicensePlate = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Iterate through each pixel in the difference bitmap.
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int a = 255;
                int r = finalImage[count + 0];
                int g = finalImage[count + 1];
                int b = finalImage[count + 2];
                deblurredLicensePlate.setPixel(x, y, Color.argb(a, r, g, b));

                count += 3;
            }
        }

        return deblurredLicensePlate;
    }
}

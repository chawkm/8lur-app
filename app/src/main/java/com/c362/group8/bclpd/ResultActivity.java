package com.c362.group8.bclpd;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.Iterator;

public class ResultActivity extends AppCompatActivity {
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 0;

    private ImageView licensePlateView;
    private ProgressBar spinner;
    private Button saveButton;

    private Bitmap licensePlate;
    private boolean isDeblurred;
    private static final int width = 270;
    private static final int height = 90;

    private TensorFlowInferenceInterface tensorflow;
    private static final String modelPath = "file:///android_asset/graph_optimized.pb";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // We use this boolean to only allow deblurring once.
        isDeblurred = false;

        // Get the blurred license plate from the crop activity.
        Intent intent = getIntent();
        licensePlate = intent.getParcelableExtra(CropActivity.BLURRED_LICENSE_PLATE);
        licensePlate = Bitmap.createScaledBitmap(licensePlate, width, height, true);
        licensePlateView = findViewById(R.id.deblurredLicensePlate);
        licensePlateView.setImageBitmap(licensePlate);

        // Spinner.
        spinner = findViewById(R.id.progressBar);

        // Save button.
        saveButton = findViewById(R.id.saveButton);
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check and request permissions.
                int permissionCheck = ContextCompat.checkSelfPermission(
                        ResultActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            ResultActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                }

                // Save to gallery.
                String savedImageURL = MediaStore.Images.Media.insertImage(
                        getContentResolver(),
                        licensePlate,
                        "License Plate",
                        "Deblurred using BCLPD"
                );

                if (savedImageURL == null) {
                    // There was an error saving.
                    Toast.makeText(
                            ResultActivity.this,
                            "Could not save the image to the gallery.",
                            Toast.LENGTH_LONG).show();
                } else {
                    goToMainActivity();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isDeblurred) {
            isDeblurred = true;
            new BackgroundDeblurring().execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_CODE) {
            // Disable the save button if permission request is denied.
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                saveButton.setEnabled(false);
            }
        }
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    // Asynchronous task used to deblur the license plate in the background.
    private class BackgroundDeblurring extends AsyncTask<Void, Void, Integer> {
        @Override
        protected void onPreExecute() {
            spinner.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Integer result) {
            spinner.setVisibility(View.GONE);
            licensePlateView.setImageBitmap(licensePlate);
            saveButton.setEnabled(true);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            AssetManager assetManager = getAssets();
            tensorflow = new TensorFlowInferenceInterface(assetManager, modelPath);
            licensePlate = deblurLicensePlate(licensePlate);

            return 0;
        }

        // Runs the model on the image.
        private Bitmap deblurLicensePlate(Bitmap licensePlate) {
            int[] byteValues = new int[width * height];
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
}

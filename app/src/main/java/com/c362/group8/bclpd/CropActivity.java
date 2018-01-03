package com.c362.group8.bclpd;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImageView;

public class CropActivity extends AppCompatActivity {
    public static final String BLURRED_LICENSE_PLATE = "BLURRED_LICENSE_PLATE";

    private CropImageView cropImageView;
    private Button finishedCroppingButton;
    private Bitmap pictureToCrop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        // Get image taken / uploaded.
        Intent intent = getIntent();
        pictureToCrop = intent.getParcelableExtra(MainActivity.PICTURE_TO_CROP);

        // Put image in the crop view.
        cropImageView = findViewById(R.id.cropImageView);
        cropImageView.setImageBitmap(pictureToCrop);

        finishedCroppingButton = findViewById(R.id.finishedCroppingButton);
        finishedCroppingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap licensePlate = cropImageView.getCroppedImage();
                if (licensePlate != null) {
                    goToResultActivity(licensePlate);
                } else {
                    // Something went wrong, stay in the crop view.
                    Toast.makeText(
                            CropActivity.this,
                            "Something went wrong. Try again.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void goToResultActivity(Bitmap licensePlate) {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(BLURRED_LICENSE_PLATE, licensePlate);
        startActivity(intent);
    }
}

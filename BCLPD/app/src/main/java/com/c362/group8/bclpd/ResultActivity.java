package com.c362.group8.bclpd;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class ResultActivity extends AppCompatActivity {
    private ImageView licensePlateView;
    private Bitmap licensePlate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Get the blurred license plate from the crop activity.
        Intent intent = getIntent();
        licensePlate = intent.getParcelableExtra(CropActivity.BLURRED_LICENSE_PLATE);

        // Load license plate to the view.
        licensePlateView = findViewById(R.id.deblurredLicensePlate);
        licensePlateView.setImageBitmap(licensePlate);
    }

    private Bitmap deblurLicensePlate(Bitmap licensePlate){
        return licensePlate;
    }
}

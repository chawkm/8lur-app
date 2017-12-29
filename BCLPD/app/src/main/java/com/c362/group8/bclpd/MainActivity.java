package com.c362.group8.bclpd;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private int TAKE_PICTURE_REQUEST_CODE = 0;
    private int UPLOAD_PICTURE_REQUEST_CODE = 1;
    ImageButton takePictureButton;
    ImageButton uploadPictureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takePictureButton = findViewById(R.id.takePictureButton);
        uploadPictureButton = findViewById(R.id.uploadPictureButton);

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Take a picture.
                Intent takePictureIntent = new Intent();
                takePictureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePictureIntent, TAKE_PICTURE_REQUEST_CODE);
            }
        });

        uploadPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the gallery to choose an image.
                Intent uploadPictureIntent = new Intent(Intent.ACTION_PICK);
                uploadPictureIntent.setType("image/*");
                startActivityForResult(uploadPictureIntent, UPLOAD_PICTURE_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        Bitmap picture;
        boolean error = false;
        try {
            if (resultCode == RESULT_OK && reqCode == UPLOAD_PICTURE_REQUEST_CODE) {
                // Case 'upload picture'.
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                picture = BitmapFactory.decodeStream(imageStream);
            } else if (resultCode == RESULT_OK && reqCode == TAKE_PICTURE_REQUEST_CODE) {
                // Case 'take picture'.
                picture = (Bitmap) data.getExtras().get("data");
            } else {
                // Shouldn't get here.
                error = true;
            }
        } catch (Exception e) {
            error = true;
        }

        if (!error) {
            goToCropActivity();
        } else {
            // Something went wrong, reload the view.
            Toast.makeText(MainActivity.this, "Something went wrong. Try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void goToCropActivity() {
        Intent intent = new Intent(this, CropActivity.class);
        startActivity(intent);
    }
}

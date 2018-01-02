package com.c362.group8.bclpd;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    public static final String PICTURE_TO_CROP = "PICTURE_TO_CROP";

    private int TAKE_PICTURE_REQUEST_CODE = 0;
    private int UPLOAD_PICTURE_REQUEST_CODE = 1;
    private ImageButton takePictureButton;
    private ImageButton uploadPictureButton;

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
                Intent uploadPictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(uploadPictureIntent, UPLOAD_PICTURE_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        Bitmap picture = null;
        boolean error = false;
        try {
            if (resultCode == RESULT_OK && reqCode == UPLOAD_PICTURE_REQUEST_CODE) {
                Log.d("upload", "Uploading picture...");
                // Case 'upload picture'.
                final Uri imageUri = data.getData();
                picture = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } else if (resultCode == RESULT_OK && reqCode == TAKE_PICTURE_REQUEST_CODE) {
                // Case 'take picture'.
                picture = (Bitmap) data.getExtras().get("data");
            } else {
                // Shouldn't get here.
                error = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            error = true;
        }

        if (!error && picture != null) {
            goToCropActivity(picture);
        } else {
            // Something went wrong, stay in the main view.
            Toast.makeText(
                    MainActivity.this,
                    "Something went wrong. Try again.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void goToCropActivity(Bitmap picture) {
        Intent intent = new Intent(this, CropActivity.class);
        intent.putExtra(PICTURE_TO_CROP, picture);
        startActivity(intent);
    }
}

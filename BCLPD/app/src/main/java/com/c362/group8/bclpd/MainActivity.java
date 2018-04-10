package com.c362.group8.bclpd;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, UPLOAD_PICTURE_REQUEST_CODE);
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
                // Case 'upload picture'.
                Uri imageUri = data.getData();
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                picture = BitmapFactory.decodeStream(imageStream);
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
        // Save the image temporarily.
        File dir = new File(Environment.getExternalStorageDirectory() + "/transfer/");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        OutputStream outputStream = null;
        File file = new File(Environment.getExternalStorageDirectory() + "/transfer/picture.png");
        try {
            outputStream = new FileOutputStream(file);
            picture.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, CropActivity.class);
        intent.putExtra(PICTURE_TO_CROP, file.getAbsolutePath());
        startActivity(intent);
    }
}

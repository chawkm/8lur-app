package com.example.christopherhawkes.deblurring_app;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class Home extends AppCompatActivity {
    private int TAKE_PICTURE_REQUEST_CODE = 0;
    private int UPLOAD_PICTURE_REQUEST_CODE = 1;
    ImageView imageView;
    ImageView deblurredView;
    Button uploadPicButton;
    Button takePicButton;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        uploadPicButton = (Button) findViewById(R.id.uploadPicButton);
        takePicButton = (Button) findViewById(R.id.takePicButton);
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView = (ImageView) findViewById(R.id.imageView2);

        uploadPicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, UPLOAD_PICTURE_REQUEST_CODE);
            }
        });

        takePicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePicIntent = new Intent();
                takePicIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

                startActivityForResult(takePicIntent, TAKE_PICTURE_REQUEST_CODE);
            }
        });

        deRiskedTensorFlow();
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (resultCode == RESULT_OK && reqCode == UPLOAD_PICTURE_REQUEST_CODE) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(Home.this, "Something went wrong uploading Image", Toast.LENGTH_LONG).show();
            }
        } else if (resultCode == RESULT_OK && reqCode == TAKE_PICTURE_REQUEST_CODE) {

            try {
                final Bitmap photo = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(photo);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(Home.this, "Something went wrong taking Image", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(Home.this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }

    private void deRiskedTensorFlow() {
        /** One time initialization: */
        AssetManager assetManager = getAssets();
        TensorFlowInferenceInterface tensorflow = new TensorFlowInferenceInterface(assetManager, "file:///android_asset/graph.pb");

        float[] input = {5.0F};
        tensorflow.feed("input", input);

        String outputNode = "output";
        String[] outputNodes = {outputNode};
        tensorflow.run(outputNodes);

        float[] output = new float[1];
        tensorflow.fetch(outputNode, output);

        System.out.println("fetched the output " + output[0]);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}

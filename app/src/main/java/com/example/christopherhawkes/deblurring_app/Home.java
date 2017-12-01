package com.example.christopherhawkes.deblurring_app;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;

public class Home extends AppCompatActivity {
    private int TAKE_PICTURE_REQUEST_CODE = 0;
    private int UPLOAD_PICTURE_REQUEST_CODE = 1;
    ImageView imageView;
    ImageView deblurredView;
    Button uploadPicButton;
    Button takePicButton;
    TensorFlowInferenceInterface tensorflow;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("On Create..");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Tensorflow
        AssetManager assetManager = getAssets();
        tensorflow = new TensorFlowInferenceInterface(assetManager, "file:///android_asset/graph_optimized.pb");


        uploadPicButton = (Button) findViewById(R.id.uploadPicButton);
        takePicButton = (Button) findViewById(R.id.takePicButton);
        imageView = (ImageView) findViewById(R.id.imageView);
        deblurredView = (ImageView) findViewById(R.id.imageView2);

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

//        deRiskedTensorFlow();
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (resultCode == RESULT_OK && reqCode == UPLOAD_PICTURE_REQUEST_CODE) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                // Deblur the image
                // TODO: Set image bitmap to output of deblurImage
                Bitmap deblurred = deblurImage(selectedImage);

                imageView.setImageBitmap(deblurred);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(Home.this, "Something went wrong uploading Image", Toast.LENGTH_LONG).show();
            }
        } else if (resultCode == RESULT_OK && reqCode == TAKE_PICTURE_REQUEST_CODE) {

            try {
                final Bitmap bitmap = (Bitmap) data.getExtras().get("data");

                // Deblur the image
                // TODO: Set image bitmap to output of deblurImage
                Bitmap deblurred = deblurImage(bitmap);
                imageView.setImageBitmap(bitmap);
                deblurredView.setImageBitmap(deblurred);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(Home.this, "Something went wrong taking Image", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(Home.this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap deblurImage(Bitmap bitmap) {
        bitmap = Bitmap.createScaledBitmap(bitmap, 270, 90, true);


        int[] byteValues = new int[bitmap.getHeight() * bitmap.getWidth()];
        float[] floatValues = new float[64 * byteValues.length * 3];
        bitmap.getPixels(byteValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < 64 * byteValues.length; i++) {
            int i_ = i % byteValues.length;
            final int val = byteValues[i_];
            floatValues[i_ * 3 + 0] = (float) (((val >> 16) & 0xff) / 255.0);
            floatValues[i_ * 3 + 1] = (float) (((val >> 8) & 0xff) / 255.0);
            floatValues[i_ * 3 + 2] = (float) ((val & 0xff) / 255.0);
        }

        tensorflow.feed("corrupted", floatValues, 64, bitmap.getHeight(), bitmap.getWidth(),3);

        Iterator<Operation> iter = tensorflow.graph().operations();
        while(iter.hasNext()) {
            System.out.println(iter.next().name());
        }
        String outputNode = "Tanh";
        String[] outputNodes = {outputNode};
        tensorflow.run(outputNodes);

        float[] output = new float[floatValues.length];
        tensorflow.fetch(outputNode, output);

        int[] finalImage = new int[floatValues.length];
        for (int i = 0; i < byteValues.length*3; i++) {
            finalImage[i] = (int) (255 * (output[i]));
        }
//        Bitmap deblurredBitmap = Bitmap.createBitmap(finalImage, bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);

        //--------------
        Bitmap deblurredBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        // Iterate through each pixel in the difference bitmap
        int count = 0;
        for(int y = 0; y < bitmap.getHeight(); y++) {
            for(int x = 0; x < bitmap.getWidth(); x++) {
                 int a = 255;
                int r = finalImage[count + 0];
                int g = finalImage[count + 1];
                int b = finalImage[count + 2];
                deblurredBitmap.setPixel(x, y, Color.argb(a, r, g, b));

                count = count+3;
            }
        }


        return deblurredBitmap;
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

package com.example.christopherhawkes.deblurring_app;

import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import org.tensorflow.TensorFlow;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class Home extends AppCompatActivity {
    private int TAKE_PICTURE_REQUEST_CODE = 0;
    VideoView imageView;
    Button takePic;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

//        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());

        takePic = (Button) findViewById(R.id.button3);
        imageView = (VideoView) findViewById(R.id.videoView);

        takePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePicIntent = new Intent();
                takePicIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

                startActivityForResult(takePicIntent, TAKE_PICTURE_REQUEST_CODE);
            }
        });



        /** One time initialization: */
        AssetManager assetManager = getAssets();
        TensorFlowInferenceInterface tensorflow = new TensorFlowInferenceInterface(assetManager, "file:///android_asset/graph.pb");

        float[] input = {5.0F};
        tensorflow.feed("input", input);

//        float[] input2 = {3.0F};
//        tensorflow.feed("b", input2);

        String outputNode = "output";
        String[] outputNodes = {outputNode};
        tensorflow.run(outputNodes);

        float[] output = new float[1];
        tensorflow.fetch(outputNode, output);

        System.out.println("fetched the output " + output[0]);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == TAKE_PICTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            imageView.setVideoURI(imageUri);
            imageView.start();
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}

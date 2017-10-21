package com.example.christopherhawkes.a8lur;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Deblur extends AppCompatActivity {
    private static final String TAG = "AndroidCameraApi";
    private Button takePictureButton;
    private Button galleryButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deblur);
        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        galleryButton = (Button) findViewById(R.id.button2);
        assert galleryButton != null;
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToGallery();
            }
        });
    }

    public void switchToGallery() {
        Intent intent = new Intent(this, Gallery.class);
        startActivity(intent);
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(Deblur.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(Deblur.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(Deblur.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Deblur.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(Deblur.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
}




//package com.example.christopherhawkes.a8lur;
//
//import android.content.Intent;
//import android.hardware.camera2.*;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Environment;
//import android.provider.MediaStore;
//import android.support.v4.content.FileProvider;
//import android.text.Spannable;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.Switch;
//import android.widget.TextView;
//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
//import android.text.SpannableStringBuilder;
//import android.text.Spanned;
//import android.text.style.ImageSpan;
//import android.view.View;
//import android.view.Menu;
//import android.view.MenuItem;
//
//import java.io.File;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//public class Deblur extends AppCompatActivity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_deblur);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
////       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
////        fab.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////                Snackbar.make(view, "What's going on braa", Snackbar.LENGTH_LONG)
////                        .setAction("Action", null).show();
////            }
////        });
//
//        int hack = 0;
//        String mainMessage1 = "Aris is cool";
//        String mainMessage2 = "Aris is not cool";
//
//        Switch sw = (Switch) findViewById(R.id.switch1);
//        sw.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "What's going on braa", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//
//        Button b = (Button) findViewById(R.id.button);
//        b.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                dispatchTakePictureIntent();
//                Snackbar.make(view, "What's going on braa", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//
//
//
//    // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//
//
//        tv.setText("lets not fuckk with C++");
//    }
//
//
//    private void setIconMenu(Menu menu, int menuItemId, int labelId, int iconId) {
//        MenuItem menuItem = menu.findItem(menuItemId);
//        SpannableStringBuilder builder =
//                new SpannableStringBuilder("  " + getResources().getString(labelId));
//        builder.setSpan(new ImageSpan(this, iconId), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        menuItem.setTitle(builder);
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_deblur, menu);
//
//        setIconMenu(menu,
//                R.id.action_settings,
//                R.string.action_ramon,
//                R.mipmap.ic_launcher);
//
//        setIconMenu(menu,
//                R.id.action_other,
//                R.string.action_aris,
//                R.mipmap.camera);
//
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//
//        switch (id) {
//            case R.id.action_settings:
//                tv.setText("action shizz");
//                return true;
//            case R.id.action_camera:
//                tv.setText("Camera");
//                return true;
//            case R.id.action_other:
//                tv.setText("I don't even care at this point.");
//                return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
////    /* Image stuff */
////    String mCurrentPhotoPath;
////
////    private File createImageFile() throws IOException {
////        // Create an image file name
////        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
////        String imageFileName = "JPEG_" + timeStamp + "_";
////        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
////        File image = File.createTempFile(
////                imageFileName,  /* prefix */
////                ".jpg",         /* suffix */
////                storageDir      /* directory */
////        );
////
////        // Save a file: path for use with ACTION_VIEW intents
////        mCurrentPhotoPath = image.getAbsolutePath();
////        return image;
////    }
////
////    static final int REQUEST_TAKE_PHOTO = 1;
////
////    private void dispatchTakePictureIntent() {
////        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
////        // Ensure that there's a camera activity to handle the intent
////        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
////            // Create the File where the photo should go
////            File photoFile = null;
////            try {
////                photoFile = createImageFile();
////            } catch (IOException ex) {
////                // Error occurred while creating the File
////            }
////            // Continue only if the File was successfully created
////            if (photoFile != null) {
////                Uri photoURI = FileProvider.getUriForFile(this,
////                        "com.example.android.fileprovider",
////                        photoFile);
////                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
////                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
////            }
////        }
////    }
////
////    private void galleryAddPic() {
////        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
////        File f = new File(mCurrentPhotoPath);
////        Uri contentUri = Uri.fromFile(f);
////        mediaScanIntent.setData(contentUri);
////        this.sendBroadcast(mediaScanIntent);
////    }
//
//
//    /**
//     * A native method that is implemented by the 'native-lib' native library,
//     * which is packaged with this application.
//     */
//    public native String stringFromJNI();
//
//    // Used to load the 'native-lib' library on application startup.
//    static {
//        System.loadLibrary("native-lib");
//    }
//}

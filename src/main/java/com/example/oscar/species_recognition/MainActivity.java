package com.example.oscar.species_recognition;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.R.attr.data;
import static android.R.attr.id;
import static android.R.attr.state_above_anchor;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;


import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import org.json.JSONObject;

public class MainActivity extends Activity {
    private Camera mCamera;
    private CameraPreview mPreview;

    private static final String TAG = "MainActivity";
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static final int SELECT_IMAGE = 1;
    public Camera.PictureCallback mPicture;

    private String selectedImagePath;
    //private ImageView imgView;
    public boolean bCamera = true;
    public String filePath = null;
    //public String url = null;
    private static final String url= "https://vision.googleapis.com/v1/images:annotate?key=";
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCjSizQ5TgnFYMEkUsqKZRsShXYhgfhOqY";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //imgView = (ImageView) findViewById(R.id.image_view);


        Log.d(TAG, "--------------------------------startup the application!");

        /*do all the things based on the situation that there is a camera in your phone*/
        if (checkCameraHardware(this)) {
            // Create an instance of Camera
            mCamera = getCameraInstance();
            mCamera.setDisplayOrientation(90);

            mPicture = new Camera.PictureCallback() {

                @Override
                public void onPictureTaken(byte[] data, Camera camera) {

                    File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                    //ImageInfo pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

                    Log.d("SRT", "picture directory = "+pictureFile);
                    //insertImage(pictureFile);
                    if (pictureFile == null) {
                        //Log.d(TAG, "Error creating media file, check storage permissions: " + e.getMessage());
                        return;
                    }

                    try {

                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        Log.d("SRT", "what is the data?  "+data);
                        fos.write(data);
                        fos.close();

                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "File not found: " + e.getMessage());
                    } catch (IOException e) {
                        Log.d(TAG, "Error accessing file: " + e.getMessage());
                    }


                    filePath = pictureFile.getPath();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);

                    Log.d("SRT", "filePath" + filePath);
                    //imgView.setVisibility(View.VISIBLE);
                    //imgView.setImageBitmap(bitmap);

                    String url = insertImage(getContentResolver(), bitmap, null, null);
                    Log.d("SRT", "the return value of insertimage is " + url);
                    //mCamera.startPreview();
                }
            };


            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);
        }

    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.d(TAG, "Camera is not available" + e.getMessage());
        }
        return c; // returns null if camera is unavailable
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "SRT");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("SRT", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        //ImageInfo imginfo = new ImageInfo();
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");

        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }
        Log.d("SRT", "Successful to create an image");
        return mediaFile;
        //return imginfo;
    }

    public void click_capture(View view) {
        //
        if( mCamera != null)
        {
            if(bCamera)
            {
                //take photo
                mCamera.takePicture(null, null, mPicture);
                bCamera = false;
            }
            else
            {
                mCamera.startPreview();
                bCamera = true;
            }

        }
    }


    public void click_album(View view) {
        //go to album
        Intent i=new Intent( MainActivity.this, BrowsePictureActivity.class);
        startActivity(i);
        //finish();

        //Intent intent = new Intent();
        //intent.setType("image/*");
        //intent.setAction(Intent.ACTION_GET_CONTENT);
        //startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGE);

    }


    public void click_recognition(View view) {
        int stage = getAPNType(this);
        Log.d("SRT", "state____________________________ ---   :" + stage);

        //call google cloud vision api
        if(!filePath.isEmpty())
        {


            Log.d("SRT", "filePath ---:" + filePath);

            File file = new File(filePath);
            byte[] bytesArray = new byte[(int) file.length()];

            try {

                FileInputStream fis = new FileInputStream(file);
                fis.read(bytesArray); //read file into bytes[]
                fis.close();

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            Log.d("SRT", "bytesArray ==" + bytesArray);
            /*
            Image image = new Image();
            image.encodeContent(bytesArray);
            String value = new StringBuilder().append(url).append(CLOUD_VISION_API_KEY).toString();

            String content ="{\n" +
                    "  \"requests\":\n" +
                    "    [\n" +
                    "      {\n" +
                    "        \"image\":" +
                    "          {\n" +
                    "            \"content\":\n"  +
                    "           \"" +
                    image.getContent().substring(0,100)  +
                    "           \"\n" +
                    "          },\n" +
                    "        \"features\":\n" +
                    "          [\n" +
                    "            {\n" +
                    "              \"type\": \"LABEL_DETECTION\"\n" +
                    "            } \n" +
                    "          ]\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  }";

            String content = "{\n" +
                    "  \"requests\": [\n" +
                    "    {\n" +
                    "      \"features\": [\n" +
                    "        {\n" +
                    "          \"type\": \"LABEL_DETECTION\"\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"image\": {\n" +
                    "        \"source\": {\n" +
                    "          \"imageUri\": \"http://oscarzhou.co.nz/images/portrait.jpg\"\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            NetUtils netUtils = new NetUtils();
            String response = netUtils.post(value, content);

            Log.d("SRT", "response :" + response);
            */


            try {
                // Process the image using Cloud Vision
                Map<String, Float> annotations = annotateImage(bytesArray);

                Log.d("SRT", "annotations:" + annotations);
            } catch (IOException e) {
                Log.w("SRT", "Unable to annotate image", e);
            }


        }
        else
        {
            Log.w("SRT", "~~~~~~~");
        }
    }

    public Map<String, Float> annotateImage(byte[] imageBytes) throws IOException {
        // Construct the Vision API instance
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        VisionRequestInitializer initializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY);

        Vision vision = new Vision.Builder(httpTransport, jsonFactory, null)
                .setVisionRequestInitializer(initializer)
                .build();

        // Create the image request
        AnnotateImageRequest imageRequest = new AnnotateImageRequest();
        Image image = new Image();
        image.encodeContent(imageBytes);
        imageRequest.setImage(image);

        // Add the features we want
        Feature labelDetection = new Feature();
        labelDetection.setType("LABEL_DETECTION");
        labelDetection.setMaxResults(10);
        imageRequest.setFeatures(Collections.singletonList(labelDetection));

        // Batch and execute the request
        BatchAnnotateImagesRequest requestBatch = new BatchAnnotateImagesRequest();
        requestBatch.setRequests(Collections.singletonList(imageRequest));

        Log.w("SRT", "!!!!!!!!!!!!!! = " + requestBatch.values());

        Log.w("SRT", "vision!!!!!! = " + vision.getBaseUrl());

        Log.w("SRT", "application name!!!!!! = " + vision.getApplicationName());


        BatchAnnotateImagesResponse response = vision.images()
                .annotate(requestBatch)
                .setDisableGZipContent(true)
                .execute();

        Log.w("SRT", "response is "+ response);
        return convertResponseToMap(response);
    }

    private Map<String, Float> convertResponseToMap(BatchAnnotateImagesResponse response) {
        Map<String, Float> annotations = new HashMap<String, Float>();

        // Convert response into a readable collection of annotations
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                annotations.put(label.getDescription(), label.getScore());
                Log.w("SRT", "description="+label.getDescription()+", score="+label.getScore());
            }
        }

        return annotations;
    }


    public void button_dictionary(View view) {
        //


    }

    public static final String insertImage(ContentResolver cr,
                                           Bitmap source,
                                           String title,
                                           String description) {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, description);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        // Add the date meta data to ensure the image is added at the front of the gallery
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

        Uri url = null;
        String stringUrl = null;    /* value to be returned */
        try {
            url = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Log.d("SRT", "url = "+url);
            Log.d("SRT", "source = "+source);
            if (source != null) {
                OutputStream imageOut = cr.openOutputStream(url);
                try {
                    source.compress(Bitmap.CompressFormat.JPEG, 50, imageOut);
                } finally {
                    Log.d("SRT", "imageOut.close()");
                    imageOut.close();
                }

                long id = ContentUris.parseId(url);
                Log.d("SRT", "id = "+id);
                // Wait until MINI_KIND thumbnail is generated.
                Bitmap miniThumb = MediaStore.Images.Thumbnails.getThumbnail(cr, id, MediaStore.Images.Thumbnails.MINI_KIND, null);
                // This is for backward compatibility.
                storeThumbnail(cr, miniThumb, id, 50F, 50F, MediaStore.Images.Thumbnails.MICRO_KIND);
            } else {
                cr.delete(url, null, null);
                url = null;
            }
        } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
                url = null;
            }
        }

        if (url != null) {
            stringUrl = url.toString();
        }

        return stringUrl;
    }


    private static final Bitmap storeThumbnail(
            ContentResolver cr,
            Bitmap source,
            long id,
            float width,
            float height,
            int kind) {

        // create the matrix to scale it
        Matrix matrix = new Matrix();

        float scaleX = width / source.getWidth();
        float scaleY = height / source.getHeight();

        matrix.setScale(scaleX, scaleY);

        Bitmap thumb = Bitmap.createBitmap(source, 0, 0,
                source.getWidth(),
                source.getHeight(), matrix,
                true
        );

        ContentValues values = new ContentValues(4);
        values.put(MediaStore.Images.Thumbnails.KIND,kind);
        values.put(MediaStore.Images.Thumbnails.IMAGE_ID,(int)id);
        values.put(MediaStore.Images.Thumbnails.HEIGHT,thumb.getHeight());
        values.put(MediaStore.Images.Thumbnails.WIDTH,thumb.getWidth());

        Uri url = cr.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream thumbOut = cr.openOutputStream(url);
            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
            thumbOut.close();
            return thumb;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }


    public static int getAPNType(Context context){

        int netType = -1;

        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();



        if(networkInfo==null){

            return netType;

        }

        int nType = networkInfo.getType();

        if(nType==ConnectivityManager.TYPE_MOBILE){

            if(networkInfo.getExtraInfo().toLowerCase().equals("cmnet")){

                netType = 3;

            }

            else{

                netType = 2;

            }

        }

        else if(nType==ConnectivityManager.TYPE_WIFI){

            netType = 1;

        }

        return netType;

    }
}









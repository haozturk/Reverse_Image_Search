package com.example.tony.postrequest;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 100;

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final String REQUESTTAG = "CancelTag";
    private static final String TAG = "MainActivity";


    //private String url = "http://192.168.1.45:5000/search";
    //private String uploadUrl = "http://192.168.1.45:5000/upload";

    //private String url = "https://peius.serveo.net/search";
    //private String uploadUrl = "https://peius.serveo.net/upload";

    private String url = "http://198af7cb.ngrok.io/search";
    private String uploadUrl = "http://198af7cb.ngrok.io/upload";

    //private String url = "https://dito.serveo.net/search";
    //private String uploadUrl = "https://dito.serveo.net/upload";

    private Bitmap bitmap;

    Button tonysButton;
    Button cameraButton;
    RequestQueue requestQueue;
    ArrayList<String> imageLinks = new ArrayList<String>();
    EditText urlInput;
    TextView best_guess;
    Uri imageUri;

    Button urlButton;

    boolean urlEntered = false;


    String uploadedImageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.relative);

        urlButton = (Button) findViewById(R.id.urlButton);



        tonysButton = (Button) findViewById(R.id.tonysButton);
        cameraButton = (Button) findViewById(R.id.cameraButton);
        urlInput = (EditText) findViewById(R.id.urlInput);
        best_guess = (TextView) findViewById(R.id.best_guess);
        requestQueue = RequestQueueSingleton.getInstance(this.getApplicationContext())
                .getRequestQueue();

        if(!hasCamera()) {
            cameraButton.setEnabled(false);
            Toast.makeText(getApplicationContext(), "Device does not have any camera!", Toast.LENGTH_LONG).show();
        }

        tonysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: Button Clicked");
                sendPostRequestAndPrintResponse();
            }
        });

    }

    public void onClickUrl(View view){

        urlButton.setVisibility(View.GONE);
        urlInput.setVisibility(View.VISIBLE);
        urlEntered = true;
        urlButton.setEnabled(false);
        Log.i(TAG, "onClickUrl: Clicked" );

    }

    //Checks if the user has a camera
    public boolean hasCamera(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    public void onClickLaunchCamera(View view){

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Take a picture and pass the result along onActivityResult
        startActivityForResult(intent,REQUEST_IMAGE_CAPTURE);

    }


    public void onClickImagesButton(View view) {
        Intent i = new Intent(this, ListView.class);
        i.putStringArrayListExtra("imageLinks", imageLinks);
        startActivity(i);
        imageLinks.clear();
    }

    public void onClickChoose(View view) {
        Intent gallery = new Intent();
        gallery.setType("image/*");
        gallery.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE && data != null) {
            imageUri = data.getData();
            //Use image here
            Log.i(TAG, "onActivityResult: " + "Image picked");

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                Toast.makeText(getApplicationContext(), "Image chose from gallery", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            //Get the photo
            Bundle extras = data.getExtras();
            bitmap = (Bitmap) extras.get("data");
            Toast.makeText(getApplicationContext(), "Image taken from camera", Toast.LENGTH_LONG).show();
            //tonysImage.setImageBitmap(photo);
        }
    }


    public void onClickUpload(View view) {


        if(bitmap!=null){
            JSONObject json = new JSONObject();
            try {
                json.put("name","image name");
                json.put("bitmap",imageToString(bitmap));
            } catch (JSONException e) {
                Log.i(TAG, "bitmapError: ");
                e.printStackTrace();
            }

            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, uploadUrl, json,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            Log.i(TAG, "onResponseUpload: " + response.toString());

                            try {
                                uploadedImageName = response.getString("imageName");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Toast.makeText(MainActivity.this,"Uploaded successfully",Toast.LENGTH_LONG).show();

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), "Server Error, run the server!", Toast.LENGTH_LONG).show();
                            Log.i(TAG, "onErrorResponseUpload: " + error.getMessage());
                            error.printStackTrace();
                        }
                    })

            {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Content-type", "application/json");

                    return params;
                }
            };

            Volley.newRequestQueue(this).add(jsonRequest);
        }
        else{
            Toast.makeText(getApplicationContext(), "Please upload a photo", Toast.LENGTH_LONG).show();
        }
    }

    private String imageToString(Bitmap bitmap) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imgBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgBytes, Base64.DEFAULT);

    }


    private void sendPostRequestAndPrintResponse() {

        JSONObject json = new JSONObject();
        try {

            if(urlEntered){
                json.put("image_url", urlInput.getText());
                urlEntered = false;
            }
            else{
                json.put("image_url", "http://198af7cb.ngrok.io/" + uploadedImageName + ".jpeg");
                Log.i(TAG, "URL: " + "http://198af7cb.ngrok.io/" + uploadedImageName + ".jpeg");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(com.android.volley.Request.Method.POST, url, json,
                new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: " + response.toString());

                        try{

                            JSONArray jsonArray = response.getJSONArray("responses");
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            JSONObject webDetection = jsonObject.getJSONObject("webDetection");
                            JSONArray visuallySimilarImages = webDetection.getJSONArray("visuallySimilarImages");
                            JSONArray bestGuessLabels = webDetection.getJSONArray("bestGuessLabels");

                            for(int i=0;i<visuallySimilarImages.length();i++){
                                JSONObject jsonUrl = visuallySimilarImages.getJSONObject(i);
                                String stringUrl = jsonUrl.getString("url");
                                imageLinks.add(stringUrl);
                            }

                            JSONObject bestGuessJson = bestGuessLabels.getJSONObject(0);
                            String stringBestGuess = bestGuessJson.getString("label");
                            stringBestGuess = stringBestGuess.toUpperCase();
                            best_guess.setText(stringBestGuess);

                        }catch (JSONException e){
                            e.printStackTrace();
                        }

                        for (int j = 0; j < imageLinks.size(); j++) {
                            Log.i(TAG, "Links: " + imageLinks.get(j) + "\n");
                        }

                        if(imageLinks.size()==0){
                            Toast.makeText(getApplicationContext(), "An error occurred, try again!", Toast.LENGTH_LONG).show();
                            Log.i(TAG, "onResponse: imageLinks Size = 0");
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "Searched successfully", Toast.LENGTH_LONG).show();
                        }


                        /*
                        try {
                            JSONArray jsonArray = response.getJSONArray("similar_images");
                            String jsonString = response.getString("best_guess");
                            jsonString = jsonString.toUpperCase();
                            best_guess.setText(jsonString);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                imageLinks.add(jsonArray.getString(i));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        for (int j = 0; j < imageLinks.size(); j++) {
                            Log.i(TAG, "Links: " + imageLinks.get(j) + "\n");
                        }
                        if(imageLinks.size()==0)
                            Toast.makeText(getApplicationContext(), "An error occured, try again!", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getApplicationContext(), "Searched succesfully", Toast.LENGTH_LONG).show();
                        */

                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "An error occured, try again!", Toast.LENGTH_LONG).show();
                Log.i(TAG, "onErrorResponse: " + error.getMessage());
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-type", "application/json");

                return params;
            }
        };

        jsonObjectRequest.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 100000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 100000;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {
                Log.i(TAG, "retry: " + error.getMessage().toString());
            }
        });

        Volley.newRequestQueue(this).add(jsonObjectRequest);
        //requestQueue.add(jsonObjectRequest);
        jsonObjectRequest.setTag(REQUESTTAG);

    }


}
package com.mahc.custombottomsheet;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.android.volley.toolbox.Volley.newRequestQueue;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    //CONSTANTS
    private final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int DEFAULT_ZOOM = 12;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    private GoogleMap mMap;
    private ClusterManager<Antenna> mClusterManager;
    private ArrayList<Antenna> AntennaCollection = new ArrayList<>();
    private String user;
    private Antenna selectedAntenna = null;
    private String obj;
    private int objnr;
    TextView bottomSheetTextView;
    View bottomSheet;
    BottomSheetBehavior behavior;
    ProgressDialog progressDialog ;

    //IMG SERVER STUFF
    String ServerURL = "http://gastroconsultung-catering.com/getData.php";
    String ImageNameFieldOnServer = "image_name" ;
    String ImagePathFieldOnServer = "image_path" ;
    boolean check = true;
    private String currentPhotoPath;
    private int page;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get the Username from Login activity
        Intent suc = super.getIntent();
        user = suc.getStringExtra("User");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        //Download Antennas
        downloadCSVVolley();

        //Request Permissions
        requestCameraPermission();
        requestLocationPermission();

        CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinatorlayout);
        bottomSheet = coordinatorLayout.findViewById(R.id.bottom_sheet);
        behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        final RelativeLayout searchbar = coordinatorLayout.findViewById(R.id.Searchbar);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                        searchbar.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED: {
                        searchbar.setVisibility(View.GONE);
                    }
                    break;
                    case BottomSheetBehavior.STATE_COLLAPSED: {
                        searchbar.setVisibility(View.VISIBLE);
                    }
                    break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        searchbar.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
        bottomSheetTextView = bottomSheet.findViewById(R.id.bottom_sheet_title);
        //ItemPagerAdapter adapter = new ItemPagerAdapter(this,mDrawables);
        //ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        //viewPager.setAdapter(adapter);

        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        //behavior.setCollapsible(false);

        final TextView searchTextView = findViewById(R.id.txt_search);

        searchTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    if(!searchMarker(textView.getText().toString())){
                        Toast.makeText(getApplicationContext(), getString(R.string.error_antenna_not_found), Toast.LENGTH_SHORT).show();
                    }
                    hideSoftKeyboard();
                    return true;
                }
                return false;
            }
        });
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mClusterManager = new ClusterManager<Antenna>(this,mMap);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                //behavior.setState(BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED);
            }
        });

        final CustomClusterRenderer renderer = new CustomClusterRenderer(this, mMap, mClusterManager);

        mClusterManager.setRenderer(renderer);

        //MARKER LISTENER
        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<Antenna>() {
            @Override
            public boolean onClusterItemClick(Antenna item) {
                displayAntenna(item);
                return true;
            }
        });

        // Show Vietnam
        LatLng vietnam = new LatLng(16, 106.5);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnam,5.5f));
    }
    //PERMISSION STUFF
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestCameraPermission(){
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    MY_CAMERA_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if(!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "Please grant the location permission", REQUEST_LOCATION_PERMISSION, perms);
        }
    }
    //SEARCH FOR ANTENNA
    private boolean searchMarker(String searchtext){
        for (Antenna ant : AntennaCollection) {
            String tit = ant.getTitle().toLowerCase(Locale.ROOT);
            String ext = ant.getExtTitle().toLowerCase(Locale.ROOT);
            if(searchtext.length() > 4 && (tit.contains(searchtext.toLowerCase(Locale.ROOT))
                    || ext.contains(searchtext.toLowerCase(Locale.ROOT)) )){
                displayAntenna(ant);
                return true;
            }
        }
        return false;
    }
    //DISPLAY ANTENNA-DATA ON BOTTOMSHEET
    private void displayAntenna(Antenna item){
        selectedAntenna = item;
        behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(item.getPosition().latitude - 0.02,item.getPosition().longitude))
                .zoom(DEFAULT_ZOOM).build();
        //Zoom in and animate the camera.
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//BOTTOMSHEET STUFF
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_antenne);
        RoundedBitmapDrawable roundedPic = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
        final float roundPx = (float) bitmap.getWidth() * 0.06f;
        roundedPic.setCornerRadius(roundPx);

        ImageView Pic = findViewById(R.id.bottom_sheet_pic);
        TextView Title = findViewById(R.id.bottom_sheet_title);
        TextView Address = findViewById(R.id.bottom_sheet_address);
        TextView extTitle = findViewById(R.id.bottom_sheet_ext_title);
        final Spinner spin1 = findViewById(R.id.spinner1);
        spin1.setEnabled(false);
        spin1.setSelection(0);
        final Spinner spin2 = findViewById(R.id.spinner2);
        spin2.setEnabled(false);
        spin2.setSelection(0);
        final Spinner spin3 = findViewById(R.id.spinner3);
        spin3.setEnabled(false);
        spin3.setSelection(0);

        final ImageButton obj1_pic = findViewById(R.id.obj1_pic);
        obj1_pic.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_dummy1));
        final ImageButton obj2_pic = findViewById(R.id.obj2_pic);
        obj2_pic.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_dummy1));
        final ImageButton obj3_pic = findViewById(R.id.obj3_pic);
        obj3_pic.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_dummy1));


        String get_url = getResources().getString(R.string.server_url) + "getLatest.php?antenna_ID=\"" + item.getTitle() + "\"";
        RequestQueue queue = newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String[] res = response.split("\n");
                        if(!response.equals("")) {
                            for (String s : res) {
                                String module = s.split("###")[0];
                                String imgpath = getResources().getString(R.string.server_url) + s.split("###")[1];
                                if(!imgpath.equals(getResources().getString(R.string.server_url))) {
                                    if (module.equals(obj1_pic.getTag().toString()) & !imgpath.equals(getResources().getString(R.string.server_url))) {
                                        Picasso.with(obj1_pic.getContext()).load(imgpath).into(obj1_pic);
                                        spin1.setSelection(Integer.parseInt(s.split("###")[2]));
                                        spin1.setEnabled(true);
                                    }
                                    if (module.equals(obj2_pic.getTag().toString()) && !imgpath.equals(getResources().getString(R.string.server_url))) {
                                        Picasso.with(obj2_pic.getContext()).load(imgpath).into(obj2_pic);
                                        spin2.setSelection(Integer.parseInt(s.split("###")[2]));
                                        spin2.setEnabled(true);
                                    }
                                    if (module.equals(obj3_pic.getTag().toString()) && !imgpath.equals(getResources().getString(R.string.server_url))) {
                                        Picasso.with(obj3_pic.getContext()).load(imgpath).into(obj3_pic);
                                        spin3.setSelection(Integer.parseInt(s.split("###")[2]));
                                        spin3.setEnabled(true);
                                    }
                                }
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,"NetworkCall Error: " + error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

        spin1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){
                httpGETupdate(selectedAntenna.getTitle(), spin1.getTag().toString(), user, Integer.toString(pos));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){
                //Another interface callback
            }

        });

        spin2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                httpGETupdate(selectedAntenna.getTitle(), spin2.getTag().toString(), user, Integer.toString(pos));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spin3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                httpGETupdate(selectedAntenna.getTitle(), spin3.getTag().toString(), user, Integer.toString(pos));
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        Pic.setImageDrawable(roundedPic);
        extTitle.setText(item.getExtTitle());
        Title.setText(item.getTitle());
        Address.setText(item.getAddress());
    }
    //DOWNLOAD N PARSE ANTENNA DATA
    private void downloadCSVVolley(){
        progressDialog = ProgressDialog.show(MainActivity.this,"Download Antennas","Please Wait",false,false);
        String get_url = getResources().getString(R.string.URL_antennas);
        RequestQueue queue = newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(String response) {
                        String convtmp = "";
                        convtmp = new String(response.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                        ArrayList<String>  lstAntennas = new ArrayList<String>(Arrays.asList(convtmp.split("\r\n")));
                        if(!lstAntennas.isEmpty()){
                            parsePins(lstAntennas);
                        }else{
                            Toast.makeText(getBaseContext(), "Network Problem! No Antenna Data", Toast.LENGTH_LONG).show();
                        }
                        progressDialog.dismiss();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,"NetworkCall Error: " + error.getMessage(),Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void httpGETupdate(String antenna_ID, String modul, String user, String status){
        String get_url = getResources().getString(R.string.server_url) + "upload.php?status=\"" + status
                                                                        + "\"&antenna_ID=\"" + antenna_ID
                                                                        + "\"&module=\"" + modul
                                                                        + "\"&user=\"" + user
                                                                        + "\"";
        RequestQueue queue = newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,"NetworkCall Error: " + error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    //PARSE ANTENNAS FROM CSV
    public void parsePins(ArrayList<String> tmp) {
        String csvDelimiter = ";";
        //ArrayList<String> tmp = FileHelper.ReadFile(this, filepath);

        tmp.remove(0);
        for (String line : tmp) {
            String[] lineArr = line.split(csvDelimiter);
            try {
                String ID = lineArr[0];
                String extID = lineArr[1];
                String Address = lineArr[3] + ", " + lineArr[4] + ", " + lineArr[5];
                String klat = lineArr[7].replace(',', '.');
                String klong = lineArr[6].replace(',', '.');

                if (!klat.equalsIgnoreCase("#NV") && !klong.equalsIgnoreCase("#NV")) {
                    //Create Antenna and add to Collection
                    Antenna tmp_ant = new Antenna(Double.parseDouble(klat), Double.parseDouble(klong), ID, Address, extID);

                    AntennaCollection.add(tmp_ant);
                    mClusterManager.addItem(tmp_ant);
                    //change clustered Icons: https://stackoverflow.com/questions/36522305/android-cluster-manager-icon-depending-on-type
                }
            }catch(Exception e){
                System.out.println(">>>>>>Error @ Parsing");
            }
        }
        //ArrayList<Marker> MarkerList = new ArrayList<Marker>(mClusterManager.getMarkerCollection().getMarkers());
        ArrayList<String> IDstrings = new ArrayList<String>();
        IDstrings.add("");
        for (Antenna a: AntennaCollection) {
            IDstrings.add(a.getTitle());
        }
        Spinner spinner_antenna = findViewById(R.id.spinner_antennas);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,IDstrings);
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);

        spinner_antenna.setAdapter(adapter);

        //Antenna selected
        spinner_antenna.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){
                TextView txt_search = findViewById(R.id.txt_search);
                txt_search.setText(parent.getItemAtPosition(pos).toString());
                try {
                    displayAntenna(AntennaCollection.get(pos-1));
                }catch(Exception ex){}
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){
                //Another interface callback
            }

        });

    }
    //NAVIGATION
    public void getDirectionsTo(View view) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+selectedAntenna.getPosition().latitude+","+selectedAntenna.getPosition().longitude));
        startActivity(intent);
    }

    public void startObjectActivity(View view){
        Intent intent = new Intent(MainActivity.this, ObjectActivity.class);
        //Pass Object number to get to right tab
        switch (view.getId()) {
            case R.id.obj1_pic:
                page = 0;
                break;
            case R.id.obj2_pic:
                page = 1;
                break;
            case R.id.obj3_pic:
                page = 2;
                break;
        }
        intent.putExtra("One", page);
        startActivity(intent);
    }

    //START CAMERA
    public void dispatchTakePictureIntent(View view) {
        Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        switch (view.getId()){
            case R.id.obj1_pic:
                obj = getResources().getString(R.string.Object1);
                objnr = R.string.Object1;
                break;
            case R.id.obj2_pic:
                obj = getResources().getString(R.string.Object2);
                objnr = R.string.Object2;
                break;
            case R.id.obj3_pic:
                obj = getResources().getString(R.string.Object3);
                objnr = R.string.Object3;
                break;
        }

        //THUMBNAIL
        if(imageIntent.resolveActivity(getPackageManager())!=null){
            startActivityForResult(imageIntent, REQUEST_IMAGE_CAPTURE);
        }
/*
        //FULL SIZE PHOTO
        if (imageIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (photoFile != null) {
                try {
                    Uri photoURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName(), photoFile);
                    imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(imageIntent, REQUEST_IMAGE_CAPTURE);
                }catch(Exception e){
                    e.printStackTrace();
                }

            }
        }

      */

    }
    //CAMERA RESULT
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ByteArrayOutputStream byteArrayOutputStreamObject = new ByteArrayOutputStream();
            ImageButton campic = findViewById(objnr);
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            //campic.setImageBitmap(imageBitmap);

            try {
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStreamObject);
                byte[] byteArrayVar = byteArrayOutputStreamObject.toByteArray();
                uploadImgByteArray(byteArrayVar);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
/*
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            ByteArrayOutputStream byteArrayOutputStreamObject = new ByteArrayOutputStream();
            Uri uri = data.getData();
            //show Thumbnail
            ImageView imageView = (ImageView) findViewById(R.id.cam_pic);
            imageView.setImageBitmap((Bitmap)data.getExtras().get("data"));

            try {
                // Adding captured image in bitmap.
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStreamObject);
                byte[] byteArrayVar = byteArrayOutputStreamObject.toByteArray();
                uploadImgByteArray(byteArrayVar);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        */
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    //UPLOAD IMG TO SERVER
    private void uploadImgByteArray(byte[] b_arr){
        final String ConvertImage = Base64.encodeToString(b_arr, Base64.DEFAULT);

        class AsyncTaskUploadClass extends AsyncTask<Void,Void,String> {

            @Override
            protected void onPreExecute() {

                super.onPreExecute();

                // Showing progress dialog at image upload time.
                progressDialog = ProgressDialog.show(MainActivity.this,"Image is Uploading","Please Wait",false,false);
            }

            @Override
            protected void onPostExecute(String string1) {

                super.onPostExecute(string1);

                // Dismiss the progress dialog after done uploading.
                progressDialog.dismiss();
                displayAntenna(selectedAntenna);
                // Printing uploading success message coming from server on android app.
                Toast.makeText(MainActivity.this,string1,Toast.LENGTH_LONG).show();
            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            protected String doInBackground(Void... params) {

                HTTPProcessClass httpProcessClass = new HTTPProcessClass();

                HashMap<String,String> HashMapParams = new HashMap<String,String>();

                HashMapParams.put(ImageNameFieldOnServer, selectedAntenna.getTitle()
                                                            + "_" + obj //ADD MODULE NAME
                                                            + "_" + user);

                HashMapParams.put(ImagePathFieldOnServer, ConvertImage);

                String FinalData = httpProcessClass.ImageHttpRequest(getString(R.string.upload_script), HashMapParams);

                return FinalData;
            }
        }


        AsyncTaskUploadClass AsyncTaskUploadClassOBJ = new AsyncTaskUploadClass();
        AsyncTaskUploadClassOBJ.execute();
    }

    public class HTTPProcessClass{
        public String LastImgHttpRequest(String reqURL){
            try {
                URL url = new URL(reqURL);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                HttpURLConnection mUrlConnection = (HttpURLConnection) url.openConnection();
                mUrlConnection.setDoInput(true);
                int RC = mUrlConnection.getResponseCode();
                if (RC == HttpsURLConnection.HTTP_OK) {
                    InputStream is = new BufferedInputStream(mUrlConnection.getInputStream());
                    int i = is.read();
                    while (i != -1) {
                        bo.write(i);
                        i = is.read();
                    }
                }
                return bo.toString();
            }catch (Exception e){
                e.printStackTrace();
                return "";
            }


            /*StringBuilder stringBuilder = new StringBuilder();
            try {
                URL url;
                HttpURLConnection httpURLConnectionObject ;
                OutputStream OutPutStream;
                BufferedReader bufferedReaderObject ;
                int RC ;
                url = new URL(reqURL);
                httpURLConnectionObject = (HttpURLConnection) url.openConnection();
                httpURLConnectionObject.setReadTimeout(19000);
                httpURLConnectionObject.setConnectTimeout(19000);
                httpURLConnectionObject.setRequestMethod("GET");
                httpURLConnectionObject.setDoInput(true);
                httpURLConnectionObject.setDoOutput(true);
                RC = httpURLConnectionObject.getResponseCode();
                if (RC == HttpsURLConnection.HTTP_OK) {

                    bufferedReaderObject = new BufferedReader(new InputStreamReader(httpURLConnectionObject.getInputStream()));

                    stringBuilder = new StringBuilder();

                    String RC2;

                    while ((RC2 = bufferedReaderObject.readLine()) != null){
                        stringBuilder.append(RC2);
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            return stringBuilder.toString();
            */
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public String ImageHttpRequest(String requestURL, HashMap<String, String> PData) {

            StringBuilder stringBuilder = new StringBuilder();

            try {

                URL url;
                HttpURLConnection httpURLConnectionObject ;
                OutputStream OutPutStream;
                BufferedWriter bufferedWriterObject ;
                BufferedReader bufferedReaderObject ;
                int RC ;

                url = new URL(requestURL);

                httpURLConnectionObject = (HttpURLConnection) url.openConnection();

                httpURLConnectionObject.setReadTimeout(19000);

                httpURLConnectionObject.setConnectTimeout(19000);

                httpURLConnectionObject.setRequestMethod("POST");

                httpURLConnectionObject.setDoInput(true);

                httpURLConnectionObject.setDoOutput(true);

                OutPutStream = httpURLConnectionObject.getOutputStream();

                bufferedWriterObject = new BufferedWriter(

                        new OutputStreamWriter(OutPutStream, StandardCharsets.UTF_8));

                bufferedWriterObject.write(bufferedWriterDataFN(PData));

                bufferedWriterObject.flush();

                bufferedWriterObject.close();

                OutPutStream.close();

                RC = httpURLConnectionObject.getResponseCode();

                if (RC == HttpsURLConnection.HTTP_OK) {

                    bufferedReaderObject = new BufferedReader(new InputStreamReader(httpURLConnectionObject.getInputStream()));

                    stringBuilder = new StringBuilder();

                    String RC2;

                    while ((RC2 = bufferedReaderObject.readLine()) != null){

                        stringBuilder.append(RC2);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return stringBuilder.toString();
        }

        private String bufferedWriterDataFN(HashMap<String, String> HashMapParams) throws UnsupportedEncodingException {

            StringBuilder stringBuilderObject;

            stringBuilderObject = new StringBuilder();

            for (Map.Entry<String, String> KEY : HashMapParams.entrySet()) {

                if (check)

                    check = false;
                else
                    stringBuilderObject.append("&");

                stringBuilderObject.append(URLEncoder.encode(KEY.getKey(), "UTF-8"));

                stringBuilderObject.append("=");

                stringBuilderObject.append(URLEncoder.encode(KEY.getValue(), "UTF-8"));
            }

            return stringBuilderObject.toString();
        }

    }
    //MOVE TO OWN LOCATION
    private void getAndMoveToDeviceLocation(){
        Log.d("", "getDeviceLocation: getting the devices current location");

        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful() && task.getResult() != null) {
                            Log.d("", "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM));
                        }else{
                            Log.d("", "current location is null");
                            Toast.makeText(getBaseContext(), "Location not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }else{
                requestLocationPermission();
            }
        }catch (SecurityException e){
            Log.e("", "getDeviceLocation: SecurityException: " +e.getMessage());
        }
    }
    //KEYBOARD HIDE
    public void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    //CUSTOM_MARKER_ICON
    public class CustomClusterRenderer extends DefaultClusterRenderer<Antenna> {

        private final Context mContext;

        public CustomClusterRenderer(Context context, GoogleMap map,
                                     ClusterManager<Antenna> clusterManager) {
            super(context, map, clusterManager);

            mContext = context;
        }

        @Override protected void onBeforeClusterItemRendered(Antenna item,
                                                             MarkerOptions markerOptions) {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_antenna_icon)).snippet(item.getTitle());
        }
    }



}
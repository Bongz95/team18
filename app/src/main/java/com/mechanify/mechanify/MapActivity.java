package com.mechanify.mechanify;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;
    private FusedLocationProviderClient mFusedLocationProvideClient;
    private boolean mLocationPermissionGranted = false;
    private  final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 1;
    private GoogleMap mMap;
    private Location mLastKnownLocation = null;
    private LatLng mDefaultLocation = new LatLng(-26.2041,28.0473);
    private final float DEFAULT_ZOOM = 15f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mGeoDataClient = Places.getGeoDataClient(this);
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this);
        mFusedLocationProvideClient = LocationServices.getFusedLocationProviderClient(this);//Determines users locations

    }

    private void getLocationPermission(){
        //Checking location permissions
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            mLocationPermissionGranted = true;
            updateLocationUI();
            Log.d(TAG, "Location Granted");

        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setTrafficEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.setIndoorEnabled(false);
        try{
            //setting google map style using JSON Styles
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,R.raw.style_json));
            if(!success){
                Log.e(TAG, "Style parsing failed");
            }
        }catch (Resources.NotFoundException e){
            Log.e(TAG,"Can't find style. Error ", e);
        }
        getLocationPermission();
        getDeviceLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode){
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mLocationPermissionGranted =true;

                }
        }

        updateLocationUI();
    }

    public void updateLocationUI(){
        if(mMap == null){
            return;
        }
        Log.d(TAG, "Updating UI");
        try{
            if(mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }else{
                mMap.setMyLocationEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        }catch (SecurityException e){
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void getDeviceLocation(){
        try{
            if(mLocationPermissionGranted){
                final Task<Location> locationResult = mFusedLocationProvideClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if(task.isSuccessful() && (mLastKnownLocation = task.getResult()) != null){

                            LatLng currLoc = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                            moveCamera(currLoc, DEFAULT_ZOOM);
                            new Directions(MapActivity.this, mMap).execute(currLoc);
                            Toast.makeText(MapActivity.this, "We have found your location", Toast.LENGTH_SHORT).show();
                        }else{
                            Log.d(TAG,"Current location is null using defaults");
                            Log.e(TAG,"Exception %s",task.getException());
                            moveCamera(mDefaultLocation, DEFAULT_ZOOM);
                            mMap.addMarker(new MarkerOptions().position(mDefaultLocation));
                            Toast.makeText(MapActivity.this, "We are unable to detect your location", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }
        }catch (SecurityException e){
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void moveCamera(LatLng loc, float zoom){
        if(mMap != null){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, zoom));

        }else{
            Log.e(TAG, "Error. Google Map not initialized!");
        }
    }
}

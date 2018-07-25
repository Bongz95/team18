package com.mechanify.mechanify;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Directions extends AsyncTask<LatLng, Void, String> {

    private Context context;
    private URL url;
    AlertDialog.Builder alertBuilder;
    GoogleMap gMap;
    LatLng startLocation;
    int index = -1;
    int next = 1;

    LatLng start;
    LatLng end;
    public Directions(Context context, GoogleMap map){
        this.gMap = map;
        this.context = context;
        try {
            url = new URL("https://maps.googleapis.com/maps/api/directions/json?");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onPostExecute(String s) {

        super.onPostExecute(s);

        try {
            JSONObject response = new JSONObject(s);
            JSONArray stepsArray = response.getJSONArray("routes").getJSONObject(0).getJSONArray("legs")
                    .getJSONObject(0).getJSONArray("steps");
            JSONObject destination = response.getJSONArray("routes").getJSONObject(0).getJSONArray("legs")
                    .getJSONObject(0).getJSONObject("end_location");
            final LatLng end_location = new LatLng(destination.getDouble("lat"), destination.getDouble("lng"));
            int count = stepsArray.length();
            String[] polylines = new String[count];
            final LinkedList<LatLng> pointsList = new LinkedList<>();
            for(int i = 0; i < count; i++){

                JSONObject objPolyline = stepsArray.getJSONObject(i);
                String points = objPolyline.getJSONObject("polyline").getString("points");

                PolylineOptions strokePolyline = new PolylineOptions();
                strokePolyline.width(8);
                strokePolyline.color(context.getResources().getColor(R.color.colorPrimaryDark));
                strokePolyline.startCap(new RoundCap());
                strokePolyline.endCap(new RoundCap());
                strokePolyline.addAll(PolyUtil.decode(points));
                strokePolyline.jointType(JointType.ROUND);
                gMap.addPolyline(strokePolyline);

                PolylineOptions primaryPolyline = new PolylineOptions();
                primaryPolyline.width(5);
                primaryPolyline.color(context.getResources().getColor(R.color.colorPrimary));
                primaryPolyline.startCap(new RoundCap());
                primaryPolyline.endCap(new RoundCap());
                primaryPolyline.addAll(PolyUtil.decode(points));
                primaryPolyline.jointType(JointType.ROUND);
                gMap.addPolyline(primaryPolyline);
                List<LatLng> list = primaryPolyline.getPoints();
                for(int x = 0; x < list.size(); x++){
                    pointsList.addFirst(list.get(x));
                }
            }
            final Marker marker = gMap.addMarker(new MarkerOptions().position(end_location).flat(true).icon(BitmapDescriptorFactory.fromResource(
                    R.drawable.mechanic
            )));

            final Handler handler = new Handler();


            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(index < pointsList.size()-1){
                        index++;
                        next = index+1;
                    }
                    if(index < pointsList.size()-1){
                        start = pointsList.get(next);
                        end = pointsList.get(index);

                    }


                    final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0,1);
                    valueAnimator.setDuration(3000);
                    valueAnimator.setInterpolator(new LinearInterpolator());
                    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float v = valueAnimator.getAnimatedFraction();
                            double lat = v*start.latitude+(1-v)*end.latitude;
                            double lng = v*start.longitude+(1-v)*end.longitude;
                            LatLng newPos = new LatLng(lat,lng);
                            marker.setPosition(newPos);
                            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f));
                        }
                    });
                    valueAnimator.start();
                    handler.postDelayed(this,3000);
                }
            }, 3000);
            alertBuilder.setMessage(destination.toString());


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
    }

    @Override
    protected String doInBackground(LatLng... objects) {
        LatLng origin = objects[0];
        startLocation = origin;
        StringBuilder params = new StringBuilder();
        params.append("https://maps.googleapis.com/maps/api/directions/json?");
        params.append("origin=");
        params.append(origin.latitude);
        params.append(",");
        params.append(origin.longitude);
        params.append("&destination=11%20Hospital%20Street%20Johannesburg%20Kingsway");
        params.append("&key=");
        params.append(context.getString(R.string.api_key));


        try {

            HttpURLConnection conn = (HttpURLConnection) new URL(params.toString()).openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder stringBuilder = new StringBuilder();
            String line = "";
            while((line = reader.readLine()) != null){
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return params.toString();
    }

}

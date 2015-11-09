package com.akarthik10.locationtracker;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.support.v7.app.ActionBarActivity;
import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;


public class LocationTracker extends ActionBarActivity {
	GoogleMap googleMap;
	Context mContext;
	Spinner selectBus;
	String GLOBAL_URL = "http://utilitaire.in/location/";
	HashMap<String,Marker> mapMarkers = new HashMap<String,Marker>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_tracker);
        mContext = this;
        final Handler mHandler = new Handler();
     // Getting reference to the SupportMapFragment of activity_main.xml
        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        // Getting GoogleMap object from the fragment
        googleMap = fm.getMap();

        // Enabling MyLocation Layer of Google Map
        googleMap.setMyLocationEnabled(true);
        
        selectBus = (Spinner) findViewById(R.id.select_bus);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (true) {
                    try {
                        
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                new DoLocationRequest().execute();
                            }
                        });
                        Thread.sleep(10000);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            }
        }).start();
        
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.location_tracker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void addPin(MarkerOptions m)
    {
    	googleMap.addMarker(m);
    	Log.d("DI", m.getPosition().toString());
    }
    
    public void animateMarker(final Marker marker, final LatLng toPosition,
            final boolean hideMarker) {
    	
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = googleMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;
      
        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
            	if(startLatLng==null)
            	{
            		return;
            	}
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }
    
    private class DoLocationRequest extends AsyncTask<Void, String, String> {
    	
    	List<String> list = new ArrayList<String>();
    	@Override
        protected String doInBackground(Void... params) {
        	String result = null;
            
               
                    DefaultHttpClient   httpclient = new DefaultHttpClient(new BasicHttpParams());
                    HttpPost httppost = new HttpPost(GLOBAL_URL+"request.php");
                    httppost.setHeader("Content-type", "application/json");

                    InputStream inputStream = null;
                    
                    try {
                        HttpResponse response = httpclient.execute(httppost);           
                        HttpEntity entity = response.getEntity();

                        inputStream = entity.getContent();
                        // json is UTF-8 by default
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                        StringBuilder sb = new StringBuilder();

                        String line = null;
                        while ((line = reader.readLine()) != null)
                        {
                            sb.append(line + "\n");
                        }
                        result = sb.toString();
 
                    } catch (Exception e) { 
                        // Oops
                    }
                    finally {
                        try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
                    }
                
            
            return result;
            
        }

       

        @Override
        protected void onPreExecute() {}

       

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if(result == null)
			{
				Toast.makeText(LocationTracker.this, "No Internet Connection Available", Toast.LENGTH_LONG).show();
				return;
			}
            JSONObject jObject = null;
			try {
				jObject = new JSONObject(result);
			} catch (JSONException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

            String statusJsonString = null;
        	if(jObject == null)
			{
				Toast.makeText(LocationTracker.this, "No Internet Connection Available", Toast.LENGTH_LONG).show();
				return;
			}
			try {
				statusJsonString = jObject.getString("status");
			} catch (JSONException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
            if(statusJsonString.equals("OK")){
            	JSONArray jArray = null;
				try {
					jArray = jObject.getJSONArray("data");
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				list.add("All Routes");
				final Spinner s = (Spinner) findViewById(R.id.select_bus);
            	for (int j=0; j < jArray.length(); j++)
            	{
            	    try {
            	        JSONObject oneObject = jArray.getJSONObject(j);
            	        // Pulling items from the array
            	        Double lat = oneObject.getDouble("latitude");
            	        Double lon = oneObject.getDouble("longitude");
            	        String speed = oneObject.getString("speed");
            	        String deviceName = oneObject.getString("device");
            	        String last_update = oneObject.getString("last_update");
            	        Log.d("D", deviceName);
            	        if(mapMarkers.containsKey(deviceName))
            	        {
            	        	animateMarker(mapMarkers.get(deviceName), new LatLng(lat, lon), false);
            	        	mapMarkers.get(deviceName).setSnippet("Speed: "+ speed+ " Km/h (Updated: "+last_update+")");
                          	if(s.getSelectedItem().toString().equals(deviceName))
        	    	    	{
        	    	    		CameraPosition cameraPosition = new CameraPosition.Builder()
        	    	    	    .target(new LatLng(lat, lon))      // Sets the center of the map to Mountain View
        	    	    	    .zoom(googleMap.getCameraPosition().zoom)                   // Sets the zoom
//        	    	    	    .bearing(90)                // Sets the orientation of the camera to east
//        	    	    	    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
        	    	    	    .build(); 
              	    	        
              	    	    	
              	    	    	 googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        	    	    	}
         	    	  
            	        }
            	        else
            	        {
            	        	MarkerOptions m = new MarkerOptions()

         	        	   .position(new LatLng(lat, lon))

         	        	   .title(deviceName)
         	        	   .snippet("Speed: "+ speed+ " Km/h (Updated: "+last_update+")")
         	        	.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            	        	//Marker imarker = 
            	        			//addPin(m);
            	        	
            	        	
//            	        	mapMarkers.put(deviceName, imarker);
            	        	Marker imarker = googleMap.addMarker(m);
            	        	mapMarkers.put(deviceName, imarker);
            	        	
            	        	list.add(deviceName);

            	        	
            	        }
            	        if(list.size()!=1)
            	        {
                 	        
                 	        ArrayAdapter<String> adapter = new ArrayAdapter<String>(LocationTracker.this,
                 	                android.R.layout.simple_spinner_dropdown_item, list.toArray(new String[list.size()]));
                 	        s.setAdapter(adapter);
                 	       s.setOnItemSelectedListener(new OnItemSelectedListener() {
                 	    	    @Override
                 	    	    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                 	    	        // your code here
                 	    	    	if(mapMarkers.containsKey(s.getSelectedItem().toString()))
                 	    	    	{
                 	    	    		CameraPosition cameraPosition = new CameraPosition.Builder()
                 	    	    	    .target(mapMarkers.get(s.getSelectedItem().toString()).getPosition())      // Sets the center of the map to Mountain View
                 	    	    	    .zoom(15)                   // Sets the zoom
//                 	    	    	    .bearing(90)                // Sets the orientation of the camera to east
                 	    	    	    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                 	    	    	    .build(); 
                          	    	        
                          	    	    	
                          	    	    	 googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                 	    	    	}
                 	    	    	else
                 	    	    	{
                 	    	    		LatLngBounds.Builder builder = new LatLngBounds.Builder();
                 	    	    		for (Marker marker : mapMarkers.values()) {
                 	    	    		    builder.include(marker.getPosition());
                 	    	    		}
                 	    	    		LatLngBounds bounds = builder.build();
                 	    	    		CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 0);
                 	    	    		googleMap.moveCamera(cu);
                 	    	    		CameraUpdate cu2 = CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), (float) (googleMap.getCameraPosition().zoom - 1.0));
                   	    	    		googleMap.animateCamera(cu2);
                 	    	    	}
                 	    	
                 	    	    }

                 	    	    @Override
                 	    	    public void onNothingSelected(AdapterView<?> parentView) {
                 	    	        // your code here
                 	    	    }

								

                 	    	});
                 	
   	    	    		
     	    	    
     	    	    	      LatLngBounds.Builder builder = new LatLngBounds.Builder();
     	   	    	    		for (Marker marker : mapMarkers.values()) {
     	   	    	    		    builder.include(marker.getPosition());
     	   	    	    		}
     	    	    		LatLngBounds bounds = builder.build();
       	    	    		CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 0);
       	    	    		googleMap.moveCamera(cu);
       	    	    		CameraUpdate cu2 = CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), (float) (googleMap.getCameraPosition().zoom - 1.0));
       	    	    		googleMap.animateCamera(cu2);
     	    	    	
   	    	    		
            	        }

            	        
            	    } catch (JSONException e) {
            	    	e.printStackTrace();
            	    }
            	     
            	    
            	}
            	

            }
            else
            {
            	Log.d("D", "Not OK"+statusJsonString);
            }
		}
    }
    

}

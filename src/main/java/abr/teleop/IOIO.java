package abr.teleop;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

public class IOIO extends IOIOActivity implements Callback, SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    static final int DEFAULT_PWM = 1500, MAX_PWM = 2000, MIN_PWM = 1000;
    private static final String TAG_IOIO = "CameraRobot-IOIO";
    private static final String TAG_CAMERA = "CameraRobot-Camera";

    //navigation variables
    public float heading = 0;
    public float bearing = 0;
    //grid variables
    public boolean gridMode = true;
    public boolean autoMode = false;
    public int[][] route; // JLK = {{0, 0}, {1, 1}, {2, 2}, {3, 3}};
    public int currR = 0;
    public int currC = 0;
    RelativeLayout layoutPreview;
    TextView txtspeed_motor, txtIP;
    int speed_motor = 0;
    int pwm_pan, pwm_tilt;
    int pwm_speed, pwm_steering;
    Camera mCamera;
    Camera.Parameters params;
    SurfaceView mPreview;
    int startTime = 0;
    OrientationEventListener oel;
    OrientationManager om;
    int size;
    Bitmap bitmap;
    ByteArrayOutputStream bos;
    int w, h;
    int[] rgbs;
    boolean initialed = false;
    //variables for logging
    float[] mGrav;
    float[] mAcc;
    float[] mGyro;
    float[] mGeo;
    File rrFile;
    File jpgFile;
    File recordingFile;
    FileOutputStream fosRR;
    Boolean logging;
    Location dest_loc;
    PixelGridView pixelGrid;
    int[][] costs;
    double[][] map;
    double [][] orig_map;
    ArrayList<Location> waypoints;
    Location[][] gridLocations;
    int currRow;
    int currCol;
    //location variables
    private GoogleApiClient mGoogleApiClient;
    private Location curr_loc;
    private LocationRequest mLocationRequest;
    private LocationListener mLocationListener;
    float distance;
    TextView distanceText;
    TextView bearingText;
    TextView headingText;
    //variables for compass
    private SensorManager mSensorManager;
    private Sensor mCompass, mAccelerometer, mGeomagnetic, mGravity, mGyroscope;
    //variables passed through intent
    int startX;
    int startY;
    int endX;
    int endY;
    String mapFilename;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.ioio);

        mPreview = (SurfaceView) findViewById(R.id.preview);
        mPreview.getHolder().addCallback(this);
        mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        layoutPreview = (RelativeLayout) findViewById(R.id.layoutPreview);
        layoutPreview.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mCamera != null)
                    mCamera.autoFocus(null);
            }
        });

        om = new OrientationManager(this);

        //get values from settings activity
        startX = getIntent().getExtras().getInt("StartX");
        startY = getIntent().getExtras().getInt("StartY");
        endX = getIntent().getExtras().getInt("EndX");
        endY = getIntent().getExtras().getInt("EndY");
        mapFilename = getIntent().getExtras().getString("File");

        //read map from text file
        BufferedReader br = null;
        try {
            String sCurrentLine;
            File[] externalDirs = getExternalFilesDirs(null);
            //if(externalDirs.length > 1) {
            //br = new BufferedReader(new FileReader(externalDirs[1].getAbsolutePath() + "/mapopen.txt"));
            //} else {
            br = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory() + "/RescueRobotics/"+mapFilename));

            //}r
            // Log.d("abr_debug", "here i am 3");
            String[] dimensions = br.readLine().split(",");
            int rows = Integer.parseInt(dimensions[0]);
            int cols = Integer.parseInt(dimensions[0]);
            // costs = new int[rows][cols];
            map = new double[rows][cols];
            orig_map = new double[rows][cols];
            for (int i = 0; i < rows; i++) {
                // String[] costsRow = br.readLine().split(",");
                String[] mapRow = br.readLine().split(",");
                for (int j = 0; j < cols; j++) {
                    // costs[i][j] = Integer.parseInt(costsRow[j]);
                    map[i][j] = Double.parseDouble(mapRow[j]);
                    orig_map[i][j] = map[i][j];
                }
            }
            /*
            ghostWave gW = new ghostWave(map);
            aerType start = new aerType(4, 4, 0, 0.0);
            aerType finish = new aerType(12, 12, 0, 0.0);
            Log.d("abr_debug", "before spike wave");
            ArrayList<aerType> path = gW.spikeWave(start,finish);
            Log.d("abr_debug", "after spike wave");
            */
            // Spike wave algorithm for finding a path
            // Initialize the Ghost Wave with a map
            // Set the start and finish location
            ghostWave gW1stPass = new ghostWave(map);
            aerType start = new aerType(startX, startY, 0, 0.0);
            aerType finish = new aerType(endX, endY, 0, 0.0);
            ArrayList<aerType> path = gW1stPass.spikeWave(start,finish);

            // Because the possibility of multiple spike waves colliding exists,
            // it is necessary to make a map with the path from the first pass
            // as the lowest cost locations.
            for (int i = 0; i < map.length; i++) {
                for (int j = 0; j < map[0].length; j++) {
                    map[i][j] = 20.0;
                }
            }
            for (int i = 0; i < path.size(); i++) {
                map[path.get(i).getX()][path.get(i).getY()] = 1.0;
            }

            // Running the Ghost Wave algorithm a second time yields the best
            // path for the start and finish locations on this map.
            ghostWave gW2ndPass = new ghostWave(map);
            path = gW2ndPass.spikeWave(start,finish);

            for (int i = path.size()-1; i >= 0; i--) {
                System.out.println ((path.get(i).getX()+1) + " " + (path.get(i).getY()+1));
            }

            route = new int[path.size()][2];

            //for (int i = 0; i < path.size(); i++) { //path was traced from goal to start?
            int index = 0;
            for (int i = path.size()-1; i >= 0; i--) {
                Log.d ("abr_debug", "Path: " + path.get(i).getX() + " " + path.get(i).getY());
                route[index][0] = path.get(i).getX();
                route[index][1] = path.get(i).getY();
                index++;
            }

            gridLocations = new Location[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    String[] loc = br.readLine().split(",");
                    Location l = new Location("");
                    l.setLatitude(Double.parseDouble(loc[0]));
                    l.setLongitude(Double.parseDouble(loc[1]));
                    gridLocations[j][i] = l; // to make up for transpose in matlab
                }
            }
            waypoints = new ArrayList<Location>();
            for (int i = 0; i < route.length; i++) {
                waypoints.add(gridLocations[route[i][0]][route[i][1]]);
            }
            dest_loc = waypoints.get(0);

        } catch (IOException e) {
            Log.e(TAG_IOIO, e.getMessage());
        }
        //setup pixel grid
        pixelGrid = new PixelGridView(this);
        //int[][] cost = {{1,2,3,4},{1,2,3,4},{1,2,3,4},{1,2,3,4}};

        currR = 0;
        currC = 0;
        // pixelGrid.setGridColors(costs, route, currR, currC);
        pixelGrid.setMapColors(orig_map, route, currR, currC);
        pixelGrid.setId(View.generateViewId());
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(700, 700);
        pixelGrid.setLayoutParams(lp);

        ((LinearLayout) findViewById(R.id.leftll)).addView(pixelGrid);

        // phone must be Android 2.3 or higher and have Google Play store
        // must have Google Play Services: https://developers.google.com/android/guides/setup
        dest_loc = new Location("");
        buildGoogleApiClient();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(waypoints != null && waypoints.size()>1) {
            waypoints.remove(0);
            dest_loc = waypoints.get(0);
        }
        //set up location listener
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                Log.i("test","loc changed");
                curr_loc = location;
                distance = curr_loc.distanceTo(dest_loc);
                if(waypoints.size() <= 1){
                    pwm_speed = 1500;
                    pwm_steering = 1500;
                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                    toneG.startTone(ToneGenerator.TONE_CDMA_ABBR_REORDER, 200);
                }

                else if (curr_loc.distanceTo(waypoints.get(0)) < 10) {
                    waypoints.remove(0);
                    dest_loc = waypoints.get(0);
                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                    toneG.startTone(ToneGenerator.TONE_CDMA_PIP, 200);
                }
                float temp_bearing = location.bearingTo(dest_loc);
                if(temp_bearing < 0)
                    temp_bearing += 360;
                bearing = temp_bearing;
                int[] gp = getClosestGridpt(curr_loc, gridLocations);
                currRow = gp[0];
                currCol = gp[1];
                Log.i("test", "curr row:" + currRow);
                Log.i("test", "curr col:" + currCol);
                // pixelGrid.setGridColors(costs, route, currRow, currCol);
                pixelGrid.setMapColors(orig_map, route, currRow, currCol);
                if(autoMode) {
                    String mill_timestamp = System.currentTimeMillis()+"";
                    int[] destWaypoint = getClosestGridpt(dest_loc, gridLocations);
                    String info = mill_timestamp + "," + curr_loc.getLatitude() + "," + curr_loc.getLongitude() + ","
                            + destWaypoint[0] +"," + destWaypoint[1] + "\n";
                    try {
                        byte[] b = info.getBytes();
                        fosRR.write(b);
                        Log.i("test","wrote");
                    } catch (IOException e) {
                        Log.e(TAG_IOIO, e.toString());
                    }
                }
            }

            @SuppressWarnings("unused")
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @SuppressWarnings("unused")
            public void onProviderEnabled(String provider) {
            }

            @SuppressWarnings("unused")
            public void onProviderDisabled(String provider) {
            }
        };

        //set up compass
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        //open file and stream for writing data
/*
        try {
			Calendar calendar = Calendar.getInstance();
			java.util.Date now = calendar.getTime();
			java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(now.getTime());
			String time = currentTimestamp.toString();
			time = time.replaceAll("[|?*<\":>+\\[\\]/']", "_");

			File[] externalDirs = getExternalFilesDirs(null);
			if(externalDirs.length > 1) {
				jpgFile = new File(externalDirs[1].getAbsolutePath() + "/testfilter");
				if (!jpgFile.exists()) {
					jpgFile.mkdirs();
				}
			} else {
				jpgFile = new File(externalDirs[0].getAbsolutePath() + "/testfilter");
				if (!jpgFile.exists()) {
					jpgFile.mkdirs();
				}
			}
			recordingFile = new File(rrFile, time+".csv");

			recordingFile.createNewFile();

			fosRR = new FileOutputStream(recordingFile);
			String labels = "Time,Lat,Lon,AccX,AccY,AccZ,GyroX,GyroY,GyroZ,GeoX,GeoY,GeoZ,GravX,GravY,GravZ,Heading,PwmSpeed,PwmSteer\n";
			byte[] b = labels.getBytes();
			fosRR.write(b);
		} catch (IOException e) {
			Log.e(TAG_IOIO, e.toString());
		}
*/
        distanceText = (TextView) findViewById(R.id.distanceText);
        bearingText = (TextView) findViewById(R.id.bearingText);
        headingText = (TextView) findViewById(R.id.headingText);

        //add functionality to gridMode button
        Button buttonGrid = (Button) findViewById(R.id.btnGrid);
        buttonGrid.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!gridMode) {
                    v.setBackgroundResource(R.drawable.button_grid_on);
                    gridMode = true;
                    pixelGrid.setVisibility(View.VISIBLE);
                } else {
                    v.setBackgroundResource(R.drawable.button_grid_off);
                    gridMode = false;
                    pixelGrid.setVisibility(View.INVISIBLE);
                }
            }
        });

        autoMode = false;

        //add functionality to autoMode button
        Button buttonAuto = (Button) findViewById(R.id.btnAuto);
        buttonAuto.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!autoMode) {
                    v.setBackgroundResource(R.drawable.button_auto_on);
                    autoMode = true;
                    //open file and stream for writing data
                    try {
                        Calendar calendar = Calendar.getInstance();
                        java.util.Date now = calendar.getTime();
                        java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(now.getTime());
                        String time = currentTimestamp.toString();
                        time = time.replaceAll("[|?*<\":>+\\[\\]/']", "_");

                        File[] externalDirs = getExternalFilesDirs(null);
                        if(externalDirs.length > 1) {
                            //rrFile = new File(externalDirs[1].getAbsolutePath() + "/rescuerobotics/"+time);
                            rrFile = new File(Environment.getExternalStorageDirectory() + "/rescuerobotics/"+time);
                            if (!rrFile.exists()) {
                                rrFile.mkdirs();
                            }
                        } else {
                            //rrFile = new File(externalDirs[0].getAbsolutePath() + "/rescuerobotics/"+time);
                            rrFile = new File(Environment.getExternalStorageDirectory() + "/rescuerobotics/"+time);
                            if (!rrFile.exists()) {
                                rrFile.mkdirs();
                            }
                        }
                        recordingFile = new File(rrFile, time+".csv");
                        recordingFile.createNewFile();

                        fosRR = new FileOutputStream(recordingFile);
                        String labels = "Time,Lat,Lon,WayptR,WayptC\n";
                        byte[] b = labels.getBytes();
                        fosRR.write(b);
                        Log.i("test","made folder");
                    } catch (IOException e) {
                        Log.e(TAG_IOIO, e.toString());
                    }
                } else {
                    v.setBackgroundResource(R.drawable.button_auto_off);
                    autoMode = false;
                    try {
                        fosRR.close();
                    } catch (IOException e) {
                        Log.e(TAG_IOIO, e.toString());
                    }
                }
            }
        });

    }

    //determine whether 2 directions are roughly pointing in the same direction, correcting for angle wraparound
    public boolean sameDir(float dir1, float dir2){
        return (Math.abs((double) (dir1 - dir2)) < 22.5 || Math.abs((double) (dir1 - dir2)) > 337.5);
        //return (Math.abs((double) (dir1 - dir2)) < 45 || Math.abs((double) (dir1 - dir2)) > 315.5);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    //Method necessary for google play location services
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    //Method necessary for google play location services
    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services
        curr_loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        startLocationUpdates();
    }

    //Method necessary for google play location services
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationListener);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
    }

    //Method necessary for google play location services
    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    //Method necessary for google play location services
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the 'Handle Connection Failures' section.
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    //Called whenever the value of a sensor changes
    @Override
    public final void onSensorChanged(SensorEvent event) {
        setText("distance: "+distance, distanceText);
        setText("bearing: "+bearing, bearingText);
        setText("heading: "+heading, headingText);
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY)
            mGrav = event.values;
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            mGyro = event.values;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mAcc = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeo = event.values;
        if (mAcc != null && mGeo != null) {
            float[] temp = new float[9];
            float[] R = new float[9];
            //Load rotation matrix into R
            SensorManager.getRotationMatrix(temp, null, mAcc, mGeo);
            //Remap to camera's point-of-view
            SensorManager.remapCoordinateSystem(temp, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);
            //Return the orientation values
            float[] values = new float[3];
            SensorManager.getOrientation(R, values);
            //Convert to degrees
            for (int i = 0; i < values.length; i++) {
                Double degrees = (values[i] * 180) / Math.PI;
                values[i] = degrees.floatValue();
            }

            //heading = (fixWraparound(values[0] - 12))%360;
            if(sameDir(bearing,heading))
                Log.i("test","same dir");
            else
                Log.i("test","not same dir");
            float temp_heading = values[0];
            if(temp_heading < 0)
                temp_heading = temp_heading + 360;
            heading = (heading * 5 + fixWraparound(temp_heading - 12)) / 6; //add 12 to make up for declination in Irvine, average out from previous 2 for smoothness
        }
    }

    //Called whenever activity resumes from pause
    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        stopLocationUpdates();
        finish();

    }

    //set the text of any text view in this application
    public void setText(final String str, final TextView tv)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.setText(str);
            }
        });
    }

    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        if (mPreview == null)
            return;

        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }

        size = 10; //hack

        params = mCamera.getParameters();
        Camera.Size pictureSize = getMaxPictureSize(params);
        Camera.Size previewSize = params.getSupportedPreviewSizes().get(size);

        params.setPictureSize(pictureSize.width, pictureSize.height);
        params.setPreviewSize(previewSize.width, previewSize.height);
        params.setPreviewFrameRate(getMaxPreviewFps(params));

        Display display = getWindowManager().getDefaultDisplay();
        LayoutParams lp = layoutPreview.getLayoutParams();

        if (om.getOrientation() == OrientationManager.LANDSCAPE_NORMAL
                || om.getOrientation() == OrientationManager.LANDSCAPE_REVERSE) {
            float ratio = (float) previewSize.width / (float) previewSize.height;
            if ((int) ((float) mPreview.getWidth() / ratio) >= display.getHeight()) {
                lp.height = (int) ((float) mPreview.getWidth() / ratio);
                lp.width = mPreview.getWidth();
            } else {
                lp.height = mPreview.getHeight();
                lp.width = (int) ((float) mPreview.getHeight() * ratio);
            }
        } else if (om.getOrientation() == OrientationManager.PORTRAIT_NORMAL
                || om.getOrientation() == OrientationManager.PORTRAIT_REVERSE) {
            float ratio = (float) previewSize.height / (float) previewSize.width;
            if ((int) ((float) mPreview.getWidth() / ratio) >= display.getHeight()) {
                lp.height = (int) ((float) mPreview.getWidth() / ratio);
                lp.width = mPreview.getWidth();
            } else {
                lp.height = mPreview.getHeight();
                lp.width = (int) ((float) mPreview.getHeight() * ratio);
            }
        }

        layoutPreview.setLayoutParams(lp);
        int deslocationX = (int) (lp.width / 2.0 - mPreview.getWidth() / 2.0);
        layoutPreview.animate().translationX(-deslocationX);

        params.setJpegQuality(100);
        mCamera.setParameters(params);

        switch (om.getOrientation()) {
            case OrientationManager.LANDSCAPE_NORMAL:
                mCamera.setDisplayOrientation(0);
                break;
            case OrientationManager.PORTRAIT_NORMAL:
                mCamera.setDisplayOrientation(90);
                break;
            case OrientationManager.LANDSCAPE_REVERSE:
                mCamera.setDisplayOrientation(180);
                break;
            case OrientationManager.PORTRAIT_REVERSE:
                mCamera.setDisplayOrientation(270);
                break;
        }

        try {
            mCamera.setPreviewDisplay(mPreview.getHolder());
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void surfaceCreated(SurfaceHolder arg0) {
        try {
            mCamera = Camera.open(0);
            mCamera.setPreviewDisplay(arg0);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceDestroyed(SurfaceHolder arg0) {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    public void decodeYUV420(int[] rgb, byte[] yuv420, int width, int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420[uvp++]) - 128;
                    u = (0xff & yuv420[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    public int[] getClosestGridpt(Location curr, Location[][] locs) {
        int[] closestLoc = {0, 0};
        double dist = curr.distanceTo(gridLocations[0][0]);
        for (int i = 0; i < locs.length; i++) {
            for (int j = 0; j < locs[0].length; j++) {
                double tempDist = curr.distanceTo(gridLocations[i][j]);
                if (tempDist < dist) {
                    closestLoc[0] = i;
                    closestLoc[1] = j;
                    dist = tempDist;
                }
            }
        }
        return closestLoc;
    }

    public Camera.Size getMaxPictureSize(Camera.Parameters params) {
        List<Camera.Size> pictureSize = params.getSupportedPictureSizes();
        int firstPictureWidth, lastPictureWidth;
        try {
            firstPictureWidth = pictureSize.get(0).width;
            lastPictureWidth = pictureSize.get(pictureSize.size() - 1).width;
            if (firstPictureWidth >
                    lastPictureWidth)
                return pictureSize.get(0);
            else
                return pictureSize.get(pictureSize.size() - 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return pictureSize.get(0);
        }
    }

    public int getMaxPreviewFps(Camera.Parameters params) {
        List<Integer> previewFps = params.getSupportedPreviewFrameRates();
        int fps = 0;
        for (int i = 0; i < previewFps.size(); i++) {
            if (previewFps.get(i) > fps)
                fps = previewFps.get(i);
        }
        return fps;
    }

    //revert any degree measurement back to the -179 to 180 degree scale
    public float fixWraparound(float deg) {
        /*
        if (deg <= 180.0 && deg > -179.99)
            return deg;
        else if (deg > 180)
            return deg - 360;
        else
            return deg + 360;
        */
        if (deg > 360)
            return deg % 360;
        else if (deg < 0)
            return deg + 360;
        else
            return deg;
    }

    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }

    class Looper extends BaseIOIOLooper {
        PwmOutput speed, steering, pan, tilt;
//		int pwm_left_motor, pwm_right_motor;


        protected void setup() throws ConnectionLostException {
            pwm_speed = DEFAULT_PWM;
            pwm_steering = DEFAULT_PWM;
            pwm_pan = DEFAULT_PWM;
            //pwm_tilt = 1800;
            pwm_tilt = DEFAULT_PWM;

            speed = ioio_.openPwmOutput(3, 50);
            steering = ioio_.openPwmOutput(4, 50);
            pan = ioio_.openPwmOutput(5, 50);
            tilt = ioio_.openPwmOutput(6, 50);

            speed.setPulseWidth(pwm_speed);
            steering.setPulseWidth(pwm_steering);
            pan.setPulseWidth(pwm_pan);
            tilt.setPulseWidth(pwm_tilt);

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Connected!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void loop() throws ConnectionLostException, InterruptedException {

            driveABR();

            if (pwm_speed > MAX_PWM) pwm_speed = MAX_PWM;
            else if (pwm_speed < MIN_PWM) pwm_speed = MIN_PWM;

            if (pwm_steering > MAX_PWM) pwm_steering = MAX_PWM;
            else if (pwm_steering < MIN_PWM) pwm_steering = MIN_PWM;

            if (pwm_pan > MAX_PWM) pwm_pan = MAX_PWM;
            else if (pwm_pan < MIN_PWM) pwm_pan = MIN_PWM;

            if (pwm_tilt > MAX_PWM) pwm_tilt = MAX_PWM;
            else if (pwm_tilt < MIN_PWM) pwm_tilt = MIN_PWM;

//        	Log.e("IOIO", "pwm_left_motor: " + pwm_left_motor + " pwm_right_motor: " + pwm_right_motor+ " pwm_pan: " + pwm_pan+ " pwm_tilt: " + pwm_tilt);

            speed.setPulseWidth(pwm_speed);
            steering.setPulseWidth(pwm_steering);
            pan.setPulseWidth(pwm_pan);
            tilt.setPulseWidth(pwm_tilt);

            Thread.sleep(20);
        }

        public void driveABR() {
            final int forwardSpeed = 180;
            final int turningSpeed = 50;

            if (autoMode) {
                if (curr_loc.distanceTo(dest_loc) > 7) { // follow compass
                    pwm_speed = 1500 + forwardSpeed;
                    if(!sameDir(bearing,heading)) {
                        if (bearing >= heading) {
                            if (bearing - heading <= 180)
                                pwm_steering = 1500 + turningSpeed;
                            else
                                pwm_steering = 1500 - turningSpeed;
                        } else {
                            if (heading - bearing <= 180)
                                pwm_steering = 1500 - turningSpeed;
                            else
                                pwm_steering = 1500 + turningSpeed;
                        }
                    } else {
                        pwm_steering = 1500;
                    }
                    pwm_pan = 1500;
                    pwm_tilt = 1500;
                    Log.d("abr_debug", "dest=");
                }
                Log.d("abr_debug", "AUTO speed=" + pwm_speed + " turn=" + pwm_steering);
            }
            else {
                pwm_pan = 1500;
                pwm_tilt = 1500;
                pwm_speed = 1500;
                pwm_steering = 1500;
                Log.d("abr_debug", "R/C speed=" + pwm_speed + " turn=" + pwm_steering);

            }
        }
        public void disconnected() {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Disonnected!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void incompatible() {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Imcompatible firmware version", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}

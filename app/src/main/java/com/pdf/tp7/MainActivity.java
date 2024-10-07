package com.pdf.tp7;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    private static final double KAABA_LAT = 21.4225;
    private static final double KAABA_LONG = 39.8262;

    private TextView qiblahTextView;
    private TextView compassTextView;

    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] gravity;
    private float[] geomagnetic;

    double userLat = -1;
    double userLong = -1;


//    SensorManager.SENSOR_DELAY_NORMAL (around 200ms)
//    SensorManager.SENSOR_DELAY_UI (around 60ms)
//    SensorManager.SENSOR_DELAY_GAME (around 20ms)
//    SensorManager.SENSOR_DELAY_FASTEST (as fast as possible)
    int delay = SensorManager.SENSOR_DELAY_UI;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        qiblahTextView = findViewById(R.id.qiblahDirection);
        compassTextView = findViewById(R.id.compassDirection);

        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            // Permission is already granted, get location
            getLocationAndCalculateQiblah();
        }

        // Initialize sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndCalculateQiblah();
            }
        }
    }

    // Method to retrieve the location and calculate Qiblah direction
    private void getLocationAndCalculateQiblah() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                userLat = location.getLatitude();
                userLong = location.getLongitude();
                double qiblahDirection = calculateQiblahDirection(userLat, userLong);

                // Display the Qiblah direction
                qiblahTextView.setText(String.format("Qiblah Direction: %.2f° clockwise from North", qiblahDirection));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Check permission again before requesting location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    // Method to calculate Qiblah direction based on the user's location
    private double calculateQiblahDirection(double userLat, double userLong) {
        // Convert degrees to radians
        double userLatRad = Math.toRadians(userLat);
        double userLongRad = Math.toRadians(userLong);
        double kaabaLatRad = Math.toRadians(KAABA_LAT);
        double kaabaLongRad = Math.toRadians(KAABA_LONG);

        // Calculate the difference in longitude
        double deltaLong = kaabaLongRad - userLongRad;

        // Formula to calculate Qiblah direction
        double x = Math.sin(deltaLong);
        double y = Math.cos(userLatRad) * Math.tan(kaabaLatRad) - Math.sin(userLatRad) * Math.cos(deltaLong);

        // Calculate the Qiblah direction in radians
        double qiblahDirectionRad = Math.atan2(x, y);

        // Convert to degrees and normalize to 0-360°
        double qiblahDirectionDeg = Math.toDegrees(qiblahDirectionRad);
        return (qiblahDirectionDeg + 360) % 360;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // Register sensor listeners
        sensorManager.registerListener(this, accelerometer, delay);
        sensorManager.registerListener(this, magnetometer, delay);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister sensor listeners
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (userLat == -1 && userLong == -1)
        {
            compassTextView.setText(String.format("Fetching Currect Location..."));
        }

        else
        {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                gravity = event.values;
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                geomagnetic = event.values;
            }

            if (gravity != null && geomagnetic != null) {
                float[] R = new float[9];
                float[] I = new float[9];

                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    double azimuth = Math.toDegrees(orientation[0]); // Orientation in degrees

                    // Normalize to 0-360
                    if (azimuth < 0) {
                        azimuth += 360;
                    }

                    double qiblahBearing = calculateQiblahDirection(userLat, userLong);

                    // Calculate difference between azimuth and Qiblah bearing
                    double qiblahDirection = qiblahBearing - azimuth;

                    // Normalize the result to 0-360
                    if (qiblahDirection < 0) {
                        qiblahDirection += 360;
                    }

                    // Update UI with Qiblah direction
                    compassTextView.setText(String.format("Qiblah Direction: %.2f°", qiblahDirection));
                }
            }
        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // You can implement if needed
    }
}

package com.example.compassapp;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    Float azimuth_angle;
    private SensorManager compassSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    TextView tv_degrees;
    ImageView iv_compass;
    private float current_degree = 0f;

    float[] accel_read;
    float[] magnetic_read;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        tv_degrees = findViewById(R.id.tv_degrees);
        iv_compass = findViewById(R.id.iv_compass);

        // Initialize sensor manager and sensors
        compassSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (compassSensorManager != null) {
            accelerometer = compassSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = compassSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register sensor listeners when activity resumes
        if (accelerometer != null && magnetometer != null) {
            compassSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            compassSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister sensor listeners when activity pauses to save battery
        compassSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            accel_read = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            magnetic_read = event.values;

        if (accel_read != null && magnetic_read != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean successful_read = SensorManager.getRotationMatrix(R, I, accel_read, magnetic_read);

            if (successful_read) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth_angle = orientation[0];
                float degrees = ((azimuth_angle * 180f) / (float) Math.PI);

                // Normalize degrees to 0-360
                if (degrees < 0) {
                    degrees += 360;
                }

                int degreesInt = Math.round(degrees);
                String direction = getDirection(degrees);

                // Update the text view with both degrees and direction
                tv_degrees.setText(degreesInt + "° " + direction);

                // Rotate the compass image
                RotateAnimation rotate = new RotateAnimation(
                        current_degree,
                        -degreesInt,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );
                rotate.setDuration(100);
                rotate.setFillAfter(true);
                iv_compass.startAnimation(rotate);
                current_degree = -degreesInt;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    /**
     * Convert degrees to compass direction
     * @param degrees Current heading in degrees (0-360)
     * @return String representation of the direction
     */
    private String getDirection(float degrees) {
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

        // Calculate index (0-15) for 16-point compass
        int index = (int) Math.round(degrees / 22.5) % 16;

        // Return full direction names for better readability
        return getFullDirectionName(directions[index]);
    }

    /**
     * Convert abbreviated direction to full name
     * @param abbr Abbreviated direction (e.g., "NW", "NNE")
     * @return Full direction name
     */
    private String getFullDirectionName(String abbr) {
        switch (abbr) {
            case "N": return "North";
            case "NNE": return "North-Northeast";
            case "NE": return "Northeast";
            case "ENE": return "East-Northeast";
            case "E": return "East";
            case "ESE": return "East-Southeast";
            case "SE": return "Southeast";
            case "SSE": return "South-Southeast";
            case "S": return "South";
            case "SSW": return "South-Southwest";
            case "SW": return "Southwest";
            case "WSW": return "West-Southwest";
            case "W": return "West";
            case "WNW": return "West-Northwest";
            case "NW": return "Northwest";
            case "NNW": return "North-Northwest";
            default: return abbr;
        }
    }
}
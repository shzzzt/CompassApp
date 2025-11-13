package com.example.compassapp;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Resources
    private TextView tv_degrees;
    private ImageView iv_compass;

    // Sensors
    private SensorManager compassSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    // Calculators - reuse arrays to avoid repeated allocations
    private float[] acc_val = new float[3];
    private float[] mag_val = new float[3];
    private float[] rotation_matrix = new float[9];
    private float[] orientation = new float[3];
    private boolean has_acc_data = false;
    private boolean has_mag_data = false;

    // Low-pass filter for smoother compass rotation
    private static final float ALPHA = 0.5f;  // Lower = smoother, Higher = more responsive
    private float current_azimuth = 0f;

    // Track last update time for real-time optimization
    private long last_update_time = 0;
    private static final int UPDATE_INTERVAL_MS = 16; // ~60 FPS
    private String last_bearing_text = "";
    private int last_bearing_degrees = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        compassSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = compassSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = compassSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        tv_degrees = findViewById(R.id.tv_degrees);
        iv_compass = findViewById(R.id.iv_compass);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            compassSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (magnetometer != null) {
            compassSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        compassSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, acc_val, 0, event.values.length);
            has_acc_data = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mag_val, 0, event.values.length);
            has_mag_data = true;
        }

        if (has_acc_data && has_mag_data) {
            // Throttle updates to ~60 FPS for optimal performance
            long current_time = System.currentTimeMillis();
            if (current_time - last_update_time < UPDATE_INTERVAL_MS) {
                return;
            }
            last_update_time = current_time;

            if (SensorManager.getRotationMatrix(rotation_matrix, null, acc_val, mag_val)) {
                SensorManager.getOrientation(rotation_matrix, orientation);
                float azimuth_r = orientation[0];
                float azimuth_d = (float) Math.toDegrees(azimuth_r);

                // Normalize to 0-360
                if (azimuth_d < 0) {
                    azimuth_d += 360;
                }

                // Apply low-pass filter for smooth rotation
                current_azimuth = ALPHA * azimuth_d + (1 - ALPHA) * current_azimuth;

                // Rotate compass (negative to rotate opposite of device rotation)
                iv_compass.setRotation(-current_azimuth);

                // Optimization: Only update text if bearing changed significantly
                int current_degrees = Math.round(current_azimuth);
                if (Math.abs(current_degrees - last_bearing_degrees) >= 1) {
                    String bearing_text = getBearingText(current_azimuth);
                    if (!bearing_text.equals(last_bearing_text)) {
                        tv_degrees.setText(bearing_text);
                        last_bearing_text = bearing_text;
                    }
                    last_bearing_degrees = current_degrees;
                }
            }
        }
    }

    private String getBearingText(float azimuth_d) {
        // Round azimuth for cleaner comparisons
        int degrees = Math.round(azimuth_d);

        // Check cardinal directions with tighter tolerance
        if (degrees == 0 || degrees == 360) return "N  0°";
        if (degrees == 90) return "E 0°";
        if (degrees == 180) return "S 0°";
        if (degrees == 270) return "W 0°";

        // Determine quadrant and format bearing with angle within quadrant
        if (degrees < 90) {
            // North-East quadrant: 0-89° (same as azimuth)
            return String.format(Locale.US, "NE %d°", degrees);
        } else if (degrees < 180) {
            // South-East quadrant: 90-179° becomes 0-89°
            return String.format(Locale.US, "SE %d°", degrees - 90);
        } else if (degrees < 270) {
            // South-West quadrant: 180-269° becomes 0-89°
            return String.format(Locale.US, "SW %d°", degrees - 180);
        } else {
            // North-West quadrant: 270-359° becomes 0-89°
            return String.format(Locale.US, "NW %d°", degrees - 270);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }
}
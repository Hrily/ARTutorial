package com.hrily.artutorial;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.telecom.StatusHints;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by krzysztofjackowski on 24/09/15.
 */
public class MyCurrentAzimuth implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;
    private int azimuthFrom = 0;
    private int azimuthTo = 0;
    private OnAzimuthChangedListener mAzimuthListener;
    Context mContext;

    public MyCurrentAzimuth(OnAzimuthChangedListener azimuthListener, Context context) {
        mAzimuthListener = azimuthListener;
        mContext = context;
    }

    public void start(){
        sensorManager = (SensorManager) mContext.getSystemService(mContext.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if(sensor == null) sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        if(sensor == null){
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
            Toast.makeText(mContext, "Using Game Rotation Vector. Direction may not be accurate!", Toast.LENGTH_SHORT).show();
        }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop(){
        sensorManager.unregisterListener(this);
    }

    public void setOnShakeListener(OnAzimuthChangedListener listener) {
        mAzimuthListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        azimuthFrom = azimuthTo;

        float[] orientation = new float[3];
        float[] rMat = new float[9];
        SensorManager.getRotationMatrixFromVector(rMat, event.values);
        azimuthTo = (int) ( Math.toDegrees( SensorManager.getOrientation( rMat, orientation )[0] ) + 360 ) % 360;

//        float[] gData = new float[3]; // accelerometer
//        float[] mData = new float[3]; // magnetometer
//        float[] rMat = new float[9];
//        float[] iMat = new float[9];
//        float[] orientation = new float[3];
//        float[] data;
//        switch ( event.sensor.getType() ) {
//            case Sensor.TYPE_ACCELEROMETER:
//                gData = event.values.clone();
//                break;
//            case Sensor.TYPE_MAGNETIC_FIELD:
//                mData = event.values.clone();
//                break;
//            default: return;
//        }
//
//        if ( SensorManager.getRotationMatrix( rMat, iMat, gData, mData ) ) {
//            azimuthTo = (int) ( Math.toDegrees( SensorManager.getOrientation( rMat, orientation )[0] ) + 360 ) % 360;
//        }

        mAzimuthListener.onAzimuthChanged(azimuthFrom, azimuthTo);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

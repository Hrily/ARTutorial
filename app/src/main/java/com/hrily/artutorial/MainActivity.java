package com.hrily.artutorial;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.location.Location;
import android.support.annotation.Size;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, OnLocationChangedListener, OnAzimuthChangedListener {

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private boolean isCameraViewOn = false;
    private AugmentedPOI mPoi;

    private double mAzimuthReal = 0;
    private double mAzimuthTheoretical = 0;
    private static double AZIMUTH_ACCURACY = 25;
    private double mMyLatitude = 0;
    private double mMyLongitude = 0;

    private MyCurrentAzimuth myCurrentAzimuth;
    private MyCurrentLocation myCurrentLocation;

    TextView descriptionTextView;
    ImageView pointerIcon;
    Display display;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setupListeners();
        setupLayout();
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        display = ((android.view.WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        setupListeners();
        setupLayout();
        setAugmentedRealityPoint();

    }


    private void setAugmentedRealityPoint() {
        mPoi = new AugmentedPOI(
                "Kościół Marciacki",
                "Kościół Marciacki w Krakowie",
                50.06169631,
                19.93919566
        );
    }

    public double calculateTheoreticalAzimuth() {
        double dX = mPoi.getPoiLatitude() - mMyLatitude;
        double dY = mPoi.getPoiLongitude() - mMyLongitude;

        double phiAngle;
        double tanPhi;
        double azimuth = 0;

        tanPhi = Math.abs(dY / dX);
        phiAngle = Math.atan(tanPhi);
        phiAngle = Math.toDegrees(phiAngle);

        if (dX > 0 && dY > 0) { // I quater
            return azimuth = phiAngle;
        } else if (dX < 0 && dY > 0) { // II
            return azimuth = 180 - phiAngle;
        } else if (dX < 0 && dY < 0) { // III
            return azimuth = 180 + phiAngle;
        } else if (dX > 0 && dY < 0) { // IV
            return azimuth = 360 - phiAngle;
        }

        return phiAngle;
    }

    private List<Double> calculateAzimuthAccuracy(double azimuth) {
        double minAngle = azimuth - AZIMUTH_ACCURACY;
        double maxAngle = azimuth + AZIMUTH_ACCURACY;
        List<Double> minMax = new ArrayList<Double>();
        if (minAngle < 0)
            minAngle += 360;
        if (maxAngle >= 360)
            maxAngle -= 360;
        minMax.clear();
        minMax.add(minAngle);
        minMax.add(maxAngle);

        return minMax;
    }

    private boolean isBetween(double minAngle, double maxAngle, double azimuth) {
        if (minAngle > maxAngle) {
            if (isBetween(0, maxAngle, azimuth) && isBetween(minAngle, 360, azimuth))
                return true;
        } else {
            if (azimuth > minAngle && azimuth < maxAngle)
                return true;
        }
        return false;
    }

    private void updateDescription() {
        descriptionTextView.setText(mPoi.getPoiName() + " azimuthTheoretical "
                + mAzimuthTheoretical + " azimuthReal " + mAzimuthReal + " latitude "
                + mMyLatitude + " longitude " + mMyLongitude);
    }

    @Override
    public void onLocationChanged(Location location) {
        mMyLatitude = location.getLatitude();
        mMyLongitude = location.getLongitude();
        mAzimuthTheoretical = calculateTheoreticalAzimuth();
        Toast.makeText(this,"latitude: "+location.getLatitude()+" longitude: "+location.getLongitude(), Toast.LENGTH_SHORT).show();
        updateDescription();
    }

    @Override
    public void onAzimuthChanged(float azimuthChangedFrom, float azimuthChangedTo) {
        mAzimuthReal = azimuthChangedTo;
        mAzimuthTheoretical = calculateTheoreticalAzimuth();

        pointerIcon = (ImageView) findViewById(R.id.icon);

        double minAngle = calculateAzimuthAccuracy(mAzimuthTheoretical).get(0);
        double maxAngle = calculateAzimuthAccuracy(mAzimuthTheoretical).get(1);

        if (isBetween(minAngle, maxAngle, mAzimuthReal)) {
            float perc = ((float) (mAzimuthReal - minAngle + 360.0) % 360) / ((float) (maxAngle - minAngle + 360.0) % 360);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = display.getWidth() - (int) (display.getWidth() * perc);
            lp.topMargin = display.getHeight()/2 - pointerIcon.getHeight();
            pointerIcon.setLayoutParams(lp);
            pointerIcon.setVisibility(View.VISIBLE);
        } else {
            pointerIcon.setVisibility(View.INVISIBLE);
        }

        updateDescription();
    }

    @Override
    protected void onStop() {
        myCurrentAzimuth.stop();
        myCurrentLocation.stop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myCurrentAzimuth.start();
        myCurrentLocation.start();
    }

    private void setupListeners() {
        myCurrentLocation = new MyCurrentLocation(this);
        myCurrentLocation.buildGoogleApiClient(this);
        myCurrentLocation.start();

        myCurrentAzimuth = new MyCurrentAzimuth(this, this);
        myCurrentAzimuth.start();
    }

    private void setupLayout() {
        descriptionTextView = (TextView) findViewById(R.id.cameraTextView);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.cameraview);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        if (isCameraViewOn) {
            mCamera.stopPreview();
            isCameraViewOn = false;
        }

        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
                isCameraViewOn = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(0);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        isCameraViewOn = false;
    }

}

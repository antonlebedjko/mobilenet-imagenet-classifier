package fi.aalto.cs.sipi.tasks;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.util.Arrays;

import fi.aalto.cs.sipi.CONFIG;
import fi.aalto.cs.sipi.MainFragment;


public class LaunchCameraTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = LaunchCameraTask.class.getSimpleName();

    private MainFragment mCaller;

    private Application mApp;
    private TextureView mTexture;
    private CameraDevice mDevice;
    private CameraCaptureSession mSession;
    private CaptureRequest.Builder mBuilder;

    private long t_cam_open, t_sess_start, t_cap_req,
            t_cap_start_c, t_cap_finish_c,
            t_cap_start_s, t_cap_finish_s, t_read;

    public LaunchCameraTask(MainFragment snpe, Application app, TextureView texture) {
        mCaller = snpe;
        mApp = app;
        mTexture = texture;
    }

    @Override
    protected Void doInBackground(Void... params) {
        t_cam_open = System.nanoTime();

        Context context = mApp.getApplicationContext();
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            manager.openCamera("0", mDeviceCB, new Handler(Looper.getMainLooper()));
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }


    public void onPreviewCreated() {
        mCaller.onCameraSessionStarted(this, mSession, (System.nanoTime() - t_sess_start));
    }

    public void captureImage() {
        t_cap_req = SystemClock.elapsedRealtimeNanos();
        try {

            if (mSession == null) {
                Log.e(TAG, "Achtung! Session is null, aborting captureImage()");
                return;
            }

            mSession.stopRepeating();
            mBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            if(CONFIG.CAMERA_FIXED_EXPOSURE){
                mBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                mBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, CONFIG.CAMERA_EXPOSURE_NS);
            }
            mBuilder.addTarget(new Surface(mTexture.getSurfaceTexture()));
            mSession.capture(mBuilder.build(), mSingleCaptureCB, null);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    private CameraDevice.StateCallback mDeviceCB = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mDevice = camera;
            mCaller.onCameraLaunched(mDevice, (System.nanoTime() - t_cam_open));
            createPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            if (mDevice != null) {
                mDevice.close();
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            if (mDevice != null) {
                mDevice.close();
            }
        }
    };

    public void createPreview() {
        try {

            if (mSession != null) {
                mSession.stopRepeating();
            }

            t_sess_start = System.nanoTime();

            SurfaceTexture texture = mTexture.getSurfaceTexture();
            Surface viewSurface = new Surface(texture);

            mBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mBuilder.addTarget(viewSurface);

            mDevice.createCaptureSession(Arrays.asList(viewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            if (null == mDevice) {
                                return;
                            }
                            try {
                                mSession = session;
                                if(CONFIG.CAMERA_FIXED_EXPOSURE) {
                                    mBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                                    mBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, CONFIG.CAMERA_EXPOSURE_NS);
                                }
                                mSession.setRepeatingRequest(mBuilder.build(), mContinuousCaptureCB, null);
                                onPreviewCreated();
                            } catch (CameraAccessException e) {
                                Log.e(TAG, e.getMessage(), e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                        }
                    }, null);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    private CameraCaptureSession.CaptureCallback mContinuousCaptureCB = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            if (!CONFIG.CAMERA_CONTINUOUS) {
                return;
            }
            t_cap_finish_c = SystemClock.elapsedRealtimeNanos();
            t_read = System.nanoTime();
            Bitmap image = mTexture.getBitmap();
            mCaller.processImage(image, (t_cap_finish_c - t_cap_start_c), (System.nanoTime() - t_read), "(continuous)");

        }

        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            t_cap_start_c = timestamp; //from sensor
        }
    };

    private CameraCaptureSession.CaptureCallback mSingleCaptureCB = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {

            t_cap_finish_s = SystemClock.elapsedRealtimeNanos();
            t_read = System.nanoTime();
            Bitmap image = mTexture.getBitmap();
            mCaller.processImage(image, (t_cap_finish_s - t_cap_start_s), (System.nanoTime() - t_read), "(single)");

            createPreview();

        }

        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            t_cap_start_s = timestamp;
            if ((t_cap_start_s - t_cap_req) > 0) {
                mCaller.addTiming(11, (t_cap_start_s - t_cap_req), "");
            } else {
                Log.w(TAG, "Achtung! Singlecapture request timed negative time: " + (t_cap_start_s - t_cap_req));
            }
        }
    };


}

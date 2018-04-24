
package fi.aalto.cs.sipi;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qualcomm.qti.snpe.NeuralNetwork;
import com.qualcomm.qti.snpe.SNPE;

import java.io.File;

public class MainActivity extends Activity {

    private LinearLayout mFrameworkButtons, mAdditionalButtons;
    private TextView mCacheStatus;
    private static final String TAG = MainActivity.class.getSimpleName();

    private CheckBox c_snpe_gpu;
    private CheckBox c_split_timings;
    private CheckBox c_fixed_exposure;

    private int mFragmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_main);

        mFrameworkButtons = findViewById(R.id.framework_buttons);
        mAdditionalButtons = findViewById(R.id.additional_buttons);
        mCacheStatus = findViewById(R.id.cache_status_text);

        c_snpe_gpu = findViewById(R.id.check_snpe_gpu);
        c_split_timings = findViewById(R.id.check_split_timings);
        c_fixed_exposure = findViewById(R.id.check_fixed_exposure);

        if (noCameraPermission() || noStoragePermission()) {
            requestPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isGPUSupported()) {
            c_snpe_gpu.setChecked(false);
            c_snpe_gpu.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        int backCount = getFragmentManager().getBackStackEntryCount();
        if (backCount > 1) {
            getFragmentManager().popBackStack(mFragmentId, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            launchMainFragment();
        } else if (backCount == 1) {
            getFragmentManager().popBackStack();
            recreate();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void launchSNPE(View v) {
        CONFIG.FRAMEWORK = CONFIG.FrameWorks.SNPE;
        setConfig();
        launchMainFragment();
    }

    private void launchTensorFlow() {
        setConfig();
        launchMainFragment();
    }

    public void launchTensorFlowFull(View v) {
        CONFIG.FRAMEWORK = CONFIG.FrameWorks.TF;
        launchTensorFlow();
    }

    public void launchTensorFlowLite(View v) {
        CONFIG.FRAMEWORK = CONFIG.FrameWorks.TF_LITE;
        launchTensorFlow();
    }

    private void setConfig() {
        CONFIG.VERBOSE_OUTPUT = c_split_timings.isChecked();
        CONFIG.CAMERA_FIXED_EXPOSURE = c_fixed_exposure.isChecked();
        if (c_snpe_gpu.isChecked()) {
            CONFIG.SNPE_RUNTIME = NeuralNetwork.Runtime.GPU;
        } else {
            CONFIG.SNPE_RUNTIME = NeuralNetwork.Runtime.CPU;
        }
    }

    private void launchMainFragment() {
        mFrameworkButtons.setVisibility(View.GONE);
        mAdditionalButtons.setVisibility(View.GONE);
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, new MainFragment().create());
        transaction.addToBackStack(null);
        mFragmentId = transaction.commit();
    }

    public void relaunch(View v) {
        Log.d(TAG, "RELAUNCHING");
        Context context = getBaseContext();
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    public boolean isGPUSupported() {
        Application app = (Application) getApplicationContext();
        SNPE.NeuralNetworkBuilder builder = new SNPE.NeuralNetworkBuilder(app);
        return builder.isRuntimeSupported(NeuralNetwork.Runtime.GPU);
    }

    public boolean noCameraPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
    }

    public boolean noStoragePermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission(String permission) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, 1);
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }


    public void clearData(View v) {
        int files_deleted = deleteCacheAndData();
        mCacheStatus.setText(files_deleted + " files deleted");
    }

    private int deleteCacheAndData() {
        int delcount = 0;
        try {
            File efdir = getBaseContext().getExternalFilesDir(null);
            File ecdir = getBaseContext().getExternalCacheDir();
            File fdir = getBaseContext().getFilesDir();
            File cdir = getBaseContext().getCacheDir();
            File[] dirs = new File[]{efdir, ecdir, fdir, cdir};
            for (File dir : dirs) {
                for (File file : dir.listFiles()) {
                    Log.d(TAG, "Deleting file " + file.getAbsolutePath());
                    if (file.delete()) {
                        delcount++;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return delcount;
    }


}

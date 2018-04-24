
package fi.aalto.cs.sipi;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.qualcomm.qti.snpe.SNPE;

import org.tensorflow.TensorFlow;

import java.io.IOException;
import java.io.InputStream;

import fi.aalto.cs.sipi.tasks.LaunchCameraTask;
import fi.aalto.cs.sipi.tasks.PreprocessFloatArraysImageTask;
import fi.aalto.cs.sipi.tasks.PreprocessFloatImageTask;
import fi.aalto.cs.sipi.tasks.ProcessResultsTask;
import fi.aalto.cs.sipi.tasks.RunSNPENetworkTask;
import fi.aalto.cs.sipi.tasks.RunTFLiteNetworkTask;
import fi.aalto.cs.sipi.tasks.RunTFNetworkTask;

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getSimpleName();

    private Activity mActivity;
    private MainActivity mMain;
    private Application mApplication;
    private AssetManager mAssetManager;
    private NeuralNetworkManager mNetManager;

    private GridView mImageGrid;
    private ModelImagesAdapter mImageGridAdapter;
    private TextView mModelNameText;
    private ScrollView mScrollView;
    private LinearLayout mScrollViewW;
    private LinearLayout mInputButtons;
    private TextView mClassificationText;
    private TextView mTimingsText;
    private Button mReloadButton;
    private Button mLoadThumbsButton;
    private Button mLaunchCameraButton;
    private LinearLayout mCameraOptions;
    private CheckBox mCheckContinuousInference;
    private TextureView mCameraTexture;

    private CameraDevice mCamera;
    private CameraCaptureSession mCameraSession;
    private LaunchCameraTask mCameraTask;

    private String[] mSubtaskDescs = new String[]{
            "-",                        //0 (unused)
            "LOAD NETWORK",             //1
            "LOAD IMAGES",              //2
            "-",                        //3 (unused)
            "PREPROCESS IMAGE",         //4
            "-",                        //5 (unused)
            "RUN INFERENCE",            //6
            "PROCESS RESULTS",          //7
            "RELEASE NETWORK",          //8
            "LAUNCH CAMERA",            //9
            "CAPTURE SESSION",          //10
            "CAPTURE REQUEST",          //11
            "CAPTURE SENSOR",           //12
            "READ IMAGE PIXELS",        //13
    };



    public MainFragment create() {
        return new MainFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mActivity = getActivity();

        mModelNameText = view.findViewById(R.id.model_overview_name_text);
        mScrollView = view.findViewById(R.id.timings_scroller);
        mScrollViewW = view.findViewById(R.id.timings_scroller_wrapper);
        mInputButtons = view.findViewById(R.id.image_input_buttons);
        mClassificationText = view.findViewById(R.id.model_overview_classification_text);
        mTimingsText = view.findViewById(R.id.model_overview_timings_text);
        mReloadButton = view.findViewById(R.id.reload_runtime_button);
        mLoadThumbsButton = view.findViewById(R.id.load_thumbs_button);
        mLaunchCameraButton = view.findViewById(R.id.launch_camera_button);
        mCameraOptions = view.findViewById(R.id.camera_options_row);
        mCheckContinuousInference = view.findViewById(R.id.check_continuous_camera);
        mCameraTexture = view.findViewById(R.id.cam_textureview);
        mImageGrid = view.findViewById(R.id.image_grid);

        if (!CONFIG.VERBOSE_OUTPUT) {
            mScrollViewW.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
            mScrollViewW.requestLayout();
        }

        mTimingsText.setOnLongClickListener((View v)->{
            clearTimings();
            return true;
        });

        mReloadButton.setVisibility(View.VISIBLE);
        mReloadButton.setOnClickListener((View v) -> loadNetwork());

        mLoadThumbsButton.setOnClickListener((View v) ->  {
            mInputButtons.setVisibility(View.GONE);
            loadImageThumbnails();
        });
        mLaunchCameraButton.setOnClickListener((View v) ->  {
            mInputButtons.setVisibility(View.GONE);
            launchCamera();
        });

        mImageGridAdapter = new ModelImagesAdapter(mActivity);
        mImageGrid.setAdapter(mImageGridAdapter);
        mImageGrid.setOnItemClickListener((AdapterView<?> parent, View v, int position, long id)->{
            final Bitmap bitmap = mImageGridAdapter.getItem(position);
            processImage(bitmap, 0, 0, "");
        });

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mApplication = (Application) mActivity.getApplicationContext();
        mMain = (MainActivity) getActivity();
        mAssetManager = getResources().getAssets();
        mNetManager = new NeuralNetworkManager(this);

        if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.SNPE) {
            mActivity.setTitle("Snapdragon NPE " + SNPE.getRuntimeVersion(mApplication));
            mModelNameText.setText(CONFIG.MODEL.DLC_FILE_NAME);
        } else if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF_LITE) {
            mActivity.setTitle("TensorFlow Lite");
            mModelNameText.setText(CONFIG.MODEL.LITE_FILE_NAME);
        } else if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF) {
            mActivity.setTitle("TensorFlow");
            mModelNameText.setText(CONFIG.MODEL.PB_FILE_NAME);
        } else {
            mActivity.setTitle("NO FRAMEWORK SELECTED");
            return;
        }

        loadNetwork();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        clearCamera();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mInputButtons.setVisibility(View.VISIBLE);
        loadImageSamples();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDetach() {
        mNetManager.releaseAllNetworks();
        super.onDetach();
    }

    public void setStatusMessage(String msg, boolean keepPrev) {
        if (keepPrev) {
            mClassificationText.append("\n" + msg);
        } else {
            mClassificationText.setText(msg);
        }
    }

    public void addTiming(int subtask_id, long nanosecs, String additional_desc) {
        if (!CONFIG.VERBOSE_OUTPUT) {
            return;
        }

        String description = mSubtaskDescs[subtask_id] + " " + additional_desc;
        long ms = nanosecs / 1000000;
        if (ms < 10) {
            long ns = (nanosecs - ms * 1000000);
            description += ": " + ms + "." + ns + "ms\n";
        } else {
            description += ": " + ms + "ms\n";
        }

        if(subtask_id == 1 || subtask_id == 6){ //display neural net related tasks bold
            mTimingsText.append(Html.fromHtml("<b>"+description+"</b><br>"));
        }
        else{
            mTimingsText.append(description);
        }

        mScrollView.fullScroll(View.FOCUS_DOWN);
    }

    public void clearTimings() {
        mTimingsText.setText(null);
        mClassificationText.setText(null);
    }

    public void clearCamera() {
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mCameraSession != null) {
            mCameraSession.close();
            mCameraSession = null;
        }
        mCameraTask = null;
        mCameraTexture.setVisibility(View.GONE);
        mCameraOptions.setVisibility(View.GONE);
        mCheckContinuousInference.setVisibility(View.GONE);
        CONFIG.CAMERA_CONTINUOUS = false;
    }


    private void loadImageThumbnails() {

        getFragmentManager().beginTransaction().addToBackStack(null).commit();

        if(mMain.noStoragePermission()){
            mMain.requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return;
        }

        long tick = System.nanoTime();

        mImageGridAdapter.clear();
        Cursor thumbCursor = mApplication.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                null, null, null, "image_id DESC");
        if(thumbCursor == null || thumbCursor.getCount() < 1){
            return;
        }
        thumbCursor.moveToFirst();
        int imgCount = 0;
        for (int i = 0; i < thumbCursor.getCount() && i < CONFIG.IMAGE_GRID_LIMIT; i++) {
            if (thumbCursor.getInt(thumbCursor.getColumnIndex(MediaStore.Images.Thumbnails.KIND)) == MediaStore.Images.Thumbnails.MINI_KIND) {
                Uri thumbUri = Uri.parse("file://" + thumbCursor.getString(thumbCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA)));
                try {
                    Bitmap thumb = MediaStore.Images.Media.getBitmap(mApplication.getContentResolver(), thumbUri);
                    addBitmapToGrid(thumb);
                    imgCount++;
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
            thumbCursor.moveToNext();
        }
        thumbCursor.close();
        addTiming(2, System.nanoTime()-tick, "("+imgCount+" files)");
    }

    private void loadImageSamples() {
        mImageGridAdapter.clear();
        try {
            String[] filenames = getResources().getAssets().list(CONFIG.SAMPLE_IMG_FOLDER);
            for (int i = 0; i < filenames.length && i < CONFIG.IMAGE_GRID_LIMIT; i++) {
                InputStream in = getResources().getAssets().open(CONFIG.SAMPLE_IMG_FOLDER + "/" + filenames[i]);
                Bitmap image = BitmapFactory.decodeStream(in);
                addBitmapToGrid(image);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void addBitmapToGrid(Bitmap bitmap) {
        if (mImageGridAdapter.getPosition(bitmap) == -1) {
            mImageGridAdapter.add(bitmap);
            mImageGridAdapter.notifyDataSetChanged();
        }
    }

    private static class ModelImagesAdapter extends ArrayAdapter<Bitmap> {
        public ModelImagesAdapter(Context context) {
            super(context, R.layout.grid_image_layout);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = LayoutInflater.from(parent.getContext()).
                        inflate(R.layout.grid_image_layout, parent, false);
            } else {
                view = convertView;
            }
            final ImageView imageView = ImageView.class.cast(view);
            imageView.setImageBitmap(getItem(position));
            return view;
        }
    }

    private void launchCamera() {

        getFragmentManager().beginTransaction().addToBackStack(null).commit();

        if(mMain.noCameraPermission()){
            mMain.requestPermission(Manifest.permission.CAMERA);
            return;
        }

        mImageGrid.setVisibility(View.GONE);
        mCameraTexture.setVisibility(View.VISIBLE);
        mCameraTask = new LaunchCameraTask(this, mApplication, mCameraTexture);
        mCameraTask.executeOnExecutor(CONFIG.EXECUTOR);
    }

    public void onCameraLaunched(CameraDevice device, long nanos) {
        addTiming(9, nanos, "");
        mCamera = device;
    }

    public void onCameraSessionStarted(LaunchCameraTask caller, CameraCaptureSession session, long nanos) {
        addTiming(10, nanos, "");
        mCameraSession = session;
        mCameraTask = caller;

        mCameraOptions.setVisibility(View.VISIBLE);
        mCameraTexture.setOnClickListener((View v)-> {
            if(!CONFIG.CAMERA_CONTINUOUS){
                mCameraTask.captureImage();
            }
        });

        mCheckContinuousInference.setVisibility(View.VISIBLE);
        mCheckContinuousInference.setOnClickListener((View v) -> {
            CONFIG.CAMERA_CONTINUOUS = mCheckContinuousInference.isChecked();
        });
    }

    private void loadNetwork() {
        setStatusMessage("Loading neural network...", false);
        mNetManager.loadAllNetworks(mApplication, mAssetManager);
    }

    public void onNetLoaded() {
        String readymsg = "Neural net ready";
        setStatusMessage(readymsg, false);
        if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF) {
            mActivity.setTitle("TensorFlow " + TensorFlow.version());
        }
    }

    public void onNetReleased(long release_nanos, int net_id) {
        addTiming(8, release_nanos, "");
        mNetManager.setNetStatus(net_id, NeuralNetworkManager.STATUS_UNLOADED);
    }

    public void processImage(Bitmap image, long cap_ns, long read_ns, String source_cb) {
        if (mNetManager.isUnloading) {
            setStatusMessage("Neural net close", false);
            return;
        }

        int net_id = mNetManager.getAvailableNetId();

        if (net_id > -1) {

            mNetManager.setNetStatus(net_id, NeuralNetworkManager.STATUS_BUSY);

            if (cap_ns > 0) {
                addTiming(12, cap_ns, source_cb);
            }
            if (read_ns > 0) {
                addTiming(13, read_ns, "");
            }

            if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF_LITE) {
                PreprocessFloatArraysImageTask task = new PreprocessFloatArraysImageTask(this, net_id);
                task.executeOnExecutor(CONFIG.EXECUTOR, image);
            }
            else { //SNPE or TF
                PreprocessFloatImageTask task = new PreprocessFloatImageTask(this, net_id);
                task.executeOnExecutor(CONFIG.EXECUTOR, image);
            }
            setStatusMessage(image.getWidth() + "x" + image.getHeight() + " image", true);
        }
    }

    public void onImageProcessed(float[] raw_floats, float[][][] raw_float_arrays, long pre_nanos, int net_id) {
        if (mNetManager.isUnloading) {
            setStatusMessage("Neural net close", false);
            return;
        }

        addTiming(4, pre_nanos, "");
        setStatusMessage("Classifying...", true);

        if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.SNPE) {
            RunSNPENetworkTask task = new RunSNPENetworkTask(this, mNetManager.getNetwork(net_id), net_id);
            task.executeOnExecutor(CONFIG.EXECUTOR, raw_floats);
        } else if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF_LITE) {
            RunTFLiteNetworkTask task = new RunTFLiteNetworkTask(this, mNetManager.getLiteInterpreter(net_id), net_id);
            task.executeOnExecutor(CONFIG.EXECUTOR, raw_float_arrays);
        } else if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF) {
            RunTFNetworkTask task = new RunTFNetworkTask(this, mNetManager.getInferenceInterface(net_id), net_id);
            task.executeOnExecutor(CONFIG.EXECUTOR, raw_floats);

        }
    }

    public void onInferenceFinished(float[] result_floats, long run_nanos, int net_id) {
        addTiming(6, run_nanos, "");
        mNetManager.setNetStatus(net_id, NeuralNetworkManager.STATUS_READY);
        try {
            if (isAdded()) {
                InputStream labelsIn = getResources().getAssets().open(CONFIG.IMAGENET_LABELS_FILE);
                ProcessResultsTask task = new ProcessResultsTask(labelsIn, this);
                task.executeOnExecutor(CONFIG.EXECUTOR, result_floats);
            } else {
                Log.w(TAG, "Fragment not attached, aborting process inference results");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void onResultsProcessed(String[] res_rows, long process_nanos) {
        addTiming(7, process_nanos, "");
        String results = "Top predictions:<br><b>";
        for (String row : res_rows) {
            results += row + "<br>";
        }
        if (CONFIG.VERBOSE_OUTPUT) {
            mTimingsText.append("----------------------------------------\n");
        }
        results += "</b> ";
        mClassificationText.setText(Html.fromHtml(results));
    }









}

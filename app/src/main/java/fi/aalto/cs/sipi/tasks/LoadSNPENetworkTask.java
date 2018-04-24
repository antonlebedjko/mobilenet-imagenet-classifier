package fi.aalto.cs.sipi.tasks;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import com.qualcomm.qti.snpe.NeuralNetwork;
import com.qualcomm.qti.snpe.SNPE;

import java.io.IOException;
import java.io.InputStream;

import fi.aalto.cs.sipi.CONFIG;
import fi.aalto.cs.sipi.NeuralNetworkManager;

public class LoadSNPENetworkTask extends AsyncTask<InputStream, Void, NeuralNetwork> {

    private static final String TAG = LoadSNPENetworkTask.class.getSimpleName();

    private final Application mApplication;
    private final NeuralNetwork.Runtime mTargetRuntime;
    private final NeuralNetwork.PerformanceProfile mPerfProfile;

    private NeuralNetworkManager mCaller;
    private int mNetId;
    private long tick1;

    public LoadSNPENetworkTask(Application application, NeuralNetworkManager caller, int net_id) {
        mApplication = application;
        mCaller = caller;
        mTargetRuntime = CONFIG.SNPE_RUNTIME;
        mPerfProfile = CONFIG.SNPE_PERF;
        mNetId = net_id;
    }

    @Override
    protected NeuralNetwork doInBackground(InputStream... params) {
        tick1 = System.nanoTime();
        NeuralNetwork network = null;
        InputStream dlcIn = params[0];
        try {
            final SNPE.NeuralNetworkBuilder builder = new SNPE.NeuralNetworkBuilder(mApplication)
                    .setRuntimeOrder(mTargetRuntime)
                    .setPerformanceProfile(mPerfProfile).setDebugEnabled(false)
                    .setModel(dlcIn, dlcIn.available());
            network = builder.build();
        } catch (IllegalStateException | IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return network;
    }

    @Override
    protected void onPostExecute(NeuralNetwork neuralNetwork) {
        mCaller.onNetworkLoaded(neuralNetwork, null, null, mNetId, (System.nanoTime() - tick1));
    }

}

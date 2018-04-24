package fi.aalto.cs.sipi.tasks;

import android.content.res.AssetManager;
import android.os.AsyncTask;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import fi.aalto.cs.sipi.CONFIG;
import fi.aalto.cs.sipi.NeuralNetworkManager;


public class LoadTFNetworkTask extends AsyncTask<AssetManager, Void, TensorFlowInferenceInterface> {

    private NeuralNetworkManager mCaller;
    private int mNetId;
    private long tick;

    public LoadTFNetworkTask(NeuralNetworkManager caller, int net_id) {
        mCaller = caller;
        mNetId = net_id;
    }

    @Override
    public TensorFlowInferenceInterface doInBackground(AssetManager... params) {
        tick = System.nanoTime();
        return new TensorFlowInferenceInterface(params[0], CONFIG.MODEL.PB_FILE_NAME);
    }

    @Override
    protected void onPostExecute(TensorFlowInferenceInterface inf) {
        mCaller.onNetworkLoaded(null, inf, null, mNetId, (System.nanoTime() - tick));
    }
}

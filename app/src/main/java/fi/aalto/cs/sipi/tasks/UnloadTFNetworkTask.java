package fi.aalto.cs.sipi.tasks;

import android.os.AsyncTask;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import fi.aalto.cs.sipi.MainFragment;


public class UnloadTFNetworkTask extends AsyncTask<TensorFlowInferenceInterface, Void, Void> {

    private MainFragment mCaller;
    private int mNetId;
    private long tick;

    public UnloadTFNetworkTask(MainFragment caller, int net_id){
        mCaller = caller;
        mNetId = net_id;
    }

    @Override
    protected Void doInBackground(TensorFlowInferenceInterface... params) {
        tick = System.nanoTime();
        params[0].close();
        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        mCaller.onNetReleased((System.nanoTime() - tick), mNetId);
    }

}

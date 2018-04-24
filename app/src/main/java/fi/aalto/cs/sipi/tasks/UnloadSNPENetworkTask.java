package fi.aalto.cs.sipi.tasks;

import android.os.AsyncTask;

import com.qualcomm.qti.snpe.NeuralNetwork;

import fi.aalto.cs.sipi.MainFragment;


public class UnloadSNPENetworkTask extends AsyncTask<NeuralNetwork, Void, Void> {

    private MainFragment mCaller;
    private int mNetId;
    private long tick;

    public UnloadSNPENetworkTask(MainFragment controller, int net_id) {
        mCaller = controller;
        mNetId = net_id;
    }

    @Override
    protected Void doInBackground(NeuralNetwork... params) {
        tick = System.nanoTime();
        params[0].release();
        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        mCaller.onNetReleased((System.nanoTime() - tick), mNetId);
    }


}

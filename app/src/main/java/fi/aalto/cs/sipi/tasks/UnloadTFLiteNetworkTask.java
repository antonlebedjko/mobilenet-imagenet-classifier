package fi.aalto.cs.sipi.tasks;

import android.os.AsyncTask;

import org.tensorflow.lite.Interpreter;

import fi.aalto.cs.sipi.MainFragment;


public class UnloadTFLiteNetworkTask extends AsyncTask<Interpreter, Void, Void> {

    private MainFragment mCaller;
    private int mNetId;
    private long tick;

    public UnloadTFLiteNetworkTask(MainFragment caller, int net_id){
        mCaller = caller;
        mNetId = net_id;
    }

    @Override
    protected Void doInBackground(Interpreter... params) {
        tick = System.nanoTime();
        params[0].close();
        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        mCaller.onNetReleased((System.nanoTime() - tick), mNetId);
    }

}

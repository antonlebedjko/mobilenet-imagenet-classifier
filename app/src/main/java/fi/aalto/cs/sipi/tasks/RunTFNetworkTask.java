package fi.aalto.cs.sipi.tasks;

import android.os.AsyncTask;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import fi.aalto.cs.sipi.CONFIG;
import fi.aalto.cs.sipi.MainFragment;
import fi.aalto.cs.sipi.ModelConfig;


public class RunTFNetworkTask extends AsyncTask<float[], Void, float[]> {

    private static final String TAG = RunTFNetworkTask.class.getSimpleName();

    private final TensorFlowInferenceInterface mInference;

    private MainFragment mCaller;
    private int mNetId;
    private long tick1;

    public RunTFNetworkTask(MainFragment caller, TensorFlowInferenceInterface inf, int net_id) {
        mInference = inf;
        mCaller = caller;
        mNetId = net_id;
    }

    @Override
    public float[] doInBackground(float[]... params) {
        tick1 = System.nanoTime();
        float[] input_floats = params[0];
        float[] output_floats = new float[ModelConfig.OUTPUT_SIZE];
        mInference.feed(CONFIG.MODEL.INPUT_LAYER_PB, input_floats, 1, ModelConfig.INPUT_SIZE, ModelConfig.INPUT_SIZE, 3);
        mInference.run(new String[]{CONFIG.MODEL.OUTPUT_LAYER_PB});
        mInference.fetch(CONFIG.MODEL.OUTPUT_LAYER_PB, output_floats);
        return output_floats;
    }

    @Override
    protected void onPostExecute(float[] out_floats) {
        mCaller.onInferenceFinished(out_floats, (System.nanoTime() - tick1), mNetId);
    }


}

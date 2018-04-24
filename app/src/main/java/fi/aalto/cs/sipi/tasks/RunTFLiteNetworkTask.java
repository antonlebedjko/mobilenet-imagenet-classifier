package fi.aalto.cs.sipi.tasks;

import android.os.AsyncTask;

import org.tensorflow.lite.Interpreter;

import java.util.Arrays;

import fi.aalto.cs.sipi.MainFragment;
import fi.aalto.cs.sipi.ModelConfig;


public class RunTFLiteNetworkTask extends AsyncTask<float[][][], Void, float[]> {

    private static final String TAG = RunTFLiteNetworkTask.class.getSimpleName();

    private final Interpreter mNet;

    private MainFragment mCaller;
    private int mNetId;
    private long tick1;

    public RunTFLiteNetworkTask(MainFragment caller, Interpreter tflite, int net_id){
        mNet = tflite;
        mCaller = caller;
        mNetId = net_id;
    }

    @Override
    public float[] doInBackground(float[][][]... params) {
        tick1 = System.nanoTime();
        float[][][][] input_float_arrays = new float[1][][][];
        input_float_arrays[0] = params[0];
        float[][] output_floats = new float[1][ModelConfig.OUTPUT_SIZE +1];
        mNet.run(input_float_arrays, output_floats);
        return Arrays.copyOfRange(output_floats[0], 1, 1001);
    }

    @Override
    protected void onPostExecute(float[] out_floats) {
        mCaller.onInferenceFinished(out_floats, (System.nanoTime() - tick1), mNetId);
    }



}

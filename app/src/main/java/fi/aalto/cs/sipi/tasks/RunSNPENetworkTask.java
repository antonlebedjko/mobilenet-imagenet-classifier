package fi.aalto.cs.sipi.tasks;

import android.os.AsyncTask;

import com.qualcomm.qti.snpe.FloatTensor;
import com.qualcomm.qti.snpe.NeuralNetwork;

import java.util.HashMap;
import java.util.Map;

import fi.aalto.cs.sipi.CONFIG;
import fi.aalto.cs.sipi.MainFragment;
import fi.aalto.cs.sipi.ModelConfig;

public class RunSNPENetworkTask extends AsyncTask<float[], Void, float[]> {

    private static final String TAG = RunSNPENetworkTask.class.getSimpleName();

    private final NeuralNetwork mNeuralNetwork;

    private MainFragment mCaller;
    private int mNetId;
    private long tick1;

    public RunSNPENetworkTask(MainFragment caller, NeuralNetwork network, int net_id) {
        mCaller = caller;
        mNeuralNetwork = network;
        mNetId = net_id;
    }

    @Override
    protected float[] doInBackground(float[]... params) {
        tick1 = System.nanoTime();
        float[] input_floats = params[0];
        final FloatTensor tensor = mNeuralNetwork.createFloatTensor(ModelConfig.INPUT_SIZE, ModelConfig.INPUT_SIZE, 3);
        tensor.write(input_floats, 0, input_floats.length);
        final Map<String, FloatTensor> inputs = new HashMap<>();
        inputs.put(CONFIG.MODEL.INPUT_LAYER_DLC, tensor);
        FloatTensor out_tensor = mNeuralNetwork.execute(inputs).get(CONFIG.MODEL.OUTPUT_LAYER_DLC);
        final float[] output_floats = new float[out_tensor.getSize()];
        out_tensor.read(output_floats, 0, output_floats.length);
        return output_floats;
    }

    @Override
    protected void onPostExecute(float[] out_floats) {
        mCaller.onInferenceFinished(out_floats, (System.nanoTime() - tick1), mNetId);
    }

}

package fi.aalto.cs.sipi;


import android.app.Application;
import android.content.res.AssetManager;
import android.util.Log;

import com.qualcomm.qti.snpe.NeuralNetwork;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.io.InputStream;

import fi.aalto.cs.sipi.tasks.LoadSNPENetworkTask;
import fi.aalto.cs.sipi.tasks.LoadTFLiteNetworkTask;
import fi.aalto.cs.sipi.tasks.LoadTFNetworkTask;
import fi.aalto.cs.sipi.tasks.UnloadSNPENetworkTask;
import fi.aalto.cs.sipi.tasks.UnloadTFLiteNetworkTask;
import fi.aalto.cs.sipi.tasks.UnloadTFNetworkTask;

public class NeuralNetworkManager {

    private static final String TAG = NeuralNetworkManager.class.getSimpleName();

    public static final int STATUS_UNLOADED = 0;
    public static final int STATUS_READY = 1;
    public static final int STATUS_BUSY = 2;

    private NeuralNetwork[] mNets;
    private TensorFlowInferenceInterface[] mInfs;
    private Interpreter[] mLites;
    private int[] mStatuses;

    private int N;
    private MainFragment mParent;

    public boolean isUnloading = false;

    public NeuralNetworkManager(MainFragment frag) {

        N = CONFIG.N_CONCURRENT_NETS;

        if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.SNPE) {
            mNets = new NeuralNetwork[N];
        } else if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF_LITE) {
            mLites = new Interpreter[N];
        } else if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF) {
            mInfs = new TensorFlowInferenceInterface[N];
        }
        mStatuses = new int[N];
        mParent = frag;
    }

    public void loadAllNetworks(Application app, AssetManager am) {

        if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.SNPE) {
            for (int i = 0; i < mNets.length; i++) {
                try {
                    InputStream dlcIn = am.open(CONFIG.MODEL.DLC_FILE_NAME);
                    LoadSNPENetworkTask task = new LoadSNPENetworkTask(app, this, i);
                    task.executeOnExecutor(CONFIG.EXECUTOR, dlcIn);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        } else if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF_LITE) {
            for (int i = 0; i < mLites.length; i++) {
                LoadTFLiteNetworkTask task = new LoadTFLiteNetworkTask(this, i);
                task.executeOnExecutor(CONFIG.EXECUTOR, am);
            }

        } else if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF) {
            for (int i = 0; i < mInfs.length; i++) {
                LoadTFNetworkTask task = new LoadTFNetworkTask(this, i);
                task.executeOnExecutor(CONFIG.EXECUTOR, am);
            }
        }

    }

    public void onNetworkLoaded(NeuralNetwork net, TensorFlowInferenceInterface inf, Interpreter tflite, int net_id, long nanos) {

        if (net != null) {
            mNets[net_id] = net;
        } else if (inf != null) {
            mInfs[net_id] = inf;
        } else if (tflite != null) {
            mLites[net_id] = tflite;
        } else {
            Log.e(TAG, "Achtung! Network load returned null for all frameworks");
            return;
        }

        mStatuses[net_id] = STATUS_READY;
        mParent.addTiming(1, nanos, "");

        if (allNetsHaveLoaded()) {
            mParent.onNetLoaded();
        }
    }

    public boolean allNetsHaveLoaded() {
        for (int i = 0; i < N; i++) {
            if (mStatuses[i] == STATUS_UNLOADED) {
                return false;
            }
        }
        return true;
    }


    public int getAvailableNetId() {
        for (int i = 0; i < N; i++) {
            if (mStatuses[i] == STATUS_READY) {
                return i;
            }
        }
        return -1;
    }

    public NeuralNetwork getNetwork(int id) {
        return mNets[id];
    }

    public TensorFlowInferenceInterface getInferenceInterface(int id) {
        return mInfs[id];
    }

    public Interpreter getLiteInterpreter(int id) {
        return mLites[id];
    }

    public void setNetStatus(int id, int status) {
        mStatuses[id] = status;
    }

    public void releaseAllNetworks() {
        if (isUnloading) {
            return;
        }

        isUnloading = true;

        for (int i = 0; i < mStatuses.length; i++) {
            if (mStatuses[i] > STATUS_UNLOADED) {

                if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.SNPE) {
                    UnloadSNPENetworkTask task = new UnloadSNPENetworkTask(mParent, i);
                    task.executeOnExecutor(CONFIG.EXECUTOR, mNets[i]);
                } else if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF_LITE) {
                    UnloadTFLiteNetworkTask task = new UnloadTFLiteNetworkTask(mParent, i);
                    task.executeOnExecutor(CONFIG.EXECUTOR, mLites[i]);
                } else if (CONFIG.FRAMEWORK == CONFIG.FrameWorks.TF) {
                    UnloadTFNetworkTask task = new UnloadTFNetworkTask(mParent, i);
                    task.executeOnExecutor(CONFIG.EXECUTOR, mInfs[i]);
                }
            }
        }
    }

}

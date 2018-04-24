package fi.aalto.cs.sipi.tasks;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import fi.aalto.cs.sipi.CONFIG;
import fi.aalto.cs.sipi.NeuralNetworkManager;


public class LoadTFLiteNetworkTask extends AsyncTask<AssetManager, Void, Interpreter> {

    private static final String TAG = LoadTFLiteNetworkTask.class.getSimpleName();

    private NeuralNetworkManager mCaller;
    private int mNetId;
    private long tick;

    public LoadTFLiteNetworkTask(NeuralNetworkManager caller, int net_id) {
        mCaller = caller;
        mNetId = net_id;
    }

    @Override
    public Interpreter doInBackground(AssetManager... params) {
        tick = System.nanoTime();
        return new Interpreter(loadFileByteBuffer(params[0]));
    }

    @Override
    protected void onPostExecute(Interpreter tflite) {
        mCaller.onNetworkLoaded(null, null, tflite, mNetId, (System.nanoTime() - tick));
    }

    private MappedByteBuffer loadFileByteBuffer(AssetManager am) {
        try {
            AssetFileDescriptor fileDescriptor = am.openFd(CONFIG.MODEL.LITE_FILE_NAME);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }
}

package fi.aalto.cs.sipi.tasks;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import fi.aalto.cs.sipi.CONFIG;
import fi.aalto.cs.sipi.MainFragment;

public class ProcessResultsTask extends AsyncTask<float[], Void, String[]> {

    private static final String TAG = ProcessResultsTask.class.getSimpleName();

    private String[] mLabels;
    private MainFragment mCaller;
    private long tick;

    public ProcessResultsTask(InputStream labelsIn, MainFragment caller) {
        mCaller = caller;
        try {
            mLabels = loadLabels(labelsIn);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    protected String[] doInBackground(float[]... params) {
        tick = System.nanoTime();

        float[] result_array = params[0];
        if (CONFIG.FRAMEWORK != CONFIG.FrameWorks.TF_LITE) {
            result_array = softMax(result_array);
        }
        final List<String> result_strings = new ArrayList<>();

        List<ResultElement> indices = new ArrayList<>();
        for (int i = 0; i < result_array.length; i++) {
            indices.add(new ResultElement(i, result_array[i]));
        }
        Collections.sort(indices);

        final int K = 4;
        for (int k = 0; k < K; k++) {
            int idx = indices.get(k).index;
            String conf = String.format("%04.1f%%", (indices.get(k).value*100));
            if(CONFIG.VERBOSE_OUTPUT){
                conf = String.format("%09.6f%%", (indices.get(k).value*100));
            }
            result_strings.add(conf + " <i>" + mLabels[idx]+"</i>");
        }
        return result_strings.toArray(new String[result_strings.size()]);
    }

    @Override
    protected void onPostExecute(String[] results) {
        mCaller.onResultsProcessed(results, (System.nanoTime() - tick));
    }


    private class ResultElement implements Comparable<ResultElement> {
        int index;
        float value;

        ResultElement(int index, float value) {
            this.index = index;
            this.value = value;
        }

        public int compareTo(ResultElement e) {
            if (this.value < e.value) {
                return 1;
            } else if (this.value > e.value) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private String[] loadLabels(InputStream labelsStream) throws IOException {
        final List<String> list = new LinkedList<>();
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(labelsStream));
        String line;
        while ((line = inputStream.readLine()) != null) {
            list.add(line);
        }
        return list.toArray(new String[list.size()]);
    }

    private float[] softMax(float[] arr) {
        double expsum = 0;
        for (int i = 0; i < arr.length; i++) {
            double exp = Math.exp((double) arr[i]);
            expsum += exp;
            arr[i] = (float) exp;
        }
        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i] / (float) expsum;
        }
        return arr;
    }

}

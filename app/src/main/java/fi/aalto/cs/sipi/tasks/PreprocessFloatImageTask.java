package fi.aalto.cs.sipi.tasks;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;

import fi.aalto.cs.sipi.MainFragment;
import fi.aalto.cs.sipi.ModelConfig;


public class PreprocessFloatImageTask extends AsyncTask<Bitmap, Void, float[]> {


    private static final String TAG = PreprocessFloatImageTask.class.getSimpleName();

    private MainFragment mCaller;
    private int mNetId;
    private long tick1;

    public PreprocessFloatImageTask(MainFragment caller, int net_id) {
        mCaller = caller;
        mNetId = net_id;
    }

    @Override
    protected float[] doInBackground(Bitmap... params) {
        tick1 = System.nanoTime();
        Bitmap image = params[0];

        if (image.getWidth() != ModelConfig.INPUT_SIZE || image.getHeight() != ModelConfig.INPUT_SIZE) {
            image = ThumbnailUtils.extractThumbnail(image, ModelConfig.INPUT_SIZE, ModelConfig.INPUT_SIZE);
        }

        final int[] pixels = new int[ModelConfig.INPUT_SIZE * ModelConfig.INPUT_SIZE];
        float[] floats = new float[ModelConfig.INPUT_SIZE * ModelConfig.INPUT_SIZE * 3];

        image.getPixels(pixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

        for (int i = 0; i < pixels.length; i++) {

            final int rgb = pixels[i];
            float b = ((rgb) & 0xFF) - ModelConfig.MEAN_B;
            float g = ((rgb >> 8) & 0xFF) - ModelConfig.MEAN_G;
            float r = ((rgb >> 16) & 0xFF) - ModelConfig.MEAN_R;

            floats[i * 3 + 0] = b * ModelConfig.SCALE;
            floats[i * 3 + 1] = g * ModelConfig.SCALE;
            floats[i * 3 + 2] = r * ModelConfig.SCALE;
        }

        return floats;
    }

    @Override
    protected void onPostExecute(float[] out_floats) {
        mCaller.onImageProcessed(out_floats, null, (System.nanoTime() - tick1), mNetId);
    }


}

package fi.aalto.cs.sipi.tasks;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;

import fi.aalto.cs.sipi.MainFragment;
import fi.aalto.cs.sipi.ModelConfig;


public class PreprocessFloatArraysImageTask extends AsyncTask<Bitmap, Void, float[][][]> {

    private static final String TAG = PreprocessFloatArraysImageTask.class.getSimpleName();

    private MainFragment mCaller;
    private int mNetId;
    private long tick1;

    public PreprocessFloatArraysImageTask(MainFragment caller, int net_id) {
        mCaller = caller;
        mNetId = net_id;
    }

    @Override
    protected float[][][] doInBackground(Bitmap... params) {
        tick1 = System.nanoTime();
        Bitmap image = params[0];
        int SIZE = ModelConfig.INPUT_SIZE;

        if (image.getWidth() != SIZE || image.getHeight() != SIZE) {
            image = ThumbnailUtils.extractThumbnail(image, SIZE, SIZE);
        }
        final int[] pixels = new int[SIZE * SIZE];
        image.getPixels(pixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

        float[][][] floats = new float[SIZE][SIZE][3];

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {

                final int rgb = pixels[SIZE * i + j];
                float b = ((rgb) & 0xFF) - ModelConfig.MEAN_B;
                float g = ((rgb >> 8) & 0xFF) - ModelConfig.MEAN_G;
                float r = ((rgb >> 16) & 0xFF) - ModelConfig.MEAN_R;

                float scale = (float) 1 / 128;
                floats[i][j][0] = b * scale;
                floats[i][j][1] = g * scale;
                floats[i][j][2] = r * scale;
            }
        }

        return floats;
    }

    @Override
    protected void onPostExecute(float[][][] out_floats) {
        mCaller.onImageProcessed(null, out_floats, (System.nanoTime() - tick1), mNetId);
    }


}

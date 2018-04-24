package fi.aalto.cs.sipi;


import android.os.AsyncTask;

import com.qualcomm.qti.snpe.NeuralNetwork;

import java.util.concurrent.Executor;


public class CONFIG {

    public static FrameWorks FRAMEWORK = FrameWorks.SNPE;
    public static NeuralNetwork.Runtime SNPE_RUNTIME = NeuralNetwork.Runtime.GPU;
    public static ModelConfig MODEL = new ModelConfig(ModelNames.MOBILENET);

    public static boolean CAMERA_CONTINUOUS = false;
    public static boolean CAMERA_FIXED_EXPOSURE = false;
    public static boolean VERBOSE_OUTPUT = false;

    public static Executor EXECUTOR = AsyncTask.SERIAL_EXECUTOR;
    public static NeuralNetwork.PerformanceProfile SNPE_PERF = NeuralNetwork.PerformanceProfile.DEFAULT;

    public static final int N_CONCURRENT_NETS = 1;
    public static final long CAMERA_EXPOSURE_NS = 16666666; // 1/60 seconds
    public static final int IMAGE_GRID_LIMIT = 25;
    public static final String IMAGENET_LABELS_FILE = "labels.txt";
    public static final String SAMPLE_IMG_FOLDER = "sample_imgs";

    public enum ModelNames {
        MOBILENET
    }

    public enum FrameWorks {
        SNPE, TF, TF_LITE
    }


}

package fi.aalto.cs.sipi;


public class ModelConfig {

    public static final float SCALE = (float) 0.017;
    public static final float MEAN_B = (float) 103.94;
    public static final float MEAN_G = (float) 116.78;
    public static final float MEAN_R = (float) 123.68;

    public static final int INPUT_SIZE = 224;
    public static final int OUTPUT_SIZE = 1000;

    public String INPUT_LAYER_PB;
    public String INPUT_LAYER_DLC;
    public String OUTPUT_LAYER_DLC;
    public String OUTPUT_LAYER_PB;

    public String PB_FILE_NAME;
    public String DLC_FILE_NAME;
    public String LITE_FILE_NAME;

    public ModelConfig(CONFIG.ModelNames model){

        if(model == CONFIG.ModelNames.MOBILENET){
            INPUT_LAYER_DLC = INPUT_LAYER_PB = "data";

            OUTPUT_LAYER_DLC = "fc7";
            OUTPUT_LAYER_PB = "fc7/BiasAdd";

            PB_FILE_NAME = "models/mobilenet.pb";
            DLC_FILE_NAME = "models/mobilenet.dlc";
            LITE_FILE_NAME = "models/mobilenet_v1.tflite";
        }

    }


}



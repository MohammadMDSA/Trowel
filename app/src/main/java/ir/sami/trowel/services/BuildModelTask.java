package ir.sami.trowel.services;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import ir.sami.trowel.data.ModelBuildConfig;
import ir.sami.trowel.utils.Utils;

public class BuildModelTask extends AsyncTask<String, String, String> {

    private Date operationStartTime;
    private ModelBuildConfig config;
    private Context context;

    public BuildModelTask(ModelBuildConfig config, Context context) {
        this.config = config;
    }

    @Override
    protected String doInBackground(String... strings) {
        File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
        File projectRoot = new File(trowelRoot, config.getProjectName());
        File images = new File(projectRoot, "images");
        File rawImages = new File(projectRoot, "Raw Images");
        File sensorDatabase = new File(Environment.getDataDirectory(), "sensor_width_camera_database.txt");

////////////////////////////////////////
////////////// Normalize ///////////////
////////////////////////////////////////
        {
            if (!images.exists())
                images.mkdirs();
            for (File image : rawImages.listFiles()) {
                File resImage = new File(images, FilenameUtils.getBaseName(image.getName()) + ".jpg");
                Utils.resize(image.getAbsolutePath(), resImage.getAbsolutePath(), config.getMaxImageDimension());
            }
            publishProgress("Normalized images");
        }
////////////////////////////////////////
////////////// Make List ///////////////
////////////////////////////////////////
        String ListResult;
        File matchesDir = new File(projectRoot, "matches");
        {
            if (!matchesDir.exists())
                matchesDir.mkdirs();

            if (!sensorDatabase.exists()) {
                try {
                    sensorDatabase.createNewFile();
                    Utils.copyStreamToFile(context.getAssets().open("sensor_width_camera_database.txt"), sensorDatabase);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            CameraCharacteristics cameraCharacteristics = null;
            try {
                cameraCharacteristics = ((CameraManager) context.getSystemService(Context.CAMERA_SERVICE)).getCameraCharacteristics("0");
            } catch (CameraAccessException e) {
                Toast.makeText(context, "Failed to access camera info", Toast.LENGTH_LONG).show();
                return "";
            }

            ListResult = imageList(images.getAbsolutePath(), sensorDatabase.getAbsolutePath(), matchesDir.getAbsolutePath(), Utils.getFocalLengthInPixels(cameraCharacteristics, config.getMaxImageDimension()) + "");
            if (failed(ListResult))
                return "FAILED";
            publishProgress("Created image list");

        }
////////////////////////////////////////
////////// Compute Features ////////////
////////////////////////////////////////

        File matchesFile = new File(matchesDir, "sfm_data.json");
        String featureResult = computeFeatures(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), config.isComputeFeatureUpRight(), config.getFeatureDescriberMethod(), config.getFeatureDescriberPreset());
        if (failed(featureResult))
            return "FAILED";
        publishProgress("Computed features");

////////////////////////////////////////
/////////// Compute Matches ////////////
////////////////////////////////////////

        String matchResult = computeMatches(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), config.getMatchGeometricModel(), config.getMatchRatio(), config.getMatchMethod());
        if (failed(matchResult))
            return "FAILED";
        publishProgress("Computed matches");

////////////////////////////////////////
///////////// Reconstruct //////////////
////////////////////////////////////////

        String reconstructResult = incrementalReconstruct(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), matchesDir.getAbsolutePath(), config.getReconstructionRefinement(), "STELLAR", "PINHOLE_CAMERA_RADIAL3");
        if (failed(reconstructResult))
            return "FAILED";
        publishProgress("Reconstructed 3d cloud");

////////////////////////////////////////
////////////// Fix Poses ///////////////
////////////////////////////////////////

        matchesFile = new File(matchesDir, "sfm_data.bin");
        File matchData = new File(matchesDir, "matches.f.bin");
        File robustFile = new File(matchesDir, "robust.bin");
        String fixPostResult = refinePoses(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), robustFile.getAbsolutePath(), config.isFixPoseBundleAdjustment(), "1", matchData.getAbsolutePath());
        if (failed(fixPostResult))
            return "FAILED";
        publishProgress("Fixed poses");

////////////////////////////////////////
/////////////// Colorize ///////////////
////////////////////////////////////////

        File colorized = new File(matchesDir, "robust_colorized.ply");
        String colorizeResult = computeDataColor(robustFile.getAbsolutePath(), colorized.getAbsolutePath());
        if (failed(colorizeResult))
            return "FAILED";
        publishProgress("Colorized result");

        return "SUCCESS";
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    private boolean failed(String res) {
        return "FAILED".equals(res);
    }

    //
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            operationStartTime = new Date();
//            binding.progressBar.setVisibility(View.VISIBLE);
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
//            super.onPostExecute(s);
//            long timeElapsed = new Date().getTime() - operationStartTime.getTime();
//            double seconds = timeElapsed / 1000d;
//            binding.sampleText.setText(seconds + " seconds");
//            binding.result.setText(s);
//            binding.progressBar.setVisibility(View.INVISIBLE);
//        }


    public native String imageList(String sImageDir, String sfileDatabase, String sOutputDir, String jfocal_pixels);

    public native String computeFeatures(String jsSfM_Data_Filename, String jsOutDir, String jbUpRight, String jsImage_Describer_Method, String jsFeaturePreset);

    public native String computeMatches(String jsSfM_Data_Filename, String jsMatchesDirectory, String jsGeometricModel, String jfDistRatio, String jsNearestMatchingMethod);

    public native String incrementalReconstruct(String jsSfM_Data_Filename, String jsMatchesDir, String jsOutDir,
                                                String jsIntrinsic_refinement_options, String jsSfMInitializer_method, String jcameraModel);

    public native String computeDataColor(String jsSfM_Data_Filename_In, String jsOutputPLY_Out);

    public native String refinePoses(String jsSfM_Data_Filename, String jsMatchesDir, String jsOutFile, String juseBundleAdjustment, String jbDirect_triangulation, String jsMatchFile);


}
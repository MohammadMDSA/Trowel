package ir.sami.trowel.services;

import static ir.sami.trowel.Constants.BUILD_NOTIFICATION_CHANNEL_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import ir.sami.trowel.Constants;
import ir.sami.trowel.R;
import ir.sami.trowel.data.ModelBuildConfig;
import ir.sami.trowel.project_detail.ProjectDetailActivity;
import ir.sami.trowel.utils.Utils;

public class BuildModelTask extends AsyncTask<String, Integer, String> {

    private Date operationStartTime;
    private final ModelBuildConfig config;
    private final Service context;

    public BuildModelTask(ModelBuildConfig config, Service context) {
        this.config = config;
        this.context = context;
    }

    @Override
    protected String doInBackground(String... strings) {
        File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
        File projectRoot = new File(trowelRoot, config.getProjectName());
        File images = new File(projectRoot, "images");
        File rawImages = new File(projectRoot, "Raw Images");
        if(!rawImages.exists())
            return "SUCCESS";
        File sensorDatabase = new File(context.getDataDir(), "sensor_width_camera_database.txt");
        publishProgress(0);

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
            publishProgress(1);
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
            publishProgress(2);

        }
////////////////////////////////////////
////////// Compute Features ////////////
////////////////////////////////////////

        File matchesFile = new File(matchesDir, "sfm_data.json");
        String featureResult = computeFeatures(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), config.isComputeFeatureUpRight(), config.getFeatureDescriberMethod(), config.getFeatureDescriberPreset());
        if (failed(featureResult))
            return "FAILED";
        publishProgress(3);

////////////////////////////////////////
/////////// Compute Matches ////////////
////////////////////////////////////////

        String matchResult = computeMatches(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), config.getMatchGeometricModel(), config.getMatchRatio(), config.getMatchMethod());
        if (failed(matchResult))
            return "FAILED";
        publishProgress(4);

////////////////////////////////////////
///////////// Reconstruct //////////////
////////////////////////////////////////

        String reconstructResult = incrementalReconstruct(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), matchesDir.getAbsolutePath(), config.getReconstructionRefinement(), "STELLAR", "PINHOLE_CAMERA_RADIAL3");
        if (failed(reconstructResult))
            return "FAILED";
        publishProgress(5);

////////////////////////////////////////
////////////// Fix Poses ///////////////
////////////////////////////////////////

        matchesFile = new File(matchesDir, "sfm_data.bin");
        File matchData = new File(matchesDir, "matches.f.bin");
        File robustFile = new File(matchesDir, "robust.bin");
        String fixPostResult = refinePoses(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), robustFile.getAbsolutePath(), config.isFixPoseBundleAdjustment(), "1", matchData.getAbsolutePath());
        if (failed(fixPostResult))
            return "FAILED";
        publishProgress(6);

////////////////////////////////////////
/////////////// Colorize ///////////////
////////////////////////////////////////

        File colorized = new File(matchesDir, "robust_colorized.ply");
        String colorizeResult = computeDataColor(robustFile.getAbsolutePath(), colorized.getAbsolutePath());
        if (failed(colorizeResult))
            return "FAILED";
        publishProgress(7);

        return "SUCCESS";
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        String stage;
        switch (values[0]) {
            default:
            case 0:
                stage = "Reducing Image Size";
                break;
            case 1:
                stage = "Creating Image List";
                break;
            case 2:
                stage = "Computing Image Features";
                break;
            case 3:
                stage = "Computing Matches";
                break;
            case 4:
                stage = "Reconstructing 3D Cloud";
                break;
            case 5:
                stage = "Adjusting Cloud";
                break;
            case 6:
                stage = "Colorizing Cloud";
                break;
            case 7:
                stage = "Done!";
                break;
        }
        Intent notificationIntent = new Intent(context, ProjectDetailActivity.class);
        notificationIntent.putExtra(Constants.PROJECT_NAME_REFERENCE, config.getProjectName());
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(context, Constants.BUILD_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(stage)
                .setContentText(config.getProjectName())
                .setProgress(7, values[0], false)
                .setSmallIcon(R.drawable.trowel_svgrepo_com)
                .setContentIntent(pendingIntent)
                .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(Constants.BUILD_NOTIFICATION_ID, notification);

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


    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        context.stopSelf();
    }

    public native String imageList(String sImageDir, String sfileDatabase, String sOutputDir, String jfocal_pixels);

    public native String computeFeatures(String jsSfM_Data_Filename, String jsOutDir, String jbUpRight, String jsImage_Describer_Method, String jsFeaturePreset);

    public native String computeMatches(String jsSfM_Data_Filename, String jsMatchesDirectory, String jsGeometricModel, String jfDistRatio, String jsNearestMatchingMethod);

    public native String incrementalReconstruct(String jsSfM_Data_Filename, String jsMatchesDir, String jsOutDir,
                                                String jsIntrinsic_refinement_options, String jsSfMInitializer_method, String jcameraModel);

    public native String computeDataColor(String jsSfM_Data_Filename_In, String jsOutputPLY_Out);

    public native String refinePoses(String jsSfM_Data_Filename, String jsMatchesDir, String jsOutFile, String juseBundleAdjustment, String jbDirect_triangulation, String jsMatchFile);


}
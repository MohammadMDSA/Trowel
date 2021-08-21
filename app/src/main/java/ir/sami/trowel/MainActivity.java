package ir.sami.trowel;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import ir.sami.trowel.databinding.ActivityMainBinding;
import ir.sami.trowel.utils.Utils;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 12;

    private View layout;
//    private NormalizeImagesTask normalizeImagesTask;
//    private MakeImageListTask makeImageListTask;
//    private ComputeFeaturesTask computeFeaturesTask;
//    private ComputeMatchesTask computeMatchesTask;
//    private IncrementalReconstructTask incrementalReconstructTask;
//    private ComputeDataColorTask computeDataColorTask;
//    private RefinePosesTask refinePosesTask;
//    private ComputeRobustColorTask computeRobustColorTask;
    private Date operationStartTime;
    private Context context;
    private Pair<Integer, Integer> imagesSize;

    static {
        try {
            System.loadLibrary("openMVG");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.mainLayout);
        this.layout = binding.mainLayout;
        // Example of a call to a native method
    }

    public void startProcess(View view) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.CAMERA
                    )) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //to simplify, call requestPermissions again
                Toast.makeText(getApplicationContext(),
                        "shouldShowRequestPermissionRationale",
                        Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        } else {
            normalizeImages();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted.
                Toast.makeText(getApplicationContext(),
                        "External activated",
                        Toast.LENGTH_LONG).show();
            } else {
                // permission denied.
                Toast.makeText(getApplicationContext(),
                        "permission denied! Oh:(",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void normalizeImages() {
//        normalizeImagesTask = new NormalizeImagesTask();
//        normalizeImagesTask.execute("");
    }

    public void match(View view) {
//        makeImageListTask = new MakeImageListTask();
//        makeImageListTask.execute("");

    }

    public void computeFeatures(View view) {
//        computeFeaturesTask = new ComputeFeaturesTask();
//        computeFeaturesTask.execute("");
    }

    public void computeMatches(View view) {
//        computeMatchesTask = new ComputeMatchesTask();
//        computeMatchesTask.execute("");
    }

    public void incrementalReconstruct(View viwe) {
//        incrementalReconstructTask = new IncrementalReconstructTask();
//        incrementalReconstructTask.execute("");
    }

    public void computeColor(View view) {
//        computeDataColorTask = new ComputeDataColorTask();
//        computeDataColorTask.execute("");
    }

    public void refine(View view) {
//        refinePosesTask = new RefinePosesTask();
//        refinePosesTask.execute("");
    }

    public void colorizeRefined(View view) {
//        computeRobustColorTask = new ComputeRobustColorTask();
//        computeRobustColorTask.execute("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        // Cancel running task(s) to avoid memory leaks
//        if (normalizeImagesTask != null)
//            normalizeImagesTask.cancel(true);
//        if (makeImageListTask != null)
//            makeImageListTask.cancel(true);
//        if (computeFeaturesTask != null)
//            computeFeaturesTask.cancel(true);
//        if (computeMatchesTask != null)
//            computeMatchesTask.cancel(true);
//        if (incrementalReconstructTask != null)
//            incrementalReconstructTask.cancel(true);
//        if (computeDataColorTask != null)
//            computeDataColorTask.cancel(true);
//        if (refinePosesTask != null)
//            refinePosesTask.cancel(true);
//        if (computeRobustColorTask != null)
//            computeRobustColorTask.cancel(true);
    }
//
//    class NormalizeImagesTask extends AsyncTask<String, String, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
//            File images = new File(trowelRoot, "images");
//            File rawImages = new File(trowelRoot, "Raw Images");
//            if (!images.exists())
//                images.mkdirs();
//            for (File image : rawImages.listFiles()) {
//                File resImage = new File(images, FilenameUtils.getBaseName(image.getName()) + ".jpg");
//                imagesSize = Utils.resize(image.getAbsolutePath(), resImage.getAbsolutePath(), 1000);
//            }
//            return "DONE!";
//        }
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
//    }
//
//    class MakeImageListTask extends AsyncTask<String, String, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
//            File images = new File(trowelRoot, "images");
//            File sensorDatabase = new File(getDataDir(), "sensor_width_camera_database.txt");
//
//            File matchesDir = new File(trowelRoot, "matches");
//            if (!matchesDir.exists())
//                matchesDir.mkdirs();
//
//            if (!sensorDatabase.exists()) {
//                try {
//                    sensorDatabase.createNewFile();
//                    Utils.copyStreamToFile(getAssets().open("sensor_width_camera_database.txt"), sensorDatabase);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            CameraCharacteristics cameraCharacteristics = null;
//            try {
//                cameraCharacteristics = ((CameraManager) layout.getContext().getSystemService(Context.CAMERA_SERVICE)).getCameraCharacteristics("0");
//            } catch (CameraAccessException e) {
//                Toast.makeText(layout.getContext(), "Failed to access camera info", Toast.LENGTH_LONG).show();
//                return "";
//            }
//
//            return imageList(images.getAbsolutePath(), sensorDatabase.getAbsolutePath(), matchesDir.getAbsolutePath(), Utils.getFocalLengthInPixels(cameraCharacteristics, 1000) + "");
//        }
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
//    }
//
//    class ComputeFeaturesTask extends AsyncTask<String, String, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
//            File matchesDir = new File(trowelRoot, "matches");
//            File matchesFile = new File(matchesDir, "sfm_data.json");
//
//            return computeFeatures(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), "0", "SIFT", "ULTRA");
//        }
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
//    }
//
//    class ComputeMatchesTask extends AsyncTask<String, String, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
//            File matchesDir = new File(trowelRoot, "matches");
//            File matchesFile = new File(matchesDir, "sfm_data.json");
//            return computeMatches(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), "e", "0.8f", "FASTCASCADEHASHINGL2");
//        }
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
//    }
//
//    class IncrementalReconstructTask extends AsyncTask<String, String, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
//            File matchesDir = new File(trowelRoot, "matches");
//            File matchesFile = new File(matchesDir, "sfm_data.json");
//            return incrementalReconstruct(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), matchesDir.getAbsolutePath(), "NONE", "STELLAR", "PINHOLE_CAMERA_RADIAL3");
//        }
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
//    }
//
//    class ComputeDataColorTask extends AsyncTask<String, String, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
//            File matchesDir = new File(trowelRoot, "matches");
//            File colorized = new File(matchesDir, "colorized.ply");
//            File matchesFile = new File(matchesDir, "sfm_data.bin");
//            return computeDataColor(matchesFile.getAbsolutePath(), colorized.getAbsolutePath());
//        }
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
//    }
//
//    class RefinePosesTask extends AsyncTask<String, String, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
//            File matchesDir = new File(trowelRoot, "matches");
//            File output = new File(matchesDir, "robust.bin");
//            File matchesFile = new File(matchesDir, "sfm_data.bin");
//            File matchData = new File(matchesDir, "matches.f.bin");
//            return refinePoses(matchesFile.getAbsolutePath(), matchesDir.getAbsolutePath(), output.getAbsolutePath(), "1", "1", matchData.getAbsolutePath());
//        }
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
//    }
//
//    class ComputeRobustColorTask extends AsyncTask<String, String, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
//            File matchesDir = new File(trowelRoot, "matches");
//            File colorized = new File(matchesDir, "robust_colorized.ply");
//            File robustFile = new File(matchesDir, "robust.bin");
//            return computeDataColor(robustFile.getAbsolutePath(), colorized.getAbsolutePath());
//        }
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
//    }
}
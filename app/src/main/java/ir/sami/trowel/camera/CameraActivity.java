package ir.sami.trowel.camera;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ir.sami.trowel.Constants;
import ir.sami.trowel.R;
import ir.sami.trowel.utils.CameraPreview;

public class CameraActivity extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private int cameraOrientation = 90;
    private String lastFileName;
    private String projectName;
    private int fileIndex;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        setTitle("New photo");

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        this.projectName = this.getIntent().getStringExtra(Constants.PROJECT_NAME_REFERENCE);
        fileIndex = 0;
        lastFileName = this.getIntent().getStringExtra(Constants.PROJECT_DETAIL_LIST_LAST_FILE_NAME);
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.camera_preview);
        frameLayout.addView(mPreview);
        mCamera.setDisplayOrientation(cameraOrientation);
        File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
        File projectRoot = new File(trowelRoot, projectName);
        File rawImagesRoot = new File(projectRoot, "Raw Images");
        if (!rawImagesRoot.exists()) {
            rawImagesRoot.mkdirs();
        }
    }

    public void takePicture(View view) {


        try {

            mCamera.takePicture(null, null, (bytes, camera) -> {
                new CameraSaveImageTask().execute(bytes);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File getNewFile() {
        File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
        File projectRoot = new File(trowelRoot, projectName);
        File rawImagesRoot = new File(projectRoot, "Raw Images");
        String lastBaseName = FilenameUtils.getBaseName(lastFileName);
        String fileName = lastBaseName + fileIndex + ".jpg";
        File newFile = new File(rawImagesRoot, fileName);
        while (newFile.exists()) {
            fileIndex++;
            fileName = lastBaseName + fileIndex + ".jpg";
            newFile = new File(rawImagesRoot, fileName);
        }
        fileIndex++;

        return newFile;
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();

    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }


    class CameraSaveImageTask extends AsyncTask<byte[], String, Boolean> {

        @Override
        protected Boolean doInBackground(byte[]... bytes) {
            try {
                File newFile = getNewFile();
                try {
                    if (!newFile.createNewFile()) {
                        throw new IOException();
                    }
                } catch (IOException e) {
                    return false;
                }
                File finalNewFile = newFile;
                mCamera.startPreview();
                FileOutputStream stream = new FileOutputStream(finalNewFile);
                stream.write(bytes[0]);
                stream.close();
            } catch (Exception e) {
                return false;
            }
            return true;
        }

    }
}
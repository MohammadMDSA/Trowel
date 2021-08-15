package ir.sami.trowel;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;

import ir.sami.trowel.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 12;

    private View layout;

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
        setContentView(binding.getRoot());

        this.layout = binding.getRoot();
        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText("FOO");
    }

    public void startMatching(View view) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //to simplify, call requestPermissions again
                Toast.makeText(getApplicationContext(),
                        "shouldShowRequestPermissionRationale",
                        Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        } else {
            // permission granted
//            writeImage();
            match();

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
                        "permission was granted, thx:)",
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

    private void match() {
        Snackbar.make(layout, "HELL YEAH!", Snackbar.LENGTH_SHORT).show();
        File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
        File images = new File(trowelRoot, "images");
        File sensorDatabase = new File(getDataDir(), "sensor_width_camera_database.txt");
        File log = new File(trowelRoot, "out.log");
        try {
            log.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!sensorDatabase.exists()) {
            try {
                sensorDatabase.createNewFile();
                Utils.copyStreamToFile(getAssets().open("sensor_width_camera_database.txt"), sensorDatabase);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File matchesDir = new File(trowelRoot, "matches");

        if (!matchesDir.exists())
            matchesDir.mkdirs();


        TextView tv = binding.sampleText;

        tv.setText(imageList(images.getAbsolutePath(), sensorDatabase.getAbsolutePath(), matchesDir.getAbsolutePath()));
    }

    public native String imageList(String sImageDir, String sfileDatabase, String sOutputDir);
}
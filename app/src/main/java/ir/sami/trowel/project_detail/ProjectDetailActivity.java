package ir.sami.trowel.project_detail;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ir.sami.trowel.Constants;
import ir.sami.trowel.R;
import ir.sami.trowel.camera.CameraActivity;
import ir.sami.trowel.data.ModelBuildConfig;
import ir.sami.trowel.databinding.ActivityProjectDetailBinding;
import ir.sami.trowel.dialogs.ProjectNameDialogFragment;
import ir.sami.trowel.project_detail.ui.main.SectionsPagerAdapter;
import ir.sami.trowel.services.BuildModelService;
import ir.sami.trowel.services.BuildModelTask;
import ir.sami.trowel.utils.Utils;

public class ProjectDetailActivity extends AppCompatActivity {

    private ActivityProjectDetailBinding binding;
    private String projectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityProjectDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        this.projectName = this.getIntent().getExtras().getString(Constants.PROJECT_NAME_REFERENCE);
        this.setTitle(projectName);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager(), projectName);
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manu_project_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.build_project:
                Intent service = new Intent(this, BuildModelService.class);
                service.putExtra(Constants.PROJECT_NAME_REFERENCE, this.projectName);
                ContextCompat.startForegroundService(this, service);
                break;
            case R.id.stop_build:
                Intent stopBuild = new Intent(this, BuildModelService.class);
                stopService(stopBuild);
                Toast.makeText(this, "Building process stopped", Toast.LENGTH_SHORT).show();
                break;
            case R.id.remove_build_data:
                removeBuildData();
                break;
            case R.id.remove_images:
                removeBuildData();
                File projectRoot = Utils.getProjectRoot(this.projectName);
                File imagesRoot = new File(projectRoot, "images");
                File rawImagesRoot = new File(projectRoot, "Raw Images");
                if (imagesRoot.exists() || rawImagesRoot.exists()) {
                    Utils.deleteDirectory(imagesRoot);
                    Utils.deleteDirectory(rawImagesRoot);
                } else {
                    Toast.makeText(this, "There are no images in the project", Toast.LENGTH_SHORT).show();
                }
        }
        return true;
    }

    public void addImage(View view) {
        Intent navigationIntent = new Intent(this, CameraActivity.class);
        File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
        File projectRoot = new File(trowelRoot, projectName);
        File rawImagesRoot = new File(projectRoot, "Raw Images");
        String[] files = rawImagesRoot.list();
        navigationIntent.putExtra(Constants.PROJECT_NAME_REFERENCE, this.projectName);
        if (files == null || files.length == 0)
            navigationIntent.putExtra(Constants.PROJECT_DETAIL_LIST_LAST_FILE_NAME, "");
        else {
            List<String> filess = Arrays.stream(files).sorted().collect(Collectors.toList());
            navigationIntent.putExtra(Constants.PROJECT_DETAIL_LIST_LAST_FILE_NAME, filess.get(filess.size() - 1));

        }
        startActivity(navigationIntent);
    }

    private void removeBuildData() {
        Intent stopBuild = new Intent(this, BuildModelService.class);
        stopService(stopBuild);
        File projectRoot = Utils.getProjectRoot(this.projectName);
        File matchesDir = new File(projectRoot, "matches");
        if (matchesDir.exists()) {
            Utils.deleteDirectory(matchesDir);
            Toast.makeText(this, "All project build data successfully deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "All project build data already deleted", Toast.LENGTH_SHORT).show();
        }
    }

}
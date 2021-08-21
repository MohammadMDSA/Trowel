package ir.sami.trowel.project_detail;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.view.View;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ir.sami.trowel.Constants;
import ir.sami.trowel.camera.CameraActivity;
import ir.sami.trowel.data.ModelBuildConfig;
import ir.sami.trowel.databinding.ActivityProjectDetailBinding;
import ir.sami.trowel.project_detail.ui.main.SectionsPagerAdapter;

public class ProjectDetailActivity extends AppCompatActivity {

    private ActivityProjectDetailBinding binding;
    private String projectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityProjectDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        this.projectName = this.getIntent().getExtras().getString(Constants.PROJECT_NAME_REFERENCE);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager(), projectName);
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);

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

    public void buildCommand(View view) {
        ModelBuildConfig config = new ModelBuildConfig();
        config.setProjectName(this.projectName);
        config.setFeatureDescriberMethod("SIFT");
        config.setFeatureDescriberPreset("ULTRA");
        config.setComputeFeatureUpRight(false);
        config.setMatchGeometricModel(ModelBuildConfig.MatchGeometricModel.EssentialMatrixFiltering);
        config.setMaxImageDimension(1000);
        config.setMatchRatio("0.8f");
        config.setMatchMethod("FASTCASCADEHASHINGL2");
        ModelBuildConfig.ReconstructionRefinement reconstructionRefinement = new ModelBuildConfig.ReconstructionRefinement();
        reconstructionRefinement.setDistortion(true);
        reconstructionRefinement.setPrincipalPoint(true);
        reconstructionRefinement.setFocalLength(true);
        config.setReconstructionRefinement(reconstructionRefinement);
        config.setFixPoseBundleAdjustment(true);
    }
}
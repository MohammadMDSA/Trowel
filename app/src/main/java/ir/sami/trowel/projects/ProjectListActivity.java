package ir.sami.trowel.projects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;

import ir.sami.trowel.Constants;
import ir.sami.trowel.R;
import ir.sami.trowel.databinding.ActivityProjectListBinding;
import ir.sami.trowel.dialogs.ProjectNameDialogFragment;
import ir.sami.trowel.dialogs.SubmissionDialogFragment;
import ir.sami.trowel.project_detail.ProjectDetailActivity;

public class ProjectListActivity extends AppCompatActivity implements ProjectNameDialogFragment.NoticeDialogListener, SubmissionDialogFragment.NoticeDialogListener {

    private ActivityProjectListBinding binding;
    private ProjectListAdaptor projectListAdaptor;
    private RecyclerView projectList;

    private static final int MY_PERMISSIONS_REQUEST_ID = 12;

    static {
        try {
            System.loadLibrary("openMVG");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manu_project_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.create_project:
                ProjectNameDialogFragment projectNameDialogFragment = new ProjectNameDialogFragment();
                projectNameDialogFragment.show(getSupportFragmentManager(), "ProjectName");
                break;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProjectListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setContentView(R.layout.activity_project_list);

        projectList = (RecyclerView) findViewById(R.id.project_list);

        projectListAdaptor = new ProjectListAdaptor();
        projectList.setLayoutManager(new LinearLayoutManager(this));
        projectList.setAdapter(projectListAdaptor);
        projectListAdaptor.setClickHandler(new ProjectListAdaptor.OnClickListener() {
            @Override
            public void click(String uri) {
                Intent navigationIntent = new Intent(ProjectListActivity.this, ProjectDetailActivity.class);
                navigationIntent.putExtra(Constants.PROJECT_NAME_REFERENCE, uri);
                startActivity(navigationIntent);
            }

            @Override
            public void delete(String uri) {
                SubmissionDialogFragment submissionDialogFragment = new SubmissionDialogFragment(uri);
                submissionDialogFragment.show(getSupportFragmentManager(), "DeleteSubmission" + uri);
            }
        });
        updateProjectListWrapper();
    }

    private void updateProjectListWrapper() {
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
                        MY_PERMISSIONS_REQUEST_ID);
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_ID);
            }
        } else {
            updateProjectList();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ID) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted.
                Toast.makeText(getApplicationContext(),
                        "External activated",
                        Toast.LENGTH_LONG).show();
                updateProjectList();
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

    private void updateProjectList() {
        File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
        String[] projects = trowelRoot.list();
        if (projects == null)
            return;
        projectListAdaptor.setProjects(Arrays.asList(projects.clone()));
        projectListAdaptor.notifyDataSetChanged();
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        updateProjectListWrapper();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }
}
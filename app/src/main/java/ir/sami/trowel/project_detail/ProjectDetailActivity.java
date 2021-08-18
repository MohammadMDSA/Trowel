package ir.sami.trowel.project_detail;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ir.sami.trowel.Constants;
import ir.sami.trowel.R;
import ir.sami.trowel.projects.ProjectListAdaptor;

public class ProjectDetailActivity extends AppCompatActivity {

    ProjectDetailListAdaptor projectDetailListAdaptor;
    private RecyclerView projectList;
    private String projectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        projectList = (RecyclerView) findViewById(R.id.thumbnail_list);

        projectDetailListAdaptor = new ProjectDetailListAdaptor();
        projectList.setLayoutManager(new GridLayoutManager(this, 4, RecyclerView.VERTICAL, false));
        projectList.setAdapter(projectDetailListAdaptor);
        projectDetailListAdaptor.setClickHandler((ProjectDetailListAdaptor.OnClickListener) uri -> {

        });
        this.projectName = this.getIntent().getExtras().getString(Constants.PROJECT_NAME_REFERENCE);
        this.setTitle(projectName);
        File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
        File projectRoot = new File(trowelRoot, projectName);
        File rawImagesRoot = new File(projectRoot, "Raw Images");
        if (!rawImagesRoot.exists())
            return;
        List<String> thumbnails = Arrays.stream(rawImagesRoot.listFiles()).map(f -> f.getAbsolutePath()).collect(Collectors.toList());
        projectDetailListAdaptor.setThumbnails(thumbnails);
        projectDetailListAdaptor.notifyDataSetChanged();
    }

    public void addImage(View view) {

    }

}
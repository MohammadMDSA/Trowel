package ir.sami.trowel.projects;

import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import ir.sami.trowel.R;

public class ProjectItemViewHolder extends RecyclerView.ViewHolder {

    private final TextView projectName;

    public ProjectItemViewHolder(@NonNull View itemView) {
        super(itemView);
        this.projectName = (TextView) itemView.findViewById(R.id.inp_project_name);
    }

    public void bind(String uri) {
        projectName.setText(uri);
    }
}

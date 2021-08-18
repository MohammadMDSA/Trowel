package ir.sami.trowel.projects;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ir.sami.trowel.R;

public class ProjectItemViewHolder extends RecyclerView.ViewHolder {

    private TextView projectName;

    public ProjectItemViewHolder(@NonNull View itemView) {
        super(itemView);
        this.projectName = (TextView) itemView.findViewById(R.id.project_name);
    }

    public void bind(String uri) {
        projectName.setText(uri);
    }
}

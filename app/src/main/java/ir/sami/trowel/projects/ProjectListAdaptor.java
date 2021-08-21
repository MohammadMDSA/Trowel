package ir.sami.trowel.projects;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ir.sami.trowel.R;

public class ProjectListAdaptor extends RecyclerView.Adapter<ProjectItemViewHolder> {

    private List<String> projects = new ArrayList<>();
    private OnClickListener clickHandler;

    @NonNull
    @Override
    public ProjectItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.project_list_item, parent, false);
        return new ProjectItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectItemViewHolder holder, int position) {
        holder.bind(projects.get(position));
        holder.itemView.setOnClickListener(view -> clickHandler.click(projects.get(position)));
        holder.itemView.findViewById(R.id.btn_project_delete).setOnClickListener(view -> clickHandler.delete(projects.get(position)));
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public void setProjects(List<String> projects) {
        this.projects = projects;
    }

    public void setClickHandler(OnClickListener clickHandler) {
        this.clickHandler = clickHandler;
    }

    interface OnClickListener {
        void click(String uri);
        void delete(String uri);
    }
}

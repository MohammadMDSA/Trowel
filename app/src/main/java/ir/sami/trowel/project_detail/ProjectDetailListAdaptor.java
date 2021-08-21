package ir.sami.trowel.project_detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ir.sami.trowel.R;
import ir.sami.trowel.projects.ProjectItemViewHolder;
import ir.sami.trowel.projects.ProjectListAdaptor;

public class ProjectDetailListAdaptor extends RecyclerView.Adapter<ProjectDetailListItemViewHolder> {

    private List<String> thumbnails = new ArrayList<>();
    private OnClickListener clickHandler;

    @NonNull
    @Override
    public ProjectDetailListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.project_detail_list_item, parent, false);
        return new ProjectDetailListItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectDetailListItemViewHolder holder, int position) {
        holder.bind(thumbnails.get(position));
        holder.itemView.setOnClickListener(view -> clickHandler.click(thumbnails.get(position)));
    }

    @Override
    public int getItemCount() {
        return thumbnails.size();
    }

    public void setThumbnails(List<String> thumbnails) {
        this.thumbnails = thumbnails;
    }

    public void setClickHandler(OnClickListener clickHandler) {
        this.clickHandler = clickHandler;
    }

    public interface OnClickListener {
        void click(String uri);
    }
}

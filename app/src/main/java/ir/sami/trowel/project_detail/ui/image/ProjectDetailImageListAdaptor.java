package ir.sami.trowel.project_detail.ui.image;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ir.sami.trowel.R;

public class ProjectDetailImageListAdaptor extends RecyclerView.Adapter<ProjectDetailImageListItemViewHolder> {

    private List<String> thumbnails = new ArrayList<>();
    private OnClickListener clickHandler;

    @NonNull
    @Override
    public ProjectDetailImageListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.project_detail_list_item, parent, false);
        return new ProjectDetailImageListItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectDetailImageListItemViewHolder holder, int position) {
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

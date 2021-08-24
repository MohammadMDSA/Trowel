package ir.sami.trowel.project_detail.ui.image;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import ir.sami.trowel.R;

public class ProjectDetailImageListItemViewHolder extends RecyclerView.ViewHolder {

    private ImageView thumbnail;

    public ProjectDetailImageListItemViewHolder(@NonNull View itemView) {
        super(itemView);
        this.thumbnail = itemView.findViewById(R.id.thumbnail);
    }

    public void bind(String uri) {
        Glide.with(this.itemView.getContext()).load(uri).into(thumbnail);
    }
}

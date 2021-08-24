package ir.sami.trowel.project_detail.ui.image;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ir.sami.trowel.R;
import ir.sami.trowel.databinding.FragmentProjectDetailImagesBinding;
import ir.sami.trowel.project_detail.ProjectDetailListAdaptor;
import ir.sami.trowel.project_detail.ui.main.PageViewModel;

/**
 * A placeholder fragment containing a simple view.
 */
public class ProjectDetailImagesFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;
    private FragmentProjectDetailImagesBinding binding;
    private String projectName;
    private ProjectDetailListAdaptor projectDetaImagelListAdaptor;
    private RecyclerView projectList;

    public static ProjectDetailImagesFragment newInstance(String projectName) {
        ProjectDetailImagesFragment fragment = new ProjectDetailImagesFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        fragment.projectName = projectName;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {

        }
        pageViewModel.setIndex(index);

    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentProjectDetailImagesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        pageViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
            }
        });
        projectList = (RecyclerView) root.findViewById(R.id.thumbnail_list);

        projectDetaImagelListAdaptor = new ProjectDetailListAdaptor();
        projectList.setLayoutManager(new GridLayoutManager(getActivity(), 4, RecyclerView.VERTICAL, false));
        projectList.setAdapter(projectDetaImagelListAdaptor);
        projectDetaImagelListAdaptor.setClickHandler((ProjectDetailListAdaptor.OnClickListener) uri -> {

        });
        File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
        File projectRoot = new File(trowelRoot, projectName);
        File rawImagesRoot = new File(projectRoot, "Raw Images");
        if (!rawImagesRoot.exists())
            return root;
        List<String> thumbnails = Arrays.stream(rawImagesRoot.listFiles()).map(f -> f.getAbsolutePath()).collect(Collectors.toList());
        projectDetaImagelListAdaptor.setThumbnails(thumbnails);
        projectDetaImagelListAdaptor.notifyDataSetChanged();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
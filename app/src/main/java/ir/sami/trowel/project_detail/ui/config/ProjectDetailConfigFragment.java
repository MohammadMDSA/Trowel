package ir.sami.trowel.project_detail.ui.config;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import ir.sami.trowel.BuildConfig;
import ir.sami.trowel.R;
import ir.sami.trowel.data.ModelBuildConfig;

public class ProjectDetailConfigFragment extends PreferenceFragmentCompat {

    private ViewGroup container;

    public static ProjectDetailConfigFragment newInstance() {
        ProjectDetailConfigFragment fragment = new ProjectDetailConfigFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.container = container;
        findPreference("image_max_dim").setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(String.valueOf(newValue));
            return true;
        });
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences_fragment, rootKey);
    }

    public ModelBuildConfig getConfig() {
        ModelBuildConfig config = new ModelBuildConfig();
//        config.setMaxImageDimension(container.findViewById();
        return config;
    }
}
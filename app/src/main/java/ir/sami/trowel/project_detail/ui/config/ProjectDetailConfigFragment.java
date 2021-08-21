package ir.sami.trowel.project_detail.ui.config;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import ir.sami.trowel.R;

public class ProjectDetailConfigFragment extends PreferenceFragmentCompat {

    public static ProjectDetailConfigFragment newInstance() {
        ProjectDetailConfigFragment fragment = new ProjectDetailConfigFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences_fragment, rootKey);
    }
}
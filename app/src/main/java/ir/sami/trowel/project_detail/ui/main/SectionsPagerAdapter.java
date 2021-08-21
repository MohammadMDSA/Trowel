package ir.sami.trowel.project_detail.ui.main;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import ir.sami.trowel.R;
import ir.sami.trowel.project_detail.ui.config.ProjectDetailConfigFragment;
import ir.sami.trowel.project_detail.ui.image.ProjectDetailImagesFragment;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.project_detail_tab_images, R.string.project_detail_tab_config, R.string.project_detail_tab_logs};
    private final Context mContext;
    private final String projectName;

    public SectionsPagerAdapter(Context context, FragmentManager fm, String projectName) {
        super(fm);
        mContext = context;
        this.projectName = projectName;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        switch (position) {
            case 0:
                return ProjectDetailImagesFragment.newInstance(projectName);
            case 1:
                return ProjectDetailConfigFragment.newInstance();

            default:
                return ProjectDetailImagesFragment.newInstance(projectName);
        }

    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        return 2;
    }
}
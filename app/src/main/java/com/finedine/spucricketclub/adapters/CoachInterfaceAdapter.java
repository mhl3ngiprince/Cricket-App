package com.finedine.spucricketclub.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.finedine.spucricketclub.fragments.TeamManagementFragment;
import com.finedine.spucricketclub.fragments.FaceRegistrationFragment;

/**
 * Adapter for ViewPager2 in CoachInterfaceActivity
 */
public class CoachInterfaceAdapter extends FragmentStateAdapter {
    private static final int NUM_TABS = 2;

    // Use our custom fragments
    private Fragment teamManagementFragment = new TeamManagementFragment();
    private Fragment faceRegistrationFragment = new FaceRegistrationFragment();

    public CoachInterfaceAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return empty fragments since we're managing views directly
        switch (position) {
            case 0:
                return teamManagementFragment;
            case 1:
                return faceRegistrationFragment;
            default:
                return teamManagementFragment;
        }
    }

    @Override
    public int getItemCount() {
        return NUM_TABS;
    }
}
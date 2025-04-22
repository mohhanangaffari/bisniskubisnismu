package com.example.bisniskubisnismu;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ProfileFragmentAdapater extends FragmentStateAdapter {

    public ProfileFragmentAdapater(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0 : return new BiographyFragment();
            case 1 : return new RatingFragment();
            case 2 : return new ListBisnisFragment();
            default: return new ListBisnisFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
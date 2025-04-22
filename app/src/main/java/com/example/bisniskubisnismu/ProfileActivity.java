package com.example.bisniskubisnismu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ProfileActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private  ProfileFragmentAdapater profileFragmentAdapater;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tabLayout = findViewById(R.id.tab_profile_layout);
        viewPager = findViewById(R.id.viewpagebio);

        profileFragmentAdapater = new ProfileFragmentAdapater(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(profileFragmentAdapater);

        new TabLayoutMediator(tabLayout,viewPager,(tab,position)->{
          switch (position){
              case 0:
                  tab.setText("Biography");
                  break;
              case 1:
                  tab.setText("Rating");
                  break;
              case 2:
                  tab.setText("List Bisnis");
                  break;
          }
        }).attach();
    }
}
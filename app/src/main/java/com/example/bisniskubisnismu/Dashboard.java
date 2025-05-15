package com.example.bisniskubisnismu;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Dashboard extends AppCompatActivity {

    RecyclerView recyclerView;
    List<BusinessModel> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard); // Pastikan layout ini berisi RecyclerView dengan id "businessRecycler"

        recyclerView = findViewById(R.id.businessRecycler);
        list = new ArrayList<>();

        list.add(new BusinessModel("Budi", "Bisnis Gym, Tanjung Karang", R.drawable.gym, R.drawable.avatar_budi));
        list.add(new BusinessModel("Joko", "Bisnis Ketoprak, Labuhan Ratu", R.drawable.ketoprak, R.drawable.avatar_joko));
        list.add(new BusinessModel("Anwar", "Bisnis Elektronik, Teluk Betung", R.drawable.elektronik, R.drawable.avatar_anwar));

        BusinessAdapter adapter = new BusinessAdapter(list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
}

package com.example.bisniskubisnismu;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class RatingFragment extends Fragment {
    private RecyclerView recyclerView;
    private RatingsAdapter ratingsAdapter;
    private List<ModelRating> modelRatingList;

    public RatingFragment() {
        super(R.layout.fragment_rating);


    }

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState){
        View view = layoutInflater.inflate(R.layout.fragment_rating, container, false);
        recyclerView = view.findViewById(R.id.listrating);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        modelRatingList = new ArrayList<>();
        modelRatingList.add(new ModelRating("hanan","mantep ni org","https://drive.google.com/uc?export=download&id=1XUeGXtNvyAHIjji2TMowSDkWRJZk3wAP"));

        ratingsAdapter = new RatingsAdapter(modelRatingList);
        recyclerView.setAdapter(ratingsAdapter);
        return view;

    }


}
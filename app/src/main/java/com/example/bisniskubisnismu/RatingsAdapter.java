package com.example.bisniskubisnismu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RatingsAdapter extends RecyclerView.Adapter<RatingsAdapter.ViewHolder>{
    private List<ModelRating> modelRatingList;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView nama;
        public TextView comment;
        public ImageView foto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nama = itemView.findViewById(R.id.namaprofile);
            comment = itemView.findViewById(R.id.commentprofile);
            foto = itemView.findViewById(R.id.profileimgrating);

        }
    }

    public RatingsAdapter(List<ModelRating> modelRatingList){
        this.modelRatingList = modelRatingList;
    }

    public RatingsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ratingscardlayout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModelRating modelRating = modelRatingList.get(position);
        holder.nama.setText(modelRating.getNama());
        holder.comment.setText(modelRating.getComment());
        Glide.with(holder.foto.getContext()).load(modelRating.getFoto()).into(holder.foto);
    }


    public int getItemCount(){
        return modelRatingList.size();
    }
}

package com.example.bisniskubisnismu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BusinessAdapter extends RecyclerView.Adapter<BusinessAdapter.ViewHolder> {

    private List<BusinessModel> businessList;

    public BusinessAdapter(List<BusinessModel> businessList) {
        this.businessList = businessList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_business, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusinessModel business = businessList.get(position);
        holder.imgBusiness.setImageResource(business.getImageResId());
        holder.avatar.setImageResource(business.getAvatarResId());
        holder.ownerAndTitle.setText(business.getOwner() + "  " + business.getTitle());
    }

    @Override
    public int getItemCount() {
        return businessList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBusiness;
        CircleImageView avatar;
        TextView ownerAndTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBusiness = itemView.findViewById(R.id.imgBusiness);
            avatar = itemView.findViewById(R.id.avatar);
            ownerAndTitle = itemView.findViewById(R.id.ownerAndTitle);
        }
    }
}

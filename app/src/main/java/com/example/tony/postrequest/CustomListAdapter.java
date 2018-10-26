package com.example.tony.postrequest;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class CustomListAdapter extends ArrayAdapter<SimilarImages> {

    ArrayList<SimilarImages> images;
    Context context;
    int resource;


    public CustomListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<SimilarImages> similarImages) {
        super(context, resource, similarImages);
        this.images= similarImages;
        this.context=context;
        this.resource=resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if(convertView == null){
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.custom_list_layout,null,true);
        }

        SimilarImages similarImages = getItem(position);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageViewProduct);

        Picasso.get().load(similarImages.getImage()).into(imageView);

        return convertView;
    }
}

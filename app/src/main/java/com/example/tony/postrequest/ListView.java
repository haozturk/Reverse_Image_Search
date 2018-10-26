package com.example.tony.postrequest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.ArrayList;

public class ListView extends AppCompatActivity {

    android.widget.ListView listView;
    ArrayList<SimilarImages> arrayList;
    ArrayList<String> imageLinks;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview);

        imageLinks = new ArrayList<String>();

        Intent i = getIntent();
        imageLinks = i.getStringArrayListExtra("imageLinks");


        listView = (android.widget.ListView) findViewById(R.id.listView);
        arrayList = new ArrayList<SimilarImages>();

        for(int j=0;j< imageLinks.size();j++){
            arrayList.add(new SimilarImages(imageLinks.get(j)));
        }

        CustomListAdapter adapter = new CustomListAdapter(
                getApplicationContext(),
                R.layout.custom_list_layout,
                arrayList
        );

        listView.setAdapter(adapter);

    }
}

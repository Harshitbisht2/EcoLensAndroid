package com.knight.ecolens;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Fetching data from Room on a background thread
        new Thread(()->{
            List<ScannedItem> list = AppDatabase.getInstance(this).scanDao().getAllScans();

            //UI updates must happen on the Main Thread
            runOnUiThread(()->{
                if(list != null){
                    HistoryAdapter adapter = new HistoryAdapter(list);
                    recyclerView.setAdapter(adapter);
                }
            });
        }).start();

    }
}

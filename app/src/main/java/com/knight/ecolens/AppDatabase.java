package com.knight.ecolens;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ScannedItem.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase{
    public abstract ScanDao scanDao();
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if(instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "ecolens_db")
                    .fallbackToDestructiveMigration()
                    .build();

        }
        return instance;
    }
}


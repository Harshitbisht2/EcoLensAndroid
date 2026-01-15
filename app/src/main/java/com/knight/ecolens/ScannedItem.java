package com.knight.ecolens;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "scan_history")
public class ScannedItem {
    @PrimaryKey(autoGenerate = true) public int id;
    public String name;
    public String category;
    public long timestamp;

    public ScannedItem(String name, String category, long timestamp){
        this.name = name;
        this.category = category;
        this.timestamp = timestamp;
    }
}

package com.knight.ecolens;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ScanDao {
    @Insert void insert(ScannedItem item);

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    List<ScannedItem> getAllScans();
}

package com.knight.ecolens;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<ScannedItem> scanList;

    public HistoryAdapter(List<ScannedItem> scanList) {
        this.scanList = scanList;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        ScannedItem item = scanList.get(i);
        holder.tvName.setText(item.name);
        holder.tvCategory.setText(item.category);

        // Converting timestamp to readable date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(item.timestamp)));

    }

    @Override
    public int getItemCount() {
        return scanList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvDate;
        public ViewHolder(@NonNull View itemView){
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvCategory = itemView.findViewById(R.id.tvItemName);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}

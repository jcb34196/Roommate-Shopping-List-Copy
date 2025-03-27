package edu.uga.cs.roommateshoppinglist;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PurchasedItemsAdapter extends RecyclerView.Adapter<PurchasedItemsAdapter.ViewHolder> {
    private List<PurchasedItem> purchasedItems;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onUndoClick(int position);
        void onEditClick(int position);
    }

    public PurchasedItemsAdapter(List<PurchasedItem> purchasedItems, OnItemClickListener listener) {
        this.purchasedItems = purchasedItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchased, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PurchasedItem item = purchasedItems.get(position);

        holder.textViewItem.setText(String.join(", ", item.getItems()));
        holder.textViewDate.setText("Date: " + item.getDate());
        holder.textViewName.setText("Name: " + item.getName());
        holder.textViewCost.setText(String.format("Total Order Cost: $%.2f", item.getTotalPrice()));

        Log.d("PurchasedItemsAdapter", "Binding item: " + item.getItems());  // Debug log

        holder.buttonUndo.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUndoClick(position);
            }
        });
        holder.buttonEdit.setOnClickListener(v ->
        {
            if (listener != null) {
                listener.onEditClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return purchasedItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewItem;
        TextView textViewDate;
        TextView textViewName;
        TextView textViewCost;
        Button buttonUndo;
        Button buttonEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewItem = itemView.findViewById(R.id.textViewItem);
            textViewDate = itemView.findViewById(R.id.textPurchaseDate);
            textViewName = itemView.findViewById(R.id.textRoommateName);
            textViewCost = itemView.findViewById(R.id.textTotalCost);
            buttonUndo = itemView.findViewById(R.id.buttonUndo);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
        }
    }
}



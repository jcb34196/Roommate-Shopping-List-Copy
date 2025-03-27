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

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {
    private List<String> shoppingList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(int position);
        void onEditClick(int position);
        void onBasketClick(int position);
    }

    public ShoppingListAdapter(List<String> shoppingList, OnItemClickListener listener) {
        this.shoppingList = shoppingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = shoppingList.get(position);
        holder.itemName.setText(item);

        Log.d("ShoppingListAdapter", "Binding item: " + item);  // Debug log

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position);
            }
        });
        // Set the edit button listener
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(position);
            }
        });
        // Set the purchased button listener
        holder.basketButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBasketClick(position);
            }
        });
    }


    @Override
    public int getItemCount() {
        return shoppingList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName;
        Button deleteButton;
        Button editButton;
        Button basketButton;
        public ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            deleteButton = itemView.findViewById(R.id.buttonDelete);
            editButton = itemView.findViewById(R.id.buttonEdit);
            basketButton = itemView.findViewById(R.id.buttonBasket);
        }
    }
}



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

public class ShoppingBasketAdapter extends RecyclerView.Adapter<ShoppingBasketAdapter.ViewHolder> {
    private List<String> shoppingBasket;
    private ShoppingBasketAdapter.OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(int position);
    }

    public ShoppingBasketAdapter(List<String> shoppingBasketItems, OnItemClickListener listener) {
        this.shoppingBasket = shoppingBasketItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.basket_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = shoppingBasket.get(position);
        holder.itemName.setText(item);

        Log.d("ShoppingBasketAdapter", "Binding item: " + item);

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return shoppingBasket.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName;
        Button deleteButton;
        public ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            deleteButton = itemView.findViewById(R.id.buttonDelete);
        }
    }
}


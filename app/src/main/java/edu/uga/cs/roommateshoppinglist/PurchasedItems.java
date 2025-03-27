package edu.uga.cs.roommateshoppinglist;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.List;


public class PurchasedItems extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView textTotalCost;
    private PurchasedItemsAdapter adapter;
    private List<PurchasedItem> purchasedItems; // Change List type to PurchasedItem
    private FirebaseDatabase database;
    private DatabaseReference purchasedItemsRef;
    private DatabaseReference shoppingListRef;
    private Button buttonPay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_purchased_items);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase references
        database = FirebaseDatabase.getInstance();
        purchasedItemsRef = database.getReference("purchased_items");
        shoppingListRef = database.getReference("shopping_list");

        // Initialize UI components
        textTotalCost = findViewById(R.id.textTotalCost);
        recyclerView = findViewById(R.id.recyclerView);
        buttonPay = findViewById(R.id.buttonPay);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        purchasedItems = new ArrayList<>();
        adapter = new PurchasedItemsAdapter(purchasedItems, new PurchasedItemsAdapter.OnItemClickListener() {
            @Override
            public void onUndoClick(int position) {
                undoItem(position);
            }

            @Override
            public void onEditClick(int position) {
                editItemPrice(position);
            }
        });

        //updateTotalCost();
        recyclerView.setAdapter(adapter);
        fetchPurchasedItems();

        buttonPay.setOnClickListener(view -> {
            purchasedItems.clear();
            deleteAllItems();

            Toast.makeText(this, "Thanks for the payment!", Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
            textTotalCost.setText("$0.00");
        });
    }

    // Fetch purchased items from Firebase
    private void fetchPurchasedItems() {
        purchasedItemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                purchasedItems.clear();
                double totalPrice = 0;

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    PurchasedItem item = itemSnapshot.getValue(PurchasedItem.class);
                    if (item != null) {
                        item.setKey(itemSnapshot.getKey());
                        purchasedItems.add(item);
                        totalPrice += item.getTotalPrice();
                    }
                }

                textTotalCost.setText(String.format("$%.2f", totalPrice));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(PurchasedItems.this, "Failed to load purchased items.", Toast.LENGTH_SHORT).show();
                Log.e("PurchasedItemsActivity", "Error: " + error.getMessage());
            }
        });
    }



    private static final double TAX_RATE = 0.07;

    private void undoItem(int position) {
        PurchasedItem itemToUndo = purchasedItems.get(position);
        String key = itemToUndo.getKey(); // Ensure PurchasedItem includes a `key` field populated when fetching data

        // Get the list of items from the purchased item
        List<String> itemsToMoveBack = itemToUndo.getItems(); // List like ["Tea Quantity 1 - $1.99"]

        // Move items back to the shopping list
        for (String item : itemsToMoveBack) {
            String itemNameWithoutPrice = item.split(" - ")[0];
            String validItemName = itemNameWithoutPrice.replaceAll("[.#$\\[\\]]", "_");

            shoppingListRef.child(validItemName).setValue(itemNameWithoutPrice)
                    .addOnSuccessListener(aVoid -> {
                        if (itemsToMoveBack.indexOf(item) == itemsToMoveBack.size() - 1) {
                            // Remove the entire purchased item from Firebase
                            removeFromPurchasedItems(key);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(PurchasedItems.this, "Failed to undo item.", Toast.LENGTH_SHORT).show();
                        Log.e("PurchasedItemsActivity", "Error: " + e.getMessage());
                    });
        }

        // Remove the item from the local list and notify adapter
        purchasedItems.remove(position);
        adapter.notifyItemRemoved(position);

        // Show success message and update the total cost
        Toast.makeText(PurchasedItems.this, "Item(s) moved back to shopping list.", Toast.LENGTH_SHORT).show();
        updateTotalCost();
    }


    // Remove the entire purchased item from the purchased_items Firebase reference
    private void removeFromPurchasedItems(String key) {
        purchasedItemsRef.child(key).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d("PurchasedItemsActivity", "Purchased item successfully removed from Firebase.");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PurchasedItems.this, "Failed to remove item from purchased list.", Toast.LENGTH_SHORT).show();
                    Log.e("PurchasedItemsActivity", "Error: " + e.getMessage());
                });
    }




    private void editItemPrice(int position) {
        PurchasedItem itemToEdit = purchasedItems.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Total Price");
        View dialogView = getLayoutInflater().inflate(R.layout.edit_price, null);
        builder.setView(dialogView);

        EditText editPriceInput = dialogView.findViewById(R.id.editPriceInput);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newPrice = editPriceInput.getText().toString().trim();
            if (!TextUtils.isEmpty(newPrice)) {
                try {
                    // Parse the new price
                    double updatedPrice = Double.parseDouble(newPrice);
                    itemToEdit.setTotalPrice(updatedPrice); // Update the price locally

                    // Update the Firebase database
                    String key = itemToEdit.getKey(); // Ensure the item has a key set when fetched
                    if (key != null) {
                        purchasedItemsRef.child(key).child("totalPrice").setValue(updatedPrice)
                                .addOnSuccessListener(aVoid -> {
                                    purchasedItems.set(position, itemToEdit); // Update local list
                                    adapter.notifyItemChanged(position); // Notify adapter
                                    //updateTotalCost(); // Update the total cost
                                    Toast.makeText(PurchasedItems.this, "Total price updated!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(PurchasedItems.this, "Failed to update price.", Toast.LENGTH_SHORT).show();
                                    Log.e("PurchasedItemsActivity", "Error: " + e.getMessage());
                                });
                    } else {
                        Toast.makeText(PurchasedItems.this, "Item key is missing.", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(PurchasedItems.this, "Invalid price format.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(PurchasedItems.this, "Price cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }


    // Delete all items
    private void deleteAllItems() {
        purchasedItemsRef.setValue(null)
                .addOnSuccessListener(aVoid -> {
                    purchasedItems.clear();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PurchasedItems.this, "Failed to delete items.", Toast.LENGTH_SHORT).show();
                    Log.e("PurchasedItemsActivity", "Error: " + e.getMessage());
                });
    }

    // Update total cost
    private void updateTotalCost() {
        double totalPrice = 0;
        for (PurchasedItem item : purchasedItems) {
            totalPrice += item.getTotalPrice();
        }

        // Add tax
        double totalPriceWithTax = totalPrice + (totalPrice * TAX_RATE);

        textTotalCost.setText(String.format("$%.2f", totalPriceWithTax));
    }
}


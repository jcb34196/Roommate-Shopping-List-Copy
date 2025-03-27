package edu.uga.cs.roommateshoppinglist;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShoppingBasket extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ShoppingBasketAdapter adapter;
    private List<String> shoppingBasketItems;
    private FirebaseDatabase database;
    private DatabaseReference basketRef;
    private Button btnCheckout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shopping_basket);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        btnCheckout = findViewById(R.id.buttonCheckout);
        database = FirebaseDatabase.getInstance();
        basketRef = database.getReference("shopping_basket");
        shoppingBasketItems = new ArrayList<>();
        adapter = new ShoppingBasketAdapter(shoppingBasketItems, new ShoppingBasketAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(int position) {
                deleteItem(position);
            }
        });
        recyclerView.setAdapter(adapter);
        fetchBasketItems();

        btnCheckout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkout();
            }
        });

    }

    // Fetch items from the shopping basket
    private void fetchBasketItems() {
        basketRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                shoppingBasketItems.clear();
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    String item = itemSnapshot.getValue(String.class);
                    shoppingBasketItems.add(item);
                }
                Log.d("ShoppingBasket", "Items fetched: " + shoppingBasketItems.size()); // Debug log
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ShoppingBasket.this, "Failed to load shopping basket.", Toast.LENGTH_SHORT).show();
                Log.e("ShoppingBasket", "Error fetching data: " + databaseError.getMessage());
            }
        });
    }

    // Delete an item from Firebase
    private void deleteItem(int position) {
        String itemToDelete = shoppingBasketItems.get(position); // Get the item to be deleted

        // Remove item from shopping basket
        basketRef.orderByValue().equalTo(itemToDelete).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().removeValue(); // Delete the item from the shopping basket

                    String itemName = extractItemName(itemToDelete);

                    // Add item back to shopping list (assuming shoppingListRef is the reference to the shopping list in Firebase)
                    DatabaseReference shoppingListRef = database.getReference("shopping_list");
                    shoppingListRef.push().setValue(itemName); // Add the item back to the shopping list
                }

                // Remove the item from the local shopping basket list and update the RecyclerView
                shoppingBasketItems.remove(position);
                adapter.notifyItemRemoved(position);

                // Show confirmation message
                Toast.makeText(ShoppingBasket.this, "Item deleted and added back to shopping list!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ShoppingBasket.this, "Failed to delete item.", Toast.LENGTH_SHORT).show();
                Log.e("ShoppingBasket", "Error deleting item: " + databaseError.getMessage());
            }
        });
    }

    // Helper method to extract item name from the string
    private String extractItemName(String itemDetails) {
        // Assuming the itemDetails are stored as "name - price" (e.g., "Apple - $1.99")
        if (itemDetails.contains("-")) {
            return itemDetails.split("-")[0].trim(); // Extract everything before the dash
        }
        return itemDetails; // Return as-is if no dash is found
    }

    private void checkout() {
        if (shoppingBasketItems.isEmpty()) {
            Toast.makeText(this, "Shopping basket is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a dialog to ask for the user's name
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enter Your Name");

        // Set up the input
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Name");
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String userName = input.getText().toString().trim();

            if (userName.isEmpty()) {
                Toast.makeText(ShoppingBasket.this, "Name is required to checkout.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Proceed with checkout
            performCheckout(userName);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void performCheckout(String userName) {
        DatabaseReference purchasedItemsRef = database.getReference("purchased_items");
        List<String> purchasedItems = new ArrayList<>(shoppingBasketItems); // Copy current basket items
        final double[] totalPrice = {0.0}; // Use an array to hold the total price
        double taxRate = 0.07; // Example tax rate of 7%

        // Calculate total price
        for (String itemDetails : shoppingBasketItems) {
            String[] parts = itemDetails.split("-");
            if (parts.length == 2) {
                try {
                    String priceString = parts[1].replaceAll("[^\\d.]", ""); // Extract price as string
                    double price = Double.parseDouble(priceString); // Parse to double
                    totalPrice[0] += price;
                } catch (NumberFormatException e) {
                    Log.e("ShoppingBasket", "Error parsing price: " + e.getMessage());
                }
            }
        }

        totalPrice[0] += totalPrice[0] * taxRate; // Add tax

        // Get current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());

        // Save purchased items and total price to Firebase
        purchasedItemsRef.push().setValue(new PurchasedItem(currentDate, userName, purchasedItems, totalPrice[0]))
                .addOnSuccessListener(aVoid -> {
                    // Clear the shopping basket
                    basketRef.removeValue().addOnSuccessListener(aVoid1 -> {
                        shoppingBasketItems.clear();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(ShoppingBasket.this, "Checkout complete! Total: $" + String.format("%.2f", totalPrice[0]), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ShoppingBasket.this, "Checkout failed. Try again.", Toast.LENGTH_SHORT).show();
                    Log.e("ShoppingBasket", "Error saving purchase: " + e.getMessage());
                });
    }

}


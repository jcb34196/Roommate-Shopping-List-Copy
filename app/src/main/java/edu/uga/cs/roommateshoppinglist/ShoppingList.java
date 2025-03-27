package edu.uga.cs.roommateshoppinglist;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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

public class ShoppingList extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ShoppingListAdapter adapter;
    private List<String> shoppingListItems;
    private FirebaseDatabase database;
    private DatabaseReference shoppingListRef;
    private EditText itemEditText;
    private Button addItemButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shopping_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialization of UI elements, recyclerView, database, and shopping list Adapter
        itemEditText = findViewById(R.id.editTextItem);
        addItemButton = findViewById(R.id.buttonAddItem);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        database = FirebaseDatabase.getInstance();
        shoppingListRef = database.getReference("shopping_list");
        shoppingListItems = new ArrayList<>();
        adapter = new ShoppingListAdapter(shoppingListItems, new ShoppingListAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(int position) {
                deleteItem(position);
            }
            @Override
            public void onEditClick(int position) {
                editItem(position);
            }
            @Override
            public void onBasketClick(int position) {moveItemToBasket(position);}
        });
        recyclerView.setAdapter(adapter);
        fetchShoppingList();

        // Add item to Firebase when button is clicked
        addItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String item = itemEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(item)) {
                    addItemToFirebase(item);
                    itemEditText.setText("");
                } else {
                    Toast.makeText(ShoppingList.this, "Item name cannot be empty!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Fetch shopping list from Firebase realtime database
    private void fetchShoppingList() {
        shoppingListRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                shoppingListItems.clear();
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    String item = itemSnapshot.getValue(String.class);
                    shoppingListItems.add(item);
                }
                Log.d("ShoppingList", "Items fetched: " + shoppingListItems.size());  // Debug log
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ShoppingList.this, "Failed to load shopping list.", Toast.LENGTH_SHORT).show();
                Log.e("ShoppingList", "Error fetching data: " + databaseError.getMessage());
            }
        });
    }

    // Add a new item to Firebase
    private void addItemToFirebase(String item) {
        String itemId = shoppingListRef.push().getKey();
        shoppingListRef.child(itemId).setValue(item);
    }

    // Delete an item from Firebase
    private void deleteItem(int position) {
        String itemToDelete = shoppingListItems.get(position);
        shoppingListRef.orderByValue().equalTo(itemToDelete).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().removeValue();
                }
                shoppingListItems.remove(position);
                adapter.notifyItemRemoved(position);
                Toast.makeText(ShoppingList.this, "Item deleted!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ShoppingList.this, "Failed to delete item.", Toast.LENGTH_SHORT).show();
                Log.e("ShoppingList", "Error deleting item: " + databaseError.getMessage());
            }
        });
    }

    // Edit an item in Firebase
    private void editItem(int position) {
        String item = shoppingListItems.get(position);
        final EditText editText = new EditText(this);
        editText.setText(item);
        new AlertDialog.Builder(this)
                .setTitle("Edit Item")
                .setView(editText)
                .setPositiveButton("Save", (dialog, which) -> {
                    String updatedItem = editText.getText().toString().trim();
                    if (!TextUtils.isEmpty(updatedItem)) {
                        updateItemInFirebase(position, updatedItem);
                    } else {
                        Toast.makeText(ShoppingList.this, "Item cannot be empty!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Update an item in Firebase
    private void updateItemInFirebase(int position, String updatedItem) {
        String oldItem = shoppingListItems.get(position);
        shoppingListRef.orderByValue().equalTo(oldItem).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().setValue(updatedItem);
                }
                shoppingListItems.set(position, updatedItem);
                adapter.notifyItemChanged(position);
                Toast.makeText(ShoppingList.this, "Item updated!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ShoppingList.this, "Failed to update item.", Toast.LENGTH_SHORT).show();
                Log.e("ShoppingList", "Error updating item: " + databaseError.getMessage());
            }
        });
    }

    private void moveItemToBasket(int position) {
        String itemToMove = shoppingListItems.get(position);

        // Prompt the user to enter a price for the item
        final EditText priceEditText = new EditText(this);
        priceEditText.setHint("Enter price");

        new AlertDialog.Builder(this)
                .setTitle("Enter Price")
                .setView(priceEditText)
                .setPositiveButton("OK", (dialog, which) -> {
                    String priceInput = priceEditText.getText().toString().trim();

                    if (!TextUtils.isEmpty(priceInput)) {
                        double price;
                        try {
                            price = Double.parseDouble(priceInput);

                            // Find the item in shopping_list and move it to shopping_basket with the price
                            shoppingListRef.orderByValue().equalTo(itemToMove).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        String key = snapshot.getKey(); // Key of the item in shopping_list

                                        // Add the item with the price to shopping_basket
                                        DatabaseReference basketRef = database.getReference("shopping_basket");
                                        basketRef.child(key).setValue(itemToMove + " - $" + price);

                                        // Remove the item from shopping_list
                                        snapshot.getRef().removeValue();
                                    }

                                    // Update the local list and notify the adapter
                                    shoppingListItems.remove(position);
                                    adapter.notifyItemRemoved(position);
                                    Toast.makeText(ShoppingList.this, "Item moved to basket with price!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Toast.makeText(ShoppingList.this, "Failed to move item to basket.", Toast.LENGTH_SHORT).show();
                                    Log.e("ShoppingList", "Error moving item: " + databaseError.getMessage());
                                }
                            });
                        } catch (NumberFormatException e) {
                            Toast.makeText(ShoppingList.this, "Invalid price. Please enter a valid number.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ShoppingList.this, "Price cannot be empty!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


}

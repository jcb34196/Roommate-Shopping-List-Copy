package edu.uga.cs.roommateshoppinglist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    public static final String TAG = "RoommateShoppingList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in,if no user is logged in, redirect to Login
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user is logged in. Redirecting to Login.");
            redirectToLogin();
        }

        // Logout button
        Button btnLogout = findViewById(R.id.buttonLogout);
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Log.d(TAG, "User logged out. Redirecting to Login.");
            redirectToLogin();
        });

        // Button to navigate to ShoppingList activity
        Button btnShoppingList = findViewById(R.id.buttonShoppingList);
        btnShoppingList.setOnClickListener(v -> {
            Log.d(TAG, "Navigating to ShoppingList activity.");
            Intent intent = new Intent(MainActivity.this, ShoppingList.class);
            startActivity(intent);
        });

        // Button to navigate to ShoppingBasket activity
        Button btnBasketList = findViewById(R.id.buttonShoppingBasket);
        btnBasketList.setOnClickListener(v -> {
            Log.d(TAG, "Navigating to ShoppingBasket activity.");
            Intent intent = new Intent(MainActivity.this, ShoppingBasket.class);
            startActivity(intent);
        });

        // Button to navigate to PurchasedItems activity
        Button btnPurchased = findViewById(R.id.buttonPurchased);
        btnPurchased.setOnClickListener(v -> {
            Log.d(TAG, "Navigating to PurchasedItems activity.");
            Intent intent = new Intent(MainActivity.this, PurchasedItems.class);
            startActivity(intent);
        });

        Log.d(TAG, "onCreate: MainActivity created.");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: MainActivity started.");

        // Recheck user authentication state
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "onStart: No user is logged in. Redirecting to Login.");
            redirectToLogin();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: MainActivity resumed.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: MainActivity paused.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: MainActivity stopped.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: MainActivity destroyed.");
    }

    // Close the current activity, redirect to login page
    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, Login.class);
        startActivity(intent);
        finish();
    }
}

package com.example.a23110035_23110060.view.activity;

import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.bottomsheet.NotificationsBottomSheet;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
        
        android.widget.ImageView btnNotification = findViewById(R.id.btnNotificationMain);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                NotificationsBottomSheet bottomSheet = new NotificationsBottomSheet();
                bottomSheet.show(getSupportFragmentManager(), "Notifications");
            });
        }
    }
}
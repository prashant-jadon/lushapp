package com.chandra.lushapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.chandra.lushapp.fragementsForMainScreen.InternetChatFragment;
import com.chandra.lushapp.fragementsForMainScreen.MyCircleFragment;
import com.chandra.lushapp.fragementsForMainScreen.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {


    private HashMap<Integer, Fragment> fragmentMap = new HashMap<>();
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        SharedPreferences sharedPreferences = getSharedPreferences("__lushapp__",MODE_PRIVATE);
        SharedPreferences.Editor editor  = sharedPreferences.edit();
        editor.putString("credential",user.getUid());
        editor.apply();

        // Load the default fragment
        currentFragment = new InternetChatFragment();
        loadFragment(currentFragment);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                int itemId = item.getItemId();

                // Check if the fragment is already loaded
                if (fragmentMap.containsKey(itemId)) {
                    fragment = fragmentMap.get(itemId);
                } else {
                    if (itemId == R.id.chatwithothers) {
                        fragment = new InternetChatFragment();
                    } else if (itemId == R.id.mycircle) {
                        fragment = new MyCircleFragment();
                    } else if (itemId == R.id.profile) {
                        fragment = new ProfileFragment();
                    }

                    if (fragment != null) {
                        fragmentMap.put(itemId, fragment);
                    }
                }

                if (fragment != null && fragment != currentFragment) {
                    loadFragment(fragment);
                    currentFragment = fragment; // Update current fragment
                    return true;
                }

                return false;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.contentFrame, fragment);
        transaction.commit();
    }
}
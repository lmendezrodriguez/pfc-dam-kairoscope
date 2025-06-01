package com.lmr.kairoscope.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lmr.kairoscope.R;

/**
 * Activity principal que gestiona la navegación global y el tema de la aplicación.
 * Configura el Navigation Component y el modo claro/oscuro.
 */
public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Aplicar tema guardado
        SharedPreferences prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean("night_mode", false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // Verificar auth ANTES de setContentView
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setContentView(R.layout.activity_main); // Solo UNA vez

        // Configurar toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configurar Navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Configurar appBarConfiguration ANTES de navegar
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.loginFragment,
                    R.id.registerFragment,
                    R.id.deckListFragment
            ).build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

            // AHORA navegar si está autenticado
            if (currentUser != null) {
                navController.navigate(R.id.deckListFragment);
            }

            // Listener para toolbar
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                if (destinationId == R.id.loginFragment || destinationId == R.id.registerFragment) {
                    toolbar.setVisibility(View.GONE);
                } else {
                    toolbar.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    /**
     * Habilita navegación hacia arriba usando Navigation Component.
     */
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_toggle_night_mode) {
            toggleNightMode();
            return true;
        } else if (id == R.id.action_user_profile) {
            // Navegar a perfil de usuario
            if (navController != null) {
                try {
                    navController.navigate(R.id.action_deckListFragment_to_userProfileFragment);
                } catch (Exception e) {
                    // Si no estamos en DeckList, ir directamente
                    navController.navigate(R.id.userProfileFragment);
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Alterna entre modo claro y oscuro, guardando la preferencia.
     */
    private void toggleNightMode() {
        SharedPreferences prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean("night_mode", false);

        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            prefs.edit().putBoolean("night_mode", false).apply();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            prefs.edit().putBoolean("night_mode", true).apply();
        }
    }
}
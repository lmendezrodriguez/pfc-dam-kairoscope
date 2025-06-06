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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lmr.kairoscope.R;

/**
 * Activity principal que gestiona la navegación global y el tema de la aplicación.
 * Configura el Navigation Component, bottom navigation y modo claro/oscuro.
 */
public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;

    private static final String KEY_CURRENT_DESTINATION = "current_destination";
    private int currentDestinationId = R.id.homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Aplicar tema guardado antes de inflar la UI
        applyStoredTheme();

        // Verificar autenticación actual
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setContentView(R.layout.activity_main);

        // Configurar componentes UI
        setupToolbar();
        setupBottomNavigation();
        setupAuthListener();
        setupNavigation(currentUser, savedInstanceState);
    }

    /**
     * Aplica el tema guardado en preferencias antes de crear las vistas.
     */
    private void applyStoredTheme() {
        SharedPreferences prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean("night_mode", false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /**
     * Configura la toolbar principal.
     */
    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    /**
     * Configura la barra de navegación inferior.
     */
    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    /**
     * Configura listener para cambios de estado de autenticación.
     */
    private void setupAuthListener() {
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            // Lógica adicional de autenticación si es necesaria
        });
    }

    /**
     * Configura el Navigation Component y la navegación entre fragmentos.
     */
    private void setupNavigation(FirebaseUser currentUser, Bundle savedInstanceState) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Definir destinos de nivel superior (sin botón atrás)
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.homeFragment,
                    R.id.deckCreationFragment,
                    R.id.deckListFragment,
                    R.id.userProfileFragment,
                    R.id.loginFragment,
                    R.id.registerFragment
            ).build();

            // Vincular Navigation Component con toolbar y bottom navigation
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(bottomNavigation, navController);

            // Gestionar navegación inicial y restauración de estado
            handleInitialNavigation(currentUser, savedInstanceState);

            // Configurar visibilidad de UI según el destino
            setupDestinationChangeListener();
        }
    }

    /**
     * Maneja la navegación inicial basada en autenticación y estado guardado.
     */
    private void handleInitialNavigation(FirebaseUser currentUser, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Restaurar destino previo
            currentDestinationId = savedInstanceState.getInt(KEY_CURRENT_DESTINATION, R.id.homeFragment);
            if (navController.getCurrentDestination() == null ||
                    navController.getCurrentDestination().getId() != currentDestinationId) {
                navController.navigate(currentDestinationId);
            }
        } else if (currentUser != null) {
            // Usuario autenticado va al home
            if (navController.getCurrentDestination() == null ||
                    navController.getCurrentDestination().getId() != R.id.homeFragment) {
                navController.navigate(R.id.homeFragment);
            }
        }
    }

    /**
     * Configura listener para ocultar/mostrar UI según el fragmento actual.
     */
    private void setupDestinationChangeListener() {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();
            Log.d("MainActivity", "Destination changed to: " + getResources().getResourceEntryName(destinationId));

            // Ocultar UI en pantallas de autenticación y detalle
            if (destinationId == R.id.loginFragment ||
                    destinationId == R.id.registerFragment ||
                    destinationId == R.id.deckDetailFragment) {
                toolbar.setVisibility(View.GONE);
                bottomNavigation.setVisibility(View.GONE);
                Log.d("MainActivity", "Navigation UI hidden.");
            } else {
                toolbar.setVisibility(View.VISIBLE);
                bottomNavigation.setVisibility(View.VISIBLE);
                Log.d("MainActivity", "Navigation UI visible.");
            }
        });
    }

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
        }

        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Guardar destino actual para restauración
        if (navController != null && navController.getCurrentDestination() != null) {
            outState.putInt(KEY_CURRENT_DESTINATION, navController.getCurrentDestination().getId());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            currentDestinationId = savedInstanceState.getInt(KEY_CURRENT_DESTINATION, R.id.homeFragment);
        }
    }

    /**
     * Alterna entre modo claro y oscuro guardando la preferencia.
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
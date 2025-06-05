package com.lmr.kairoscope.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider; // Mantener si DeckListViewModel se usa en MainActivity
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Mantener para logs si los usas
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lmr.kairoscope.R;
// Importaciones relacionadas con DeckListResponse y DeckRepository ya no serían necesarias aquí
// si DeckListViewModel solo se usa en los fragmentos.
// import com.lmr.kairoscope.data.model.DeckListResponse;
// import com.lmr.kairoscope.data.repository.DeckRepository;
// import com.lmr.kairoscope.viewmodel.DeckListViewModel;
// import com.google.android.material.snackbar.Snackbar; // No necesario si showDeckLimitMessage se mueve

/**
 * Activity principal que gestiona la navegación global y el tema de la aplicación.
 * Configura el Navigation Component y el modo claro/oscuro.
 */
public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    // DeckListViewModel ahora será gestionado por los fragmentos directamente.
    // private DeckListViewModel deckListViewModel; // <-- ELIMINAR ESTA LÍNEA

    private static final String KEY_CURRENT_DESTINATION = "current_destination";
    private int currentDestinationId = R.id.homeFragment; // Valor por defecto

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

        setContentView(R.layout.activity_main);

        // Configurar toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configurar bottom navigation
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // INICIALIZAR VIEWMODEL (Solo si es estrictamente necesario en MainActivity,
        // por ejemplo, si tienes una lógica de carga inicial que solo la Activity puede manejar).
        // Si no, el DeckListViewModel se creará y gestionará en los fragmentos que lo necesiten.
        // DeckRepository deckRepository = new DeckRepository(this);
        // deckListViewModel = new ViewModelProvider(this, new DeckListViewModel.Factory(deckRepository))
        //         .get(DeckListViewModel.class);

        // AHORA SÍ el AuthStateListener (Mantener si necesitas cargar datos específicos de usuario aquí)
        // Si DeckListViewModel.loadDeckList() solo se usa en DeckListFragment, múevelo allí.
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // Si la carga de la lista de barajas solo es relevante en DeckListFragment,
                // esta línea se puede mover al onResume/onViewCreated de DeckListFragment.
                // Si necesitas cargarla globalmente al inicio de sesión, déjala.
                // deckListViewModel.loadDeckList();
            }
        });

        // Configurar Navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Configurar appBarConfiguration: DEBEN ser los IDs de los destinos "top-level"
            // de tu BottomNavigationView y cualquier otro destino que sea un punto de entrada directo del grafo
            // que NO tenga barra inferior (ej. login/register).
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.homeFragment,          // Corresponde a un ítem de la bottom bar
                    R.id.deckCreationFragment,  // Corresponde a un ítem de la bottom bar
                    R.id.deckListFragment,      // Corresponde a un ítem de la bottom bar
                    R.id.userProfileFragment,    // Corresponde a un ítem de la bottom bar
                    R.id.loginFragment,         // Punto de entrada sin bottom bar
                    R.id.registerFragment       // Punto de entrada sin bottom bar
            ).build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

            // ******* CAMBIO CLAVE: Usamos NavigationUI.setupWithNavController directamente *******
            // Esto permite que NavigationUI maneje la navegación y la selección de ítems
            // de la BottomNavigationView automáticamente y sin conflictos.
            NavigationUI.setupWithNavController(bottomNavigation, navController);

            // ******* ELIMINAR CUALQUIER bottomNavigation.setOnItemSelectedListener aquí *******
            // bottomNavigation.setOnItemSelectedListener(...) // <-- REMOVE THIS ENTIRE BLOCK


            // Restaurar destino o navegar si está autenticado
            if (savedInstanceState != null) {
                currentDestinationId = savedInstanceState.getInt(KEY_CURRENT_DESTINATION, R.id.homeFragment);
                if (navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() != currentDestinationId) {
                    navController.navigate(currentDestinationId);
                }
            } else if (currentUser != null) {
                if (navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() != R.id.homeFragment) {
                    navController.navigate(R.id.homeFragment);
                }
            }

            // Listener para toolbar y bottom navigation visibility
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                Log.d("MainActivity", "Destination changed to: " + getResources().getResourceEntryName(destinationId));

                // Ocultar toolbar y bottom nav en login, register y deck detail
                if (destinationId == R.id.loginFragment ||
                        destinationId == R.id.registerFragment ||
                        destinationId == R.id.deckDetailFragment) {
                    toolbar.setVisibility(View.GONE);
                    bottomNavigation.setVisibility(View.GONE);
                    Log.d("MainActivity", "Bottom navigation hidden.");
                } else {
                    toolbar.setVisibility(View.VISIBLE);
                    bottomNavigation.setVisibility(View.VISIBLE);
                    Log.d("MainActivity", "Bottom navigation visible.");
                    // NavigationUI.setupWithNavController ya maneja la selección del ítem.
                    // No necesitas forzarlo con .setChecked(true) aquí.
                }
            });
        }
    }

    // ******* ELIMINAR ESTOS MÉTODOS DE MAINACTIVITY *******
    // Moverán a DeckCreationFragment o su ViewModel
    // private void showDeckLimitMessage() { ... }
    // private boolean canCreateNewDeck() { ... }
    // ******************************************************

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

        // Opcional: si tienes ítems en el menú de la toolbar que navegan a destinos del nav_graph.
        // NavigationUI.onNavDestinationSelected puede manejar esto.
        return NavigationUI.onNavDestinationSelected(item, navController) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
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
package com.lmr.kairoscope.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentContainerView;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.os.Bundle;
import android.view.View; // Importa View

import com.google.android.material.appbar.MaterialToolbar; // Importa MaterialToolbar
import com.lmr.kairoscope.R; // Asegúrate de que R esté importado correctamente

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private MaterialToolbar toolbar; // Referencia a la Toolbar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtener referencia a la MaterialToolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // Configura la Toolbar como la ActionBar

        // Obtén el NavController del NavHostFragment
        // La forma recomendada es usando findFragmentById y casteando a NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Configura AppBarConfiguration.
            // Necesitamos decirle al NavigationUI qué destinos son de "nivel superior"
            // donde no debe mostrar el botón de retroceso (flecha arriba).
            // Los fragmentos de Login y Register NUNCA deben tener botón de retroceso hacia atrás
            // porque son puntos de entrada o flujos de inicio.
            // DeckListFragment es el destino principal tras la autenticación, tampoco debería tener flecha hacia atrás
            // si es el primer destino visible después del flujo de auth.
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.loginFragment, // ID del fragmento de Login en tu nav_graph.xml
                    R.id.registerFragment, // ID del fragmento de Register en tu nav_graph.xml
                    R.id.deckListFragment // ID del fragmento de DeckList (pantalla principal)
                    // Añade aquí los IDs de cualquier otro fragmento que sea un punto de entrada principal
                    // o al que puedas llegar desde la barra de navegación inferior/drawer (si los añades)
            ).build();


            // Vincula la Toolbar con el NavController y AppBarConfiguration
            // Esto hará que la Toolbar muestre el título del Fragmento actual y el botón de "atrás"
            // automáticamente, respetando los destinos de nivel superior.
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

            // Listener para cambiar la visibilidad de la Toolbar
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                // Ocultar la Toolbar en las pantallas de Login y Register
                if (destinationId == R.id.loginFragment || destinationId == R.id.registerFragment) {
                    toolbar.setVisibility(View.GONE);
                } else {
                    // Mostrar la Toolbar en todas las demás pantallas
                    toolbar.setVisibility(View.VISIBLE);
                }
            });

        } else {
            // Esto no debería pasar si el FragmentContainerView está bien configurado
            // Puedes añadir un Log.e() o un Toast aquí si quieres depurar
        }
    }

    // Este método permite que el botón de flecha arriba/atrás en la Toolbar funcione
    // con el Navigation Component.
    @Override
    public boolean onSupportNavigateUp() {
        // Intenta navegar hacia arriba en la jerarquía de navegación (usando AppBarConfiguration)
        // Si no es posible (porque ya estás en un destino de nivel superior),
        // entonces permite que la Activity maneje el evento (comportamiento por defecto).
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
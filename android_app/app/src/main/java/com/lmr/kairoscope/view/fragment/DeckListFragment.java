package com.lmr.kairoscope.view.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lmr.kairoscope.R; // Importa la clase R para poder referenciar tus layouts

public class DeckListFragment extends Fragment {

    // En pasos futuros, aquí declararemos referencias a elementos UI (EditText, Button)
    // y a nuestro AuthViewModel.

    // El método onCreateView es donde inflamos el layout XML del fragmento
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // R.layout.fragment_login hace referencia al archivo XML que creaste antes
        View view = inflater.inflate(R.layout.fragment_deck_list, container, false);

        // Aquí es donde normalmente encontrarías referencias a los elementos de UI
        // usando view.findViewById(R.id.alguna_id_en_el_layout);
        // Por ahora, lo dejamos simple.

        // Aquí también inicializarías tu ViewModel y empezarías a observar LiveData

        return view; // Devolvemos la vista inflada
    }

    // Otros métodos de ciclo de vida del fragmento (como onViewCreated, onActivityCreated, etc.)
    // se pueden añadir aquí si son necesarios para tareas específicas.
}
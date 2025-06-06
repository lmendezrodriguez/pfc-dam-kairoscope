package com.lmr.kairoscope.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

import com.lmr.kairoscope.R;
import com.lmr.kairoscope.data.model.Deck;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador para mostrar la lista de barajas del usuario en un RecyclerView.
 * Muestra nombre, disciplina, cantidad de cartas y fecha de creación de cada baraja.
 */
public class DeckListAdapter extends RecyclerView.Adapter<DeckListAdapter.DeckViewHolder> {

    private List<Deck> deckList = new ArrayList<>();
    private OnDeckClickListener onDeckClickListener;

    /**
     * Interface para manejar los eventos de click en los elementos de la lista.
     */
    public interface OnDeckClickListener {
        void onDeckClick(Deck deck);
        void onDeckDelete(Deck deck);
    }

    public DeckListAdapter(OnDeckClickListener listener) {
        this.onDeckClickListener = listener;
    }

    @NonNull
    @Override
    public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_deck, parent, false);
        return new DeckViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeckViewHolder holder, int position) {
        Deck deck = deckList.get(position);
        holder.bind(deck);
    }

    @Override
    public int getItemCount() {
        return deckList.size();
    }

    /**
     * Actualiza la lista de barajas y notifica los cambios al RecyclerView.
     */
    public void updateDeckList(List<Deck> newDeckList) {
        this.deckList.clear();
        if (newDeckList != null) {
            this.deckList.addAll(newDeckList);
        }
        notifyDataSetChanged();
    }

    /**
     * ViewHolder que mantiene las referencias a las vistas de cada elemento.
     */
    class DeckViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewDeckName;
        private TextView textViewDiscipline;
        private TextView textViewCardCount;
        private TextView textViewCreationDate;
        private MaterialButton buttonDelete;

        public DeckViewHolder(@NonNull View itemView) {
            super(itemView);
            // Inicializar vistas
            textViewDeckName = itemView.findViewById(R.id.textViewDeckName);
            textViewDiscipline = itemView.findViewById(R.id.textViewDiscipline);
            textViewCardCount = itemView.findViewById(R.id.textViewCardCount);
            textViewCreationDate = itemView.findViewById(R.id.textViewCreationDate);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteDeck);

            // Configurar listeners de click
            itemView.setOnClickListener(v -> {
                if (onDeckClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onDeckClickListener.onDeckClick(deckList.get(position));
                    }
                }
            });

            buttonDelete.setOnClickListener(v -> {
                if (onDeckClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onDeckClickListener.onDeckDelete(deckList.get(position));
                    }
                }
            });
        }

        /**
         * Vincula los datos de una baraja con las vistas del elemento.
         */
        public void bind(Deck deck) {
            textViewDeckName.setText(deck.getName());
            textViewDiscipline.setText(deck.getDiscipline());
            textViewCardCount.setText(deck.getCard_count() + " cartas");
            textViewCreationDate.setText("Creado " + formatDate(deck.getCreated_at()));

            // Aplicar color personalizado de la baraja
            try {
                int color = Color.parseColor(deck.getChosen_color());
                ((MaterialCardView) itemView).setStrokeColor(color);

                // Personalizar chip de disciplina con el color de la baraja
                GradientDrawable drawable = (GradientDrawable) textViewDiscipline.getBackground();
                drawable.setColor(color);

                // Ajustar color del texto según la luminosidad del fondo
                if (isColorDark(color)) {
                    textViewDiscipline.setTextColor(Color.WHITE);
                } else {
                    textViewDiscipline.setTextColor(Color.BLACK);
                }

            } catch (Exception e) {
                // Color por defecto en caso de error
                textViewDiscipline.setTextColor(Color.WHITE);
            }
        }

        /**
         * Convierte fecha ISO a formato legible (dd MMM).
         */
        private String formatDate(String isoDate) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
                Date date = inputFormat.parse(isoDate);
                return outputFormat.format(date);
            } catch (Exception e) {
                return "Reciente";
            }
        }

        /**
         * Determina si un color es oscuro usando la fórmula de luminosidad.
         */
        private boolean isColorDark(int color) {
            double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
            return darkness >= 0.5;
        }
    }
}
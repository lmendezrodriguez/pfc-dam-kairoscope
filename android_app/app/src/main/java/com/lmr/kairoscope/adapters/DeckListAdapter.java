package com.lmr.kairoscope.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.lmr.kairoscope.R;
import com.lmr.kairoscope.data.model.Deck;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying list of user's decks.
 * Shows deck name, discipline, card count and creation date.
 */
public class DeckListAdapter extends RecyclerView.Adapter<DeckListAdapter.DeckViewHolder> {

    private List<Deck> deckList = new ArrayList<>();
    private OnDeckClickListener onDeckClickListener;


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

    public void updateDeckList(List<Deck> newDeckList) {
        this.deckList.clear();
        if (newDeckList != null) {
            this.deckList.addAll(newDeckList);
        }
        notifyDataSetChanged();
    }

    class DeckViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewDeckName;
        private TextView textViewDiscipline;
        private TextView textViewCardCount;
        private View viewColorIndicator;
        private MaterialButton buttonDelete;

        public DeckViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDeckName = itemView.findViewById(R.id.textViewDeckName);
            textViewDiscipline = itemView.findViewById(R.id.textViewDiscipline);
            textViewCardCount = itemView.findViewById(R.id.textViewCardCount);
            viewColorIndicator = itemView.findViewById(R.id.viewColorIndicator);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteDeck);

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

        public void bind(Deck deck) {
            textViewDeckName.setText(deck.getName());
            textViewDiscipline.setText(deck.getDiscipline());

            String cardCountText = deck.getCard_count() + " cartas • Creado " + formatDate(deck.getCreated_at());
            textViewCardCount.setText(cardCountText);

            // Set color indicator
            try {
                int color = Color.parseColor(deck.getChosen_color());
                viewColorIndicator.setBackgroundColor(color);
            } catch (Exception e) {
                viewColorIndicator.setBackgroundColor(Color.parseColor("#31628D"));
            }
        }

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
    }
}
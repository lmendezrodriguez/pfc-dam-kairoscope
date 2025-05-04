from django.db import models


class UserProfile(models.Model):
    """
    Represents an authenticated user via Firebase in our database.
    Links Firebase UID to our application data.
    """
    firebase_uid = models.CharField(max_length=128, unique=True, db_index=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"UserProfile: {self.firebase_uid}"


class Deck(models.Model):
    """
    Represents a generated strategy deck.
    """
    user = models.ForeignKey(UserProfile, on_delete=models.CASCADE,
                             related_name='decks')
    name = models.CharField(max_length=100)
    discipline = models.CharField(max_length=50)
    block_description = models.CharField(max_length=255)
    chosen_color = models.CharField(max_length=7)  # Assuming hex color format
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        """
        Meta class for Deck model. Makes sure that each user can only have
        one deck with the same name. Ordering the decks by creation date.
        """
        unique_together = ('user', 'name')
        ordering = ['-created_at']

    def __str__(self):
        return f"Deck: {self.name} - User: {self.user.firebase_uid}"


class Card(models.Model):
    """
    Represents an individual card (strategy) within a deck.
    """
    deck = models.ForeignKey(Deck, on_delete=models.CASCADE,
                             related_name='cards')
    text = models.TextField()

    def __str__(self):
        return f"Card: {self.text[:50]}... - Deck: {self.deck.name}"
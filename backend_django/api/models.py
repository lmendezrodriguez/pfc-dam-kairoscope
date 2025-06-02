"""
Modelos de datos para la aplicación Kairoscope.
Define la estructura de usuarios, barajas de estrategias y cartas individuales.
"""
from django.db import models


class UserProfile(models.Model):
    """
    Representa un usuario autenticado vía Firebase en nuestra base de datos.
    Vincula el UID de Firebase con los datos de la aplicación.
    """
    # UID único de Firebase, indexado para búsquedas rápidas
    firebase_uid = models.CharField(max_length=128, unique=True, db_index=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"UserProfile: {self.firebase_uid}"


class Deck(models.Model):
    """
    Representa una baraja de estrategias generada por IA.
    Contiene los parámetros de generación y metadatos de la baraja.
    """
    # Relación con el usuario propietario, eliminación en cascada
    user = models.ForeignKey(UserProfile, on_delete=models.CASCADE,
                             related_name='decks')
    name = models.CharField(max_length=100)
    discipline = models.CharField(max_length=50)
    block_description = models.CharField(max_length=255)
    chosen_color = models.CharField(max_length=7)  # Formato hexadecimal (#RRGGBB)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        """
        Configuración del modelo Deck.
        Garantiza nombres únicos por usuario y ordena por fecha de creación.
        """
        unique_together = ('user', 'name')
        ordering = ['-created_at']

    def __str__(self):
        return f"Deck: {self.name} - User: {self.user.firebase_uid}"


class Card(models.Model):
    """
    Representa una carta individual (estrategia) dentro de una baraja.
    Contiene el texto de la estrategia oblicua generada.
    """
    # Relación con la baraja padre, eliminación en cascada
    deck = models.ForeignKey(Deck, on_delete=models.CASCADE,
                             related_name='cards')
    text = models.TextField()

    def __str__(self):
        return f"Card: {self.text[:50]}... - Deck: {self.deck.name}"
from django.db import models


class UserProfile(models.Model):
   """
   Representa un usuario autenticado vía Firebase en nuestra base de datos.
   Vincula el UID de Firebase con los datos de nuestra aplicación.
   """
   firebase_uid = models.CharField(max_length=128, unique=True, db_index=True)
   created_at = models.DateTimeField(auto_now_add=True)

   def __str__(self):
       return f"UserProfile: {self.firebase_uid}"


class Deck(models.Model):
   """
   Representa una baraja de estrategias generada.
   """
   user = models.ForeignKey(UserProfile, on_delete=models.CASCADE,
                            related_name='decks')
   name = models.CharField(max_length=100)
   discipline = models.CharField(max_length=50)
   block_description = models.CharField(max_length=255)
   chosen_color = models.CharField(max_length=7)  # Formato color hexadecimal
   created_at = models.DateTimeField(auto_now_add=True)

   class Meta:
       """
       Metaclase del modelo Deck. Asegura que cada usuario solo pueda tener
       una baraja con el mismo nombre. Ordena las barajas por fecha de creación.
       """
       unique_together = ('user', 'name')
       ordering = ['-created_at']

   def __str__(self):
       return f"Deck: {self.name} - User: {self.user.firebase_uid}"


class Card(models.Model):
   """
   Representa una carta individual (estrategia) dentro de una baraja.
   """
   deck = models.ForeignKey(Deck, on_delete=models.CASCADE,
                            related_name='cards')
   text = models.TextField()

   def __str__(self):
       return f"Card: {self.text[:50]}... - Deck: {self.deck.name}"
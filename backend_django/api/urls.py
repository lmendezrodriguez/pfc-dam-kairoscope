from django.urls import path
from . import views

urlpatterns = [
    path('decks/', views.create_deck, name='create_deck'),  # POST
]
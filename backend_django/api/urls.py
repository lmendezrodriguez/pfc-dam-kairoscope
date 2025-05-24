from django.urls import path
from . import views

urlpatterns = [
    path('deck/', views.create_deck, name='create_deck'),  # POST
    path('deck/',views.list_decks, name='list_decks'),  # GET
]
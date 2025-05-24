from django.urls import path
from . import views

urlpatterns = [
    path('deck/', views.create_deck, name='create_deck'),  # POST
]
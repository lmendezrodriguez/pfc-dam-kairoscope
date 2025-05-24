from django.urls import path
from . import views

urlpatterns = [
    path('deck/', views.deck_handler, name='deck_handler'),
]
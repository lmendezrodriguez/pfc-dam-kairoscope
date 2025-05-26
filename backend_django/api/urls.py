from django.urls import path
from . import views

urlpatterns = [
    path('deck/', views.deck_handler, name='deck_handler'),
    path('deck/<int:deck_id>/', views.deck_detail_handler, name='deck_detail_handler')
]
"""
Configuración de URLs para la API de Kairoscope.
Define los endpoints para gestión de barajas de estrategias.
"""
from django.urls import path
from . import views

urlpatterns = [
    # Endpoint para listar (GET) y crear (POST) barajas
    path('deck/', views.deck_handler, name='deck_handler'),
    # Endpoint para obtener detalles (GET) y eliminar (DELETE) una baraja específica
    path('deck/<int:deck_id>/', views.deck_detail_handler, name='deck_detail_handler')
]
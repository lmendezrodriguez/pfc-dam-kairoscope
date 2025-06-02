"""
Configuración de URLs para el proyecto Kairoscope.
Define las rutas principales y delega las rutas de la API a la app correspondiente.
"""
from django.contrib import admin
from django.urls import path, include

urlpatterns = [
    path('admin/', admin.site.urls),
    # Incluye todas las URLs de la API bajo el prefijo /api/
    path('api/', include('api.urls')),
]
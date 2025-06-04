"""
Configuración del admin de Django para los modelos UserProfile, Deck y Card.
Proporciona una interfaz administrativa más amigable para la gestión de datos.
"""

from django.contrib import admin
from .models import UserProfile, Deck, Card


@admin.register(UserProfile)
class UserProfileAdmin(admin.ModelAdmin):
    """Configuración del admin para perfiles de usuario vinculados a Firebase."""
    list_display = ('id', 'firebase_uid', 'created_at')
    search_fields = ('firebase_uid',)
    ordering = ('-created_at',)


@admin.register(Deck)
class DeckAdmin(admin.ModelAdmin):
    """Configuración del admin para barajas de estrategias."""
    list_display = (
    'id', 'name', 'user', 'discipline', 'chosen_color', 'created_at')
    list_filter = ('created_at', 'discipline')
    # Permite buscar por nombre de baraja o UID de Firebase del usuario
    search_fields = ('name', 'user__firebase_uid')
    ordering = ('-created_at',)


@admin.register(Card)
class CardAdmin(admin.ModelAdmin):
    """Configuración del admin para cartas individuales de estrategias."""
    list_display = ('id', 'deck', 'get_text_preview')
    search_fields = ('text',)

    def get_text_preview(self, obj):
        """Muestra una vista previa truncada del texto de la carta."""
        return obj.text[:50] + '...' if len(obj.text) > 50 else obj.text

    get_text_preview.short_description = 'Preview'
# Django admin configuration for the UserProfile, Deck, and Card models
# Give the admin interface a more user-friendly representation of the models

from django.contrib import admin
from .models import UserProfile, Deck, Card


@admin.register(UserProfile)
class UserProfileAdmin(admin.ModelAdmin):
    list_display = ('id', 'firebase_uid', 'created_at')
    search_fields = ('firebase_uid',)
    ordering = ('-created_at',)


@admin.register(Deck)
class DeckAdmin(admin.ModelAdmin):
    list_display = (
    'id', 'name', 'user', 'discipline', 'chosen_color', 'created_at')
    list_filter = ('created_at', 'discipline')
    search_fields = ('name', 'user__firebase_uid')
    ordering = ('-created_at',)


@admin.register(Card)
class CardAdmin(admin.ModelAdmin):
    list_display = ('id', 'deck', 'get_text_preview')
    search_fields = ('text',)

    def get_text_preview(self, obj):
        return obj.text[:50] + '...' if len(obj.text) > 50 else obj.text

    get_text_preview.short_description = 'Preview'
# backend_django/api/apps.py
from django.apps import AppConfig


class ApiConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'api'

    def ready(self):
        """Se ejecuta cuando Django inicia la aplicación"""
        # Importamos aquí para evitar problemas de importación circular
        from api.core.rag_processor import RAGProcessor

        try:
            # Intentamos inicializar RAGProcessor al arrancar
            RAGProcessor()
            print("RAGProcessor inicializado al arrancar Django")
        except Exception as e:
            print(f"Error al inicializar RAGProcessor: {e}")
            # No lanzamos excepción para permitir que Django siga iniciando
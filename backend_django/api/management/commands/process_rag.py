"""
Comando de gestión Django para construir y guardar el vector store FAISS.
Procesa la base de conocimiento y crea el índice vectorial para RAG.
"""
import logging
from django.core.management.base import BaseCommand, CommandError
from api.core.rag_processor import RAGProcessor

logger = logging.getLogger('api.core')


class Command(BaseCommand):
    """
    Comando Django para construir el vector store FAISS desde la base de conocimiento.
    Uso: python manage.py process_rag
    """

    help = 'Builds FAISS vector store from JSONL files in knowledge_base directory'

    def handle(self, *args, **options):
        """
        Ejecuta el pipeline completo de procesamiento RAG.
        Maneja errores y proporciona feedback al usuario.
        """
        logger.info("Starting RAG vector store build process")

        try:
            # Crear procesador e iniciar pipeline completo
            processor = RAGProcessor()
            processor.process_and_save_vector_store()

            # Mensaje de éxito visible en consola
            self.stdout.write(
                self.style.SUCCESS(
                    "✓ Vector store built and saved successfully")
            )
            logger.info("RAG management command completed successfully")

        except ValueError as ve:
            # Error de configuración (API keys, rutas, etc.)
            logger.error(f"Configuration error in RAG command: {ve}")
            raise CommandError(f"Configuration error: {ve}")
        except Exception as e:
            # Errores inesperados durante el procesamiento
            logger.error(f"Unexpected error in RAG command: {e}")
            raise CommandError(f"Unexpected error: {e}")
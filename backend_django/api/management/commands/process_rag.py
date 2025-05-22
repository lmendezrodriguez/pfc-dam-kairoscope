import logging
from django.core.management.base import BaseCommand, CommandError
from api.core.rag_processor import RAGProcessor

logger = logging.getLogger('api.core')


class Command(BaseCommand):
    """Django command to build and save FAISS vector store from knowledge base."""

    help = 'Builds FAISS vector store from JSONL files in knowledge_base directory'

    def handle(self, *args, **options):
        logger.info("Starting RAG vector store build process")

        try:
            processor = RAGProcessor()
            processor.process_and_save_vector_store()

            self.stdout.write(
                self.style.SUCCESS(
                    "✓ Vector store built and saved successfully")
            )
            logger.info("RAG management command completed successfully")

        except ValueError as ve:
            logger.error(f"Configuration error in RAG command: {ve}")
            raise CommandError(f"Configuration error: {ve}")
        except Exception as e:
            logger.error(f"Unexpected error in RAG command: {e}")
            raise CommandError(f"Unexpected error: {e}")
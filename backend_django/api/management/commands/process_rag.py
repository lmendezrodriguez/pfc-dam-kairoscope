from django.core.management.base import BaseCommand, CommandError
from django.conf import settings
import os
from pathlib import Path # Import Path

# Import your RAGProcessor class
from api.core.rag_processor import RAGProcessor

class Command(BaseCommand):
    help = 'Runs the RAGProcessor to build and save the FAISS vector store.'

    def handle(self, *args, **options):
        self.stdout.write("Starting RAG Processor management command...")

        # You can optionally pass custom paths, but the default uses settings.BASE_DIR
        # knowledge_base_dir = Path(settings.BASE_DIR) / "api" / "knowledge_base"
        # vector_store_dir = Path(settings.BASE_DIR) / "api" / "vector_store"

        # Ensure the knowledge base directory exists (optional, but good practice)
        # if not knowledge_base_dir.exists():
        #      raise CommandError(f"Knowledge base directory not found: {knowledge_base_dir}")

        # Ensure the vector store directory exists (the processor does this too,
        # but checking here gives a clearer error if there's a path issue)
        # os.makedirs(vector_store_dir, exist_ok=True) # Processor handles this

        try:
            # Instantiate the processor (it uses the default paths from settings)
            # Or pass them explicitly:
            # processor = RAGProcessor(knowledge_base_path=knowledge_base_dir,
            #                          vector_store_path=vector_store_dir)
            processor = RAGProcessor()


            # Run the main processing method
            processor.process_and_save_vector_store()

            self.stdout.write(self.style.SUCCESS("Successfully processed and saved the vector store."))

        except ValueError as ve:
            raise CommandError(f"Configuration or Data Error: {ve}")
        except Exception as e:
            raise CommandError(f"An unexpected error occurred: {e}")
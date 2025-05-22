import os
import sys
import logging
from typing import Optional
from pathlib import Path

from django.apps import AppConfig
from django.conf import settings
from langchain_community.vectorstores import FAISS
from langchain_core.vectorstores import VectorStoreRetriever

from .core.rag_processor import RAGProcessor

logger = logging.getLogger('api.core')


class ApiConfig(AppConfig):
    """Configuración de la app API con carga automática del vector store."""

    default_auto_field = 'django.db.models.BigAutoField'
    name = 'api'

    # Retriever compartido para toda la aplicación
    vector_store_retriever: Optional[VectorStoreRetriever] = None

    def ready(self):
        """Carga el vector store retriever al iniciar la aplicación."""

        # Comandos que no necesitan vector store
        SKIP_COMMANDS = ['makemigrations', 'migrate', 'collectstatic',
                         'test', 'shell', 'dbshell', 'process_rag']

        current_command = sys.argv[1] if len(sys.argv) > 1 else None
        is_runserver = 'runserver' in sys.argv
        is_main_process = os.environ.get('RUN_MAIN') == 'true'

        # Solo cargar en proceso principal de runserver o servidores de producción
        should_skip = (
                current_command in SKIP_COMMANDS or
                (is_runserver and not is_main_process)
        )

        logger.debug(f"Command: {current_command}, runserver: {is_runserver}, "
                     f"main_process: {is_main_process}, skip: {should_skip}")

        if should_skip:
            logger.debug("Skipping vector store loading")
            ApiConfig.vector_store_retriever = None
            return

        self._load_vector_store()

    def _load_vector_store(self):
        """Carga el vector store FAISS y crea el retriever."""
        logger.info("Loading FAISS vector store retriever")

        try:
            vector_store_dir = Path(settings.BASE_DIR) / "api" / "vector_store"

            if not vector_store_dir.exists():
                logger.warning(
                    f"Vector store not found at {vector_store_dir}. "
                    "Run 'python manage.py process_rag' to create it.")
                ApiConfig.vector_store_retriever = None
                return

            # Inicializar embeddings para cargar el vector store
            embeddings_model = RAGProcessor._initialize_embeddings()
            logger.debug("Embeddings model initialized for loading")

            # Cargar vector store FAISS
            vector_store = FAISS.load_local(
                folder_path=vector_store_dir,
                embeddings=embeddings_model,
                allow_dangerous_deserialization=True
            )

            # Crear retriever
            retriever = vector_store.as_retriever(search_kwargs={"k": 8})
            ApiConfig.vector_store_retriever = retriever

            logger.info("FAISS vector store retriever loaded successfully")

        except Exception as e:
            logger.error(f"Failed to load vector store: {e}")
            ApiConfig.vector_store_retriever = None
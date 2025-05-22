import os
import json
import logging
from pathlib import Path
from typing import List, Dict, Any, Optional
from django.conf import settings

from langchain.schema import Document
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from langchain_community.vectorstores import FAISS

logger = logging.getLogger('api.core')
from langchain_community.vectorstores import FAISS # Usar langchain_community para componentes de terceros
from langchain_core.vectorstores import VectorStoreRetriever


class RAGProcessor:
    """Procesa documentos JSONL y crea vector store FAISS para RAG."""

    def __init__(self,
                 knowledge_base_path: Optional[Path] = None,
                 vector_store_path: Optional[Path] = None):

        # Configurar las rutas usando Path para manejo robusto de directorios
        self.knowledge_base_path = knowledge_base_path or Path(settings.BASE_DIR) / "api" / "knowledge_base"
        self.vector_store_path = vector_store_path or Path(settings.BASE_DIR) / "api" / "vector_store"

        # Asegurarse de que el directorio donde se guardará el vector store exista
        os.makedirs(self.vector_store_path, exist_ok=True)
        logger.info(f"Knowledge base: {self.knowledge_base_path}")
        logger.info(f"Vector store: {self.vector_store_path}")

        # Verificar que la API Key de Google esté disponible en el entorno
        api_key = os.getenv('GOOGLE_API_KEY')
        if not api_key:
            logger.error("GOOGLE_API_KEY not found in environment")
            raise ValueError(
                "GOOGLE_API_KEY no encontrada en variables de entorno.")

        # Inicializar el modelo de Embeddings de Google GenAI con LangChain
        # LangChain GoogleGenerativeAIEmbeddings lee GOOGLE_API_KEY automáticamente
        try:
            self.embeddings = GoogleGenerativeAIEmbeddings(
                model="models/text-embedding-004")
            logger.info("Google GenAI embeddings initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize embeddings: {e}")
            raise

    def _load_documents(self) -> List[Document]:
        """Carga documentos desde archivos JSONL."""
        documents = []
        logger.info(f"Loading documents from {self.knowledge_base_path}")

        jsonl_files = list(self.knowledge_base_path.glob("*.jsonl"))
        # Recorrer todos los archivos con extensión .jsonl en el directorio especificado
        for file_path in jsonl_files:
            try:
                with open(file_path, 'r', encoding='utf-8') as file:
                    # Procesar cada línea del archivo JSONL
                    for line_number, line in enumerate(file, 1):
                        line = line.strip()
                        if not line:
                            continue

                        try:
                            data: Dict[str, Any] = json.loads(line)
                            if 'text' in data and data['text'].strip():
                                document = Document(
                                    page_content=data['text'],
                                    metadata={
                                        # Incluir todos los campos del JSON excepto 'text' como metadatos
                                        **{k: v for k, v in data.items() if k != 'text'},
                                        'source_file': str(file_path.name), # Nombre del archivo de origen
                                        'line_number': line_number # Número de línea en el archivo
                                    }
                                )
                                documents.append(document)
                            else:
                                logger.warning(
                                    f"Empty text in {file_path.name}:{line_number}")

                        except json.JSONDecodeError:
                            logger.error(
                                f"Invalid JSON in {file_path.name}:{line_number}")
                        except Exception as e:
                            logger.error(
                                f"Error processing line {line_number} in {file_path.name}: {e}")

            except FileNotFoundError:
                logger.warning(f"File not found: {file_path}")
            except Exception as e:
                logger.error(f"Error reading file {file_path}: {e}")

        logger.info(
            f"Loaded {len(documents)} documents from {len(jsonl_files)} files")
        return documents

    def _split_documents(self, documents: List[Document]) -> List[Document]:
        """Divide documentos en chunks optimizados para RAG."""
        if not documents:
            logger.warning("No documents to split")
            return []

        logger.info(
            f"Splitting {len(documents)} documents (chunk_size=400, overlap=0)")

        # Inicializar el splitter recursivo de caracteres
        # Busca separadores como saltos de línea para dividir texto
        # chunk_size: Tamaño máximo aproximado de cada chunk en unidades de length_function (caracteres)
        # chunk_overlap: Cuántos caracteres se repiten al inicio del siguiente chunk
        # length_function: Cómo medir el tamaño (len para caracteres)
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=400,
            chunk_overlap=0, # Sin solapamiento según recomendación para mejorar IoU
            length_function=len
        )

        chunks = text_splitter.split_documents(documents)
        logger.info(f"Created {len(chunks)} chunks")
        return chunks

    def _create_faiss_index(self, chunks: List[Document]) -> FAISS:
        """Crea índice FAISS con embeddings de Google GenAI."""
        if not chunks:
            logger.error("No chunks available for FAISS index")
            raise ValueError(
                "No hay chunks disponibles para crear el índice FAISS.")

        logger.info(f"Creating FAISS index from {len(chunks)} chunks")

        # Crear el vector store FAISS directamente desde los chunks y el modelo de embeddings.
        # FAISS.from_documents maneja la generación de embeddings para cada chunk internamente.
        try:
            vector_store = FAISS.from_documents(chunks, self.embeddings)
            logger.info("FAISS index created successfully")
            return vector_store
        except Exception as e:
            logger.error(f"Failed to create FAISS index: {e}")
            raise

    def _save_vector_store(self, vector_store: FAISS):
        """Guarda índice FAISS en disco."""
        logger.info(f"Saving FAISS index to: {self.vector_store_path}")
        try:
            # save_local guarda varios archivos (index, docstore, etc.)
            # necesarios para cargar el índice
            vector_store.save_local(self.vector_store_path)
            logger.info("FAISS index saved successfully")
        except Exception as e:
            logger.error(f"Failed to save FAISS index: {e}")
            raise

    def process_and_save_vector_store(self):
        """Pipeline principal: carga → split → embeddings → save."""
        logger.info("Starting RAG vector store processing pipeline")

        try:
            # Paso 1: Cargar documentos
            documents = self._load_documents()
            if not documents:
                logger.warning("No valid documents loaded, stopping process")
                return

            # Paso 2: Dividir documentos en chunks
            chunks = self._split_documents(documents)
            if not chunks:
                logger.warning("No chunks created, stopping process")
                return

            # Paso 3: Crear el índice FAISS
            vector_store = self._create_faiss_index(chunks)

            # Paso 4: Guardar el índice FAISS
            self._save_vector_store(vector_store)

            logger.info("RAG vector store processing completed successfully")

        except ValueError as ve:
            logger.error(f"Configuration/Data error: {ve}")
            raise
        except Exception as e:
            # Capturar cualquier otro error inesperado
            print(f"\nOcurrió un Error Inesperado Durante el Proceso: {e}")
            print("--- Proceso Fallido ---")

    @staticmethod
    def _initialize_embeddings() -> GoogleGenerativeAIEmbeddings:
        """Inicializa y devuelve el modelo de embeddings de Google GenAI."""
        api_key = os.getenv('GOOGLE_API_KEY')
        if not api_key:
            raise ValueError("GOOGLE_API_KEY no encontrada en variables de entorno.")

        try:
            # model="models/text-embedding-004" (o el que uses)
            embeddings = GoogleGenerativeAIEmbeddings(model="models/text-embedding-004")
            print("Embeddings de Google Generative AI (models/text-embedding-004) inicializados.")
            return embeddings
        except Exception as e:
             print(f"Error al inicializar GoogleGenerativeAIEmbeddings: {e}")
             raise # Re-lanzar la excepción

    @classmethod
    def load_faiss_retriever(cls,
                             vector_store_path: Optional[Path] = None
                             ) -> VectorStoreRetriever:
        """
        Carga un índice FAISS guardado y devuelve un retriever de LangChain.

        Args:
            vector_store_path: Ruta al directorio donde se guardó el índice FAISS.
                               Por defecto: settings.BASE_DIR / "api" / "vector_store".

        Returns:
            Un objeto VectorStoreRetriever configurado para usar el índice cargado.

        Raises:
            FileNotFoundError: Si el índice FAISS no se encuentra en la ruta especificada.
            Exception: Si ocurre un error durante la carga o inicialización.
        """
        store_path = vector_store_path or Path(settings.BASE_DIR) / "api" / "vector_store"
        print(f"Intentando cargar el índice FAISS desde: {store_path}")

        # Asegurarse de que el directorio existe antes de intentar cargar
        if not store_path.exists():
             raise FileNotFoundError(f"Directorio del vector store no encontrado: {store_path}")

        try:
            # Inicializar el mismo modelo de embeddings que se usó para guardar
            embeddings_model = cls._initialize_embeddings()

            # Cargar el vector store FAISS
            vector_store = FAISS.load_local(folder_path=store_path,
                                            embeddings=embeddings_model,
                                            allow_dangerous_deserialization=True) # Necesario para index.pkl

            print("Índice FAISS cargado exitosamente.")

            # Devolver el vector store como un retriever
            # Puedes configurar parámetros del retriever aquí si es necesario (ej: search_kwargs={"k": N})
            return vector_store.as_retriever()

        except FileNotFoundError:
             # Relanzar FileNotFoundError específicamente si es el problema
             raise
        except Exception as e:
            print(f"Error al intentar cargar el índice FAISS desde {store_path}: {e}")
            raise # Re-lanzar otras excepciones de carga
            logger.error(f"Unexpected error in RAG processing: {e}")
            raise

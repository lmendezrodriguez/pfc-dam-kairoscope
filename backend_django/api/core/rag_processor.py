import os
import json
import logging
from pathlib import Path
from typing import List, Dict, Any, Optional
from datetime import datetime

from django.conf import settings
from langchain.schema import Document
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from langchain_community.vectorstores import FAISS
from langchain_core.vectorstores import VectorStoreRetriever

logger = logging.getLogger('api.core')


class RAGProcessor:
    """Procesa documentos estructurados (JSONL) y no estructurados (TXT) para RAG unificado."""

    def __init__(self,
                 knowledge_base_path: Optional[Path] = None,
                 vector_store_path: Optional[Path] = None):
        self.knowledge_base_path = knowledge_base_path or Path(
            settings.BASE_DIR) / "api" / "knowledge_base"
        self.vector_store_path = vector_store_path or Path(
            settings.BASE_DIR) / "api" / "vector_store"

        # Subdirectorios para diferentes tipos de contenido
        self.structured_path = self.knowledge_base_path / "structured"
        self.unstructured_path = self.knowledge_base_path / "unstructured"

        os.makedirs(self.vector_store_path, exist_ok=True)
        os.makedirs(self.structured_path, exist_ok=True)
        os.makedirs(self.unstructured_path, exist_ok=True)

        logger.info(f"Knowledge base: {self.knowledge_base_path}")
        logger.info(f"Vector store: {self.vector_store_path}")

        api_key = os.getenv('GOOGLE_API_KEY')
        if not api_key:
            logger.error("GOOGLE_API_KEY not found in environment")
            raise ValueError(
                "GOOGLE_API_KEY no encontrada en variables de entorno.")

        try:
            self.embeddings = GoogleGenerativeAIEmbeddings(
                model="models/text-embedding-004")
            logger.info("Google GenAI embeddings initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize embeddings: {e}")
            raise

    def _load_structured_documents(self) -> List[Document]:
        """Carga documentos estructurados desde archivos JSONL."""
        documents = []
        logger.info(
            f"Loading structured documents from {self.structured_path}")

        jsonl_files = list(self.structured_path.glob("*.jsonl"))
        if not jsonl_files:
            logger.warning(f"No JSONL files found in {self.structured_path}")
            return documents

        for file_path in jsonl_files:
            logger.debug(f"Processing structured file: {file_path.name}")
            try:
                with open(file_path, 'r', encoding='utf-8') as file:
                    for line_number, line in enumerate(file, 1):
                        line = line.strip()
                        if not line:
                            continue

                        try:
                            data: Dict[str, Any] = json.loads(line)
                            if 'text' in data and data['text'].strip():
                                # Enriquecer metadata con información del tipo de fuente
                                metadata = {
                                    **{k: v for k, v in data.items() if
                                       k != 'text'},
                                    'source_type': 'structured',
                                    'source_file': str(file_path.name),
                                    'line_number': line_number,
                                    'char_length': len(data['text']),
                                    'processing_date': datetime.now().isoformat()
                                }

                                document = Document(
                                    page_content=data['text'],
                                    metadata=metadata
                                )
                                documents.append(document)

                        except json.JSONDecodeError:
                            logger.error(
                                f"Invalid JSON in {file_path.name}:{line_number}")
                        except Exception as e:
                            logger.error(
                                f"Error processing line {line_number} in {file_path.name}: {e}")

            except Exception as e:
                logger.error(f"Error reading file {file_path}: {e}")

        logger.info(f"Loaded {len(documents)} structured documents")
        return documents

    def _load_unstructured_documents(self) -> List[Document]:
        """Carga documentos no estructurados desde archivos de texto."""
        documents = []
        logger.info(
            f"Loading unstructured documents from {self.unstructured_path}")

        text_files = list(self.unstructured_path.glob("*.txt"))
        if not text_files:
            logger.warning(f"No TXT files found in {self.unstructured_path}")
            return documents

        for file_path in text_files:
            logger.debug(f"Processing unstructured file: {file_path.name}")
            try:
                with open(file_path, 'r', encoding='utf-8') as file:
                    content = file.read().strip()
                    if content:
                        # Para textos no estructurados, creamos un documento temporal
                        # que luego será dividido en chunks
                        metadata = {
                            'source_type': 'unstructured',
                            'source_file': str(file_path.name),
                            'original_length': len(content),
                            'processing_date': datetime.now().isoformat()
                        }

                        document = Document(
                            page_content=content,
                            metadata=metadata
                        )
                        documents.append(document)

            except Exception as e:
                logger.error(f"Error reading file {file_path}: {e}")

        logger.info(f"Loaded {len(documents)} unstructured documents")
        return documents

    def _process_documents(self, structured_docs: List[Document],
                           unstructured_docs: List[Document]) -> List[
        Document]:
        """Procesa documentos: estructurados sin chunking, no estructurados con chunking semántico."""
        processed_docs = []

        # Documentos estructurados: ya están en el tamaño correcto
        processed_docs.extend(structured_docs)
        logger.info(
            f"Added {len(structured_docs)} structured documents without chunking")

        # Documentos no estructurados: chunking semántico inteligente
        if unstructured_docs:
            # Configuración para chunking semántico
            semantic_splitter = RecursiveCharacterTextSplitter(
                chunk_size=400,  # Tamaño más grande para preservar contexto
                chunk_overlap=0, # Sin superposición para evitar redundancia
                # Overlap moderado para continuidad semántica
                length_function=len,
                # Separadores semánticos priorizados
                separators=[
                    "\n\n\n",  # Secciones principales
                    "\n\n",  # Párrafos
                    "\n",  # Líneas
                    ". ",  # Oraciones
                    "? ",  # Preguntas
                    "! ",  # Exclamaciones
                    "; ",  # Cláusulas
                    ", ",  # Elementos de lista
                    " ",  # Palabras
                    ""  # Caracteres (último recurso)
                ]
            )

            for doc in unstructured_docs:
                chunks = semantic_splitter.split_documents([doc])

                # Post-procesamiento: filtrar chunks muy pequeños y enriquecer metadata
                valid_chunks = []
                for i, chunk in enumerate(chunks):
                    # Filtrar chunks demasiado pequeños (probablemente fragmentos)
                    if len(chunk.page_content.strip()) < 25:
                        logger.debug(
                            f"Skipping tiny chunk ({len(chunk.page_content)} chars)")
                        continue

                    # Enriquecer metadata
                    chunk.metadata.update({
                        'chunk_index': i,
                        'chunk_total': len(chunks),
                        'chunk_position': 'beginning' if i == 0 else 'end' if i == len(
                            chunks) - 1 else 'middle',
                        'chunk_size': len(chunk.page_content)
                    })

                    valid_chunks.append(chunk)

                processed_docs.extend(valid_chunks)
                logger.debug(
                    f"Split {doc.metadata['source_file']} into {len(valid_chunks)} semantic chunks")

        logger.info(f"Total processed documents: {len(processed_docs)}")
        return processed_docs

    def _create_faiss_index(self, documents: List[Document]) -> FAISS:
        """Crea índice FAISS unificado con embeddings de Google GenAI."""
        if not documents:
            logger.error("No documents available for FAISS index")
            raise ValueError(
                "No hay documentos disponibles para crear el índice FAISS.")

        logger.info(
            f"Creating unified FAISS index from {len(documents)} documents")

        try:
            vector_store = FAISS.from_documents(documents, self.embeddings)
            logger.info("Unified FAISS index created successfully")
            return vector_store
        except Exception as e:
            logger.error(f"Failed to create FAISS index: {e}")
            raise

    def _save_vector_store(self, vector_store: FAISS):
        """Guarda índice FAISS unificado en disco."""
        logger.info(f"Saving unified FAISS index to: {self.vector_store_path}")
        try:
            vector_store.save_local(self.vector_store_path)
            logger.info("Unified FAISS index saved successfully")
        except Exception as e:
            logger.error(f"Failed to save FAISS index: {e}")
            raise

    def process_and_save_vector_store(self):
        """Pipeline principal: carga ambos tipos → procesa → embeddings → save."""
        logger.info("Starting unified RAG vector store processing pipeline")

        try:
            # Cargar ambos tipos de documentos
            structured_docs = self._load_structured_documents()
            unstructured_docs = self._load_unstructured_documents()

            if not structured_docs and not unstructured_docs:
                logger.warning(
                    "No documents found in any source, stopping process")
                return

            # Procesar documentos (chunking solo para no estructurados)
            processed_docs = self._process_documents(structured_docs,
                                                     unstructured_docs)

            # Crear índice unificado
            vector_store = self._create_faiss_index(processed_docs)

            # Guardar
            self._save_vector_store(vector_store)

            # Log estadísticas
            structured_count = len([d for d in processed_docs if
                                    d.metadata.get(
                                        'source_type') == 'structured'])
            unstructured_count = len(processed_docs) - structured_count

            logger.info(
                f"Processing completed - Structured: {structured_count}, Unstructured chunks: {unstructured_count}")

        except ValueError as ve:
            logger.error(f"Configuration/Data error: {ve}")
            raise
        except Exception as e:
            logger.error(f"Unexpected error in RAG processing: {e}")
            raise

    @staticmethod
    def _initialize_embeddings() -> GoogleGenerativeAIEmbeddings:
        """Inicializa modelo de embeddings de Google GenAI."""
        api_key = os.getenv('GOOGLE_API_KEY')
        if not api_key:
            logger.error(
                "GOOGLE_API_KEY not found for embeddings initialization")
            raise ValueError(
                "GOOGLE_API_KEY no encontrada en variables de entorno.")

        try:
            embeddings = GoogleGenerativeAIEmbeddings(
                model="models/text-embedding-004")
            logger.debug("Embeddings model initialized for loading")
            return embeddings
        except Exception as e:
            logger.error(f"Failed to initialize embeddings for loading: {e}")
            raise

    @classmethod
    def load_faiss_retriever(cls, vector_store_path: Optional[Path] = None,
                             search_kwargs: Optional[
                                 Dict] = None) -> VectorStoreRetriever:
        """Carga índice FAISS unificado y devuelve retriever configurado."""
        store_path = vector_store_path or Path(
            settings.BASE_DIR) / "api" / "vector_store"
        logger.info(f"Loading unified FAISS index from: {store_path}")

        if not store_path.exists():
            logger.error(f"Vector store directory not found: {store_path}")
            raise FileNotFoundError(
                f"Directorio del vector store no encontrado: {store_path}")

        try:
            embeddings_model = cls._initialize_embeddings()

            vector_store = FAISS.load_local(
                folder_path=store_path,
                embeddings=embeddings_model,
                allow_dangerous_deserialization=True
            )

            # Configuración por defecto para búsqueda híbrida
            default_search_kwargs = {"k": 8}
            if search_kwargs:
                default_search_kwargs.update(search_kwargs)

            logger.info(
                f"FAISS index loaded successfully with search_kwargs: {default_search_kwargs}")
            return vector_store.as_retriever(
                search_kwargs=default_search_kwargs)

        except Exception as e:
            logger.error(f"Failed to load FAISS index from {store_path}: {e}")
            raise

    @staticmethod
    def create_divergence_retriever(vector_store, query, k=5):
        """Busca documentos MENOS similares al query"""
        # Buscar muchos candidatos para tener pool amplio
        docs_with_scores = vector_store.similarity_search_with_score(
            query, k=k * 10
        )
        # Ordenar por score MÁS ALTO (menos similares en embeddings)
        divergent_docs = sorted(docs_with_scores, key=lambda x: x[1],
                                reverse=True)
        # Retornar solo los documentos, sin scores
        return [doc for doc, score in divergent_docs[:k]]

    @staticmethod
    def create_random_retriever(vector_store, k=5):
        """Recupera documentos completamente aleatorios"""
        import random

        # Obtener todos los documentos del vector store
        # Hack: buscar con query vacío y k muy alto
        all_docs = vector_store.similarity_search("",
                                                  k=500)  # Ajusta según tu DB

        # Si tienes menos docs que k, devolver todos
        if len(all_docs) <= k:
            return all_docs

        # Selección aleatoria
        return random.sample(all_docs, k)

    @staticmethod
    def create_mixed_retriever(vector_store, query, k_sim=5, k_div=5,
                               k_random=5):
        """Combina similarity, divergence y random retrievers"""
        mixed_docs = []

        try:
            # 1. Documentos por similitud (método tradicional)
            sim_docs = vector_store.similarity_search(query, k=k_sim)
            mixed_docs.extend(sim_docs)

            # 2. Documentos divergentes
            div_docs = RAGProcessor.create_divergence_retriever(vector_store,
                                                                query, k=k_div)
            mixed_docs.extend(div_docs)

            # 3. Documentos aleatorios
            random_docs = RAGProcessor.create_random_retriever(vector_store,
                                                               k=k_random)
            mixed_docs.extend(random_docs)

            # Eliminar duplicados manteniendo orden
            seen_content = set()
            unique_docs = []
            for doc in mixed_docs:
                if doc.page_content not in seen_content:
                    seen_content.add(doc.page_content)
                    unique_docs.append(doc)

            return unique_docs

        except Exception as e:
            print(f"Error in mixed retriever: {e}")
            # Fallback a similarity simple
            return vector_store.similarity_search(query, k=k_sim)
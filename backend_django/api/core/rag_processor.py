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


class RAGProcessor:
    """Procesa documentos JSONL y crea vector store FAISS para RAG."""

    def __init__(self,
                 knowledge_base_path: Optional[Path] = None,
                 vector_store_path: Optional[Path] = None):
        self.knowledge_base_path = knowledge_base_path or Path(
            settings.BASE_DIR) / "api" / "knowledge_base"
        self.vector_store_path = vector_store_path or Path(
            settings.BASE_DIR) / "api" / "vector_store"

        os.makedirs(self.vector_store_path, exist_ok=True)
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

    def _load_documents(self) -> List[Document]:
        """Carga documentos desde archivos JSONL."""
        documents = []
        logger.info(f"Loading documents from {self.knowledge_base_path}")

        jsonl_files = list(self.knowledge_base_path.glob("*.jsonl"))
        if not jsonl_files:
            logger.warning(
                f"No JSONL files found in {self.knowledge_base_path}")
            return documents

        for file_path in jsonl_files:
            logger.debug(f"Processing file: {file_path.name}")
            try:
                with open(file_path, 'r', encoding='utf-8') as file:
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
                                        **{k: v for k, v in data.items() if
                                           k != 'text'},
                                        'source_file': str(file_path.name),
                                        'line_number': line_number
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

        # Sin overlap para mejor IoU en RAG
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=400,
            chunk_overlap=0,
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
            vector_store.save_local(self.vector_store_path)
            logger.info("FAISS index saved successfully")
        except Exception as e:
            logger.error(f"Failed to save FAISS index: {e}")
            raise

    def process_and_save_vector_store(self):
        """Pipeline principal: carga → split → embeddings → save."""
        logger.info("Starting RAG vector store processing pipeline")

        try:
            documents = self._load_documents()
            if not documents:
                logger.warning("No valid documents loaded, stopping process")
                return

            chunks = self._split_documents(documents)
            if not chunks:
                logger.warning("No chunks created, stopping process")
                return

            vector_store = self._create_faiss_index(chunks)
            self._save_vector_store(vector_store)

            logger.info("RAG vector store processing completed successfully")

        except ValueError as ve:
            logger.error(f"Configuration/Data error: {ve}")
            raise
        except Exception as e:
            logger.error(f"Unexpected error in RAG processing: {e}")
            raise

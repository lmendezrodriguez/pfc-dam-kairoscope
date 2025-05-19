"""
Módulo responsable de gestionar el vector store (FAISS).
"""
import os
from pathlib import Path
from typing import List, Optional

from django.conf import settings
from langchain.embeddings.base import Embeddings
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from langchain.vectorstores import FAISS
from langchain.schema import Document


class VectorStoreManager:
    """Gestiona la creación, carga y guardado del vector store."""

    def __init__(self, api_key: Optional[str] = None,
                 vector_store_path: Optional[str] = None):
        """
        Inicializa el gestor de vector store.

        Args:
            api_key: API key para Google. Si es None, se busca en variables de entorno.
            vector_store_path: Ruta donde se guarda el vector store. Si es None,
                              se usa la ruta por defecto.
        """
        # Verificar API key
        self.api_key = api_key or os.getenv('GOOGLE_API_KEY')
        if not self.api_key:
            raise ValueError("No se encontró GOOGLE_API_KEY")

        # Configurar rutas
        self.vector_store_path = vector_store_path or Path(
            settings.BASE_DIR) / "api" / "vector_store"
        os.makedirs(self.vector_store_path, exist_ok=True)

        # Inicializar embeddings
        self.embeddings = GoogleGenerativeAIEmbeddings(
            google_api_key=api_key,
            model="models/text-embedding-004"
            # Especifica el nombre del modelo
        )

    def _create_embeddings(self) -> Embeddings:
        """
        Crea y configura el modelo de embeddings.

        Returns:
            Modelo de embeddings configurado
        """
        return GooglePalmEmbeddings(google_api_key=self.api_key)

    def load_or_create(self, documents: List[Document]) -> FAISS:
        """
        Carga el vector store desde disco si existe, o lo crea con los documentos proporcionados.

        Args:
            documents: Lista de documentos para crear el vector store si no existe

        Returns:
            Vector store FAISS
        """
        vector_store_file = self.vector_store_path / "faiss_index"

        # Verificar si existe el vector store
        if vector_store_file.exists() and (
        vector_store_file.with_suffix('.faiss')).exists():
            try:
                print("VectorStoreManager: Cargando vector store existente...")
                vector_store = FAISS.load_local(
                    str(vector_store_file),
                    self.embeddings,
                    allow_dangerous_deserialization=True
                )
                print("VectorStoreManager: Vector store cargado correctamente")
                return vector_store
            except Exception as e:
                print(f"VectorStoreManager: Error al cargar vector store: {e}")
                print("VectorStoreManager: Creando un nuevo vector store...")
        else:
            print(
                "VectorStoreManager: No se encontró vector store. Creando uno nuevo...")

        return self.create_and_save(documents, vector_store_file)

    def create_and_save(self, documents: List[Document],
                        vector_store_file: Path) -> FAISS:
        """
        Crea un nuevo vector store y lo guarda en disco.

        Args:
            documents: Lista de documentos para el vector store
            vector_store_file: Ruta donde guardar el vector store

        Returns:
            Vector store FAISS creado
        """
        try:
            if not documents:
                print(
                    "VectorStoreManager: ADVERTENCIA - No hay documentos para crear el vector store")
                documents = [Document(page_content="Documento de respaldo")]

            print(
                f"VectorStoreManager: Creando vector store con {len(documents)} documentos...")
            vector_store = FAISS.from_documents(documents, self.embeddings)

            # Guardamos en disco
            vector_store.save_local(str(vector_store_file))
            print(
                f"VectorStoreManager: Vector store creado y guardado en {vector_store_file}")

            return vector_store
        except Exception as e:
            print(f"VectorStoreManager: Error al crear vector store: {e}")
            # Fallback - vector store mínimo en memoria
            return FAISS.from_documents(
                [Document(page_content="Documento de respaldo para error")],
                self.embeddings
            )

    def rebuild(self, documents: List[Document]) -> FAISS:
        """
        Reconstruye el vector store con los documentos proporcionados.

        Args:
            documents: Lista de documentos para reconstruir el vector store

        Returns:
            Vector store FAISS reconstruido
        """
        vector_store_file = self.vector_store_path / "faiss_index"
        print("VectorStoreManager: Reconstruyendo vector store...")
        return self.create_and_save(documents, vector_store_file)
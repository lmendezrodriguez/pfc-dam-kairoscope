# backend_django/api/core/rag_processor.py
import os
import json
from pathlib import Path
from typing import List, Dict, Any

from django.conf import settings
from langchain.embeddings import \
    GooglePalmEmbeddings  # Puedes usar GoogleGenerativeAIEmbeddings si prefieres
from langchain.vectorstores import FAISS
from langchain.schema import Document


class RAGProcessor:
    """
    Procesador RAG (Retrieval-Augmented Generation) que gestiona la búsqueda
    de contenido relevante en la base de conocimientos para generar estrategias.

    Implementa carga/guardado del vector store en disco para evitar recálculos.
    """

    # Variable de clase para el patrón singleton (opcional)
    _instance = None

    def __new__(cls):
        # Implementación del patrón singleton (opcional)
        if cls._instance is None:
            cls._instance = super(RAGProcessor, cls).__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        # Evitamos inicializar múltiples veces si usamos singleton
        if hasattr(self, '_initialized') and self._initialized:
            return

        print("Inicializando RAGProcessor...")

        # Rutas para la base de conocimientos y el vector store
        self.knowledge_base_path = Path(
            settings.BASE_DIR) / "api" / "knowledge_base"
        self.vector_store_path = Path(
            settings.BASE_DIR) / "api" / "vector_store"

        # Aseguramos que exista la carpeta para el vector store
        os.makedirs(self.vector_store_path, exist_ok=True)

        # Verificamos la API key
        api_key = os.getenv('GOOGLE_API_KEY')
        if not api_key:
            raise ValueError(
                "GOOGLE_API_KEY no encontrada en variables de entorno")

        # Inicializamos el modelo de embeddings
        self.embeddings = GooglePalmEmbeddings(google_api_key=api_key)

        # Cargamos documentos
        self.documents = self._load_documents()

        # Cargamos o creamos el vector store (sin recalcular si ya existe)
        self.vector_store = self._load_or_create_vector_store()

        self._initialized = True
        print("RAGProcessor inicializado correctamente")

    def _load_documents(self) -> List[Document]:
        """
        Carga documentos desde archivos JSONL en la carpeta knowledge_base.
        """
        documents = []

        for file_path in self.knowledge_base_path.glob("*.jsonl"):
            try:
                with open(file_path, 'r', encoding='utf-8') as file:
                    for line_number, line in enumerate(file, 1):
                        if line.strip():
                            try:
                                data = json.loads(line)

                                text = data.get('text', '')
                                metadata = {
                                    'source_file': file_path.name,
                                    'line_number': line_number
                                }

                                # Otros campos como metadatos
                                for key, value in data.items():
                                    if key != 'text':
                                        metadata[key] = value

                                doc = Document(page_content=text,
                                               metadata=metadata)
                                documents.append(doc)

                            except json.JSONDecodeError:
                                print(
                                    f"Error al parsear JSON en {file_path}:{line_number}")
            except Exception as e:
                print(f"Error al leer {file_path}: {e}")

        print(f"Documentos cargados: {len(documents)}")
        return documents

    def _load_or_create_vector_store(self):
        """
        Carga el vector store desde el disco si existe,
        o lo crea y guarda si no existe.
        """
        vector_store_file = self.vector_store_path / "faiss_index"

        # Verificamos si existe el vector store en disco
        if vector_store_file.exists() and (
        vector_store_file.with_suffix('.faiss')).exists():
            try:
                print("Cargando vector store existente...")
                vector_store = FAISS.load_local(
                    str(vector_store_file),
                    self.embeddings,
                    allow_dangerous_deserialization=True
                )
                print("Vector store cargado correctamente desde disco")
                return vector_store
            except Exception as e:
                print(f"Error al cargar vector store desde disco: {e}")
                print("Se creará un nuevo vector store...")
        else:
            print("No se encontró vector store en disco. Creando uno nuevo...")

        # Si llegamos aquí, necesitamos crear un nuevo vector store
        return self._create_and_save_vector_store(vector_store_file)

    def _create_and_save_vector_store(self, vector_store_file):
        """
        Crea un nuevo vector store y lo guarda en disco.
        """
        try:
            if not self.documents:
                print(
                    "ADVERTENCIA: No hay documentos para crear el vector store")
                documents = [Document(page_content="Documento de respaldo")]
            else:
                documents = self.documents

            print(f"Creando vector store con {len(documents)} documentos...")
            vector_store = FAISS.from_documents(documents, self.embeddings)

            # Guardamos el vector store en disco para usos futuros
            vector_store.save_local(str(vector_store_file))
            print(f"Vector store creado y guardado en {vector_store_file}")

            return vector_store
        except Exception as e:
            print(f"Error al crear vector store: {e}")
            # Si todo falla, creamos un vector store mínimo en memoria
            return FAISS.from_documents(
                [Document(page_content="Documento de respaldo para error")],
                self.embeddings
            )

    def rebuild_vector_store(self) -> bool:
        """
        Método público para reconstruir manualmente el vector store.
        Útil durante el desarrollo cuando se modifican los documentos.

        Returns:
            bool: True si se reconstruyó correctamente, False en caso contrario
        """
        try:
            print("Reconstruyendo vector store...")
            vector_store_file = self.vector_store_path / "faiss_index"

            # Recargamos los documentos para capturar cambios
            self.documents = self._load_documents()

            # Creamos nuevo vector store
            self.vector_store = self._create_and_save_vector_store(
                vector_store_file)

            print("Vector store reconstruido exitosamente")
            return True
        except Exception as e:
            print(f"Error al reconstruir vector store: {e}")
            return False

    def search_relevant_content(self, query: str, k: int = 5) -> List[
        Document]:
        """
        Busca los documentos más relevantes para una consulta.

        Args:
            query: El texto de la consulta
            k: Número de documentos a devolver

        Returns:
            Lista de documentos más relevantes
        """
        try:
            # Búsqueda por similitud semántica
            results = self.vector_store.similarity_search(query, k=k)
            return results
        except Exception as e:
            print(f"Error en la búsqueda vectorial: {e}")
            # Fallback a búsqueda más simple
            return self._fallback_search(query, k)

    def _fallback_search(self, query: str, k: int = 5) -> List[Document]:
        """
        Método alternativo de búsqueda basado en palabras clave.
        """
        query_words = query.lower().split()
        scored_docs = []

        for doc in self.documents:
            text = doc.page_content.lower()
            score = sum(1 for word in query_words if word in text)

            if score > 0:
                scored_docs.append((doc, score))

        scored_docs.sort(key=lambda x: x[1], reverse=True)
        return [doc for doc, _ in scored_docs[:k]]

    def search_by_tags(self, tags: List[str], k: int = 5) -> List[Document]:
        """
        Busca documentos por etiquetas específicas.
        """
        matched_docs = []

        for doc in self.documents:
            # Obtenemos las etiquetas del documento
            doc_tags = doc.metadata.get('etiquetas', [])

            # Contamos coincidencias
            matches = len(set(tags) & set(doc_tags)) if isinstance(doc_tags,
                                                                   list) else 0

            if matches > 0:
                matched_docs.append((doc, matches))

        # Ordenamos por número de coincidencias
        matched_docs.sort(key=lambda x: x[1], reverse=True)
        return [doc for doc, _ in matched_docs[:k]]
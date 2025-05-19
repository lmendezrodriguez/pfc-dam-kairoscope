"""
Módulo principal para procesamiento RAG (Retrieval-Augmented Generation).
Integra DocumentLoader y VectorStoreManager para proporcionar funcionalidad
de búsqueda de contenido relevante.
"""
from typing import List, Optional

from langchain.schema import Document

from .document_loader import DocumentLoader
from .vector_store_manager import VectorStoreManager


class RAGProcessor:
    """
    Procesador RAG que proporciona búsqueda de contenido relevante
    usando embeddings y similitud vectorial.

    Implementa el patrón Singleton para asegurar una única instancia.
    """
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(RAGProcessor, cls).__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if getattr(self, '_initialized', False):
            return

        print("RAGProcessor: Inicializando...")

        # Componentes
        self.document_loader = DocumentLoader()
        self.vector_store_manager = VectorStoreManager()

        # Cargar documentos
        self.documents = self.document_loader.load_all_documents()

        # Inicializar vector store
        self.vector_store = self.vector_store_manager.load_or_create(
            self.documents)

        self._initialized = True
        print("RAGProcessor: Inicializado correctamente")

    def search_relevant_content(self, query: str, k: int = 5) -> List[
        Document]:
        """
        Busca documentos relevantes para una consulta mediante similitud vectorial.

        Args:
            query: Texto de la consulta
            k: Número de documentos a devolver

        Returns:
            Lista de documentos relevantes
        """
        try:
            results = self.vector_store.similarity_search(query, k=k)
            return results
        except Exception as e:
            print(f"RAGProcessor: Error en la búsqueda vectorial: {e}")
            return self._fallback_search(query, k)

    def _fallback_search(self, query: str, k: int = 5) -> List[Document]:
        """
        Búsqueda alternativa basada en palabras clave cuando falla la búsqueda vectorial.

        Args:
            query: Texto de la consulta
            k: Número de documentos a devolver

        Returns:
            Lista de documentos relevantes
        """
        query_words = query.lower().split()
        scored_docs = []

        for doc in self.documents:
            text = doc.page_content.lower()
            # Puntuación simple: palabras de la consulta presentes en el texto
            score = sum(1 for word in query_words if word in text)

            if score > 0:
                scored_docs.append((doc, score))

        # Ordenar por puntuación (mayor primero)
        scored_docs.sort(key=lambda x: x[1], reverse=True)
        return [doc for doc, _ in scored_docs[:k]]

    def search_by_tags(self, tags: List[str], k: int = 5) -> List[Document]:
        """
        Busca documentos que coincidan con ciertas etiquetas.

        Args:
            tags: Lista de etiquetas a buscar
            k: Número máximo de documentos a devolver

        Returns:
            Lista de documentos que coinciden con las etiquetas
        """
        matched_docs = []

        for doc in self.documents:
            doc_tags = doc.metadata.get('etiquetas', [])
            # Contar coincidencias entre las etiquetas buscadas y las del documento
            matches = len(set(tags) & set(doc_tags)) if isinstance(doc_tags,
                                                                   list) else 0

            if matches > 0:
                matched_docs.append((doc, matches))

        # Ordenar por número de coincidencias
        matched_docs.sort(key=lambda x: x[1], reverse=True)
        return [doc for doc, _ in matched_docs[:k]]

    def rebuild_vector_store(self) -> bool:
        """
        Reconstruye el vector store con los documentos actuales.
        Útil cuando se han añadido nuevos documentos.

        Returns:
            True si se reconstruyó correctamente, False en caso contrario
        """
        try:
            # Recargar documentos para capturar cambios
            self.documents = self.document_loader.load_all_documents()

            # Reconstruir vector store
            self.vector_store = self.vector_store_manager.rebuild(
                self.documents)

            print("RAGProcessor: Vector store reconstruido exitosamente")
            return True
        except Exception as e:
            print(f"RAGProcessor: Error al reconstruir vector store: {e}")
            return False
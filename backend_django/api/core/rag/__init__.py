"""
Paquete que contiene componentes para Retrieval-Augmented Generation (RAG).
"""
from .document_loader import DocumentLoader
from .vector_store_manager import VectorStoreManager
from .rag_processor import RAGProcessor

# Exportar clases principales para facilitar importaciones
__all__ = ['DocumentLoader', 'VectorStoreManager', 'RAGProcessor']
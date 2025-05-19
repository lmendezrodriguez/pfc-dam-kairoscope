"""
Paquete core que contiene la lógica principal de la aplicación.
"""
# Importación directa para compatibilidad con código existente
from .deck.deck_builder import DeckBuilder
from .rag.rag_processor import RAGProcessor

# Exportar clases principales para facilitar importaciones
__all__ = ['DeckBuilder', 'RAGProcessor']
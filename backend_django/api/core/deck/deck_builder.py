"""
Módulo principal que orquesta el proceso de generación de barajas de estrategias.
"""
from typing import Dict, List, Any, Optional

from langchain.schema import Document

from ..rag.rag_processor import RAGProcessor
from .strategy_generator import StrategyGenerator
from .deck_namer import DeckNamer

class DeckBuilder:
    """
    Orquestador para el proceso de generación de barajas de estrategias.
    Integra RAGProcessor, StrategyGenerator y DeckNamer.
    """

    def __init__(self):
        """Inicializa el builder con sus componentes."""
        self.rag_processor = RAGProcessor()
        self.strategy_generator = StrategyGenerator()
        self.deck_namer = DeckNamer()

    def build_deck(self, discipline: str, block_description: str,
                  color: str, num_cards: int = 123) -> Dict[str, Any]:
        """
        Construye una baraja completa de estrategias oblicuas.

        Args:
            discipline: La disciplina o campo creativo
            block_description: Descripción del bloqueo creativo
            color: Color elegido (formato hex)
            num_cards: Número de cartas a generar

        Returns:
            Diccionario con nombre, estrategias y metadatos de la baraja
        """
        # 1. Obtener inspiración del RAGProcessor
        inspiration_docs = self._gather_inspiration(discipline, block_description)

        # 2. Generar estrategias
        strategies = self.strategy_generator.generate_strategies(
            discipline=discipline,
            block_description=block_description,
            color=color,
            inspiration_docs=inspiration_docs,
            num_strategies=num_cards
        )

        # 3. Generar nombre para la baraja
        deck_name = self.deck_namer.generate_name(discipline, color)

        # 4. Construir y devolver el resultado
        return {
            'name': deck_name,
            'strategies': strategies,
            'metadata': {
                'discipline': discipline,
                'block_description': block_description,
                'color': color,
                'generated_with': 'langchain' if self.strategy_generator.use_llm else 'fallback',
                'inspiration_count': len(inspiration_docs)
            }
        }

    def _gather_inspiration(self, discipline: str, block_description: str) -> List[Document]:
        """
        Recopila documentos de inspiración relevantes.

        Args:
            discipline: La disciplina creativa
            block_description: Descripción del bloqueo

        Returns:
            Lista combinada de documentos relevantes
        """
        # Buscar por descripción del bloqueo
        block_docs = self.rag_processor.search_relevant_content(
            block_description, k=8)

        # Buscar por disciplina
        discipline_docs = self.rag_processor.search_relevant_content(
            discipline, k=4)

        # Buscar por etiquetas relacionadas con la creatividad
        creative_docs = self.rag_processor.search_by_tags(
            ["creatividad", "proceso", "bloqueo"], k=3)

        # Combinar todos los documentos, eliminando posibles duplicados
        all_docs = []
        seen_contents = set()

        for doc in block_docs + discipline_docs + creative_docs:
            content = doc.page_content
            if content not in seen_contents:
                all_docs.append(doc)
                seen_contents.add(content)

        return all_docs

    def rebuild_knowledge_base(self) -> bool:
        """
        Reconstruye la base de conocimientos y el vector store.
        Útil después de actualizar archivos en knowledge_base.

        Returns:
            True si se reconstruyó correctamente, False en caso contrario
        """
        try:
            success = self.rag_processor.rebuild_vector_store()
            if success:
                # Refrescar también las estrategias de fallback
                self.strategy_generator = StrategyGenerator()
                return True
            return False
        except Exception as e:
            print(f"DeckBuilder: Error al reconstruir la base de conocimientos: {e}")
            return False
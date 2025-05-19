"""
Módulo responsable de cargar documentos desde archivos JSONL.
"""
import json
from pathlib import Path
from typing import List, Dict, Any

from django.conf import settings
from langchain.schema import Document


class DocumentLoader:
    """Carga documentos desde archivos JSONL en la knowledge base."""

    def __init__(self, knowledge_base_path=None):
        """
        Inicializa el cargador de documentos.

        Args:
            knowledge_base_path: Ruta a la carpeta knowledge_base. Si es None,
                                se usa la ruta por defecto.
        """
        self.knowledge_base_path = knowledge_base_path or Path(
            settings.BASE_DIR) / "api" / "knowledge_base"

    def load_all_documents(self) -> List[Document]:
        """
        Carga todos los documentos de la knowledge base.

        Returns:
            Lista de documentos LangChain
        """
        documents = []

        # Buscar todos los archivos JSONL en la carpeta
        for file_path in self.knowledge_base_path.glob("*.jsonl"):
            file_docs = self.load_documents_from_file(file_path)
            documents.extend(file_docs)

        print(f"DocumentLoader: Cargados {len(documents)} documentos en total")
        return documents

    def load_documents_from_file(self, file_path: Path) -> List[Document]:
        """
        Carga documentos desde un archivo JSONL específico.

        Args:
            file_path: Ruta al archivo JSONL

        Returns:
            Lista de documentos LangChain del archivo
        """
        documents = []

        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                for line_number, line in enumerate(file, 1):
                    if line.strip():
                        try:
                            data = json.loads(line)

                            # Extraer el texto principal
                            text = data.get('text', '')

                            # Preparar metadatos
                            metadata = {
                                'source_file': file_path.name,
                                'line_number': line_number
                            }

                            # Añadir otros campos como metadatos
                            for key, value in data.items():
                                if key != 'text':
                                    metadata[key] = value

                            # Crear documento LangChain
                            doc = Document(page_content=text,
                                           metadata=metadata)
                            documents.append(doc)

                        except json.JSONDecodeError:
                            print(
                                f"Error al parsear JSON en {file_path}:{line_number}")

        except Exception as e:
            print(f"Error al leer {file_path}: {e}")

        print(
            f"DocumentLoader: Cargados {len(documents)} documentos de {file_path.name}")
        return documents

    def load_documents_by_tag(self, tag: str, max_docs: int = 50) -> List[
        Document]:
        """
        Carga documentos que contienen una etiqueta específica.

        Args:
            tag: Etiqueta a buscar
            max_docs: Número máximo de documentos a devolver

        Returns:
            Lista de documentos que contienen la etiqueta
        """
        all_docs = self.load_all_documents()

        # Filtrar documentos por etiqueta
        matching_docs = []
        for doc in all_docs:
            tags = doc.metadata.get('etiquetas', [])
            if isinstance(tags, list) and tag in tags:
                matching_docs.append(doc)

            if len(matching_docs) >= max_docs:
                break

        return matching_docs
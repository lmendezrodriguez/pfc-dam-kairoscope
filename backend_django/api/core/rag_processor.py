import os
import json
from pathlib import Path
from typing import List, Dict, Any
from django.conf import settings
import google.generativeai as genai
import numpy as np


class RAGProcessor:
    def __init__(self):
        self.knowledge_base_path = Path(
            settings.BASE_DIR) / "api" / "knowledge_base"

        # Configurar Google Generative AI
        api_key = os.getenv('GOOGLE_API_KEY')
        if not api_key:
            raise ValueError(
                "GOOGLE_API_KEY no encontrada en variables de entorno")

        genai.configure(api_key=api_key)

        # Cargar documentos
        self.documents = self._load_documents()
        self.document_embeddings = None

    def _load_documents(self) -> List[Dict[str, Any]]:
        """Carga documentos desde archivos JSONL"""
        documents = []

        # Procesar archivos JSONL
        for file_path in self.knowledge_base_path.glob("*.jsonl"):
            try:
                with open(file_path, 'r', encoding='utf-8') as file:
                    for line_number, line in enumerate(file, 1):
                        if line.strip():
                            try:
                                data = json.loads(line)

                                # Añadir metadatos de origen
                                data['source_file'] = str(file_path.name)
                                data['line_number'] = line_number

                                documents.append(data)

                            except json.JSONDecodeError:
                                print(
                                    f"Error parsing JSON en {file_path}:{line_number}")
            except Exception as e:
                print(f"Error leyendo {file_path}: {e}")

        print(f"Documentos cargados: {len(documents)}")
        return documents

    def _precompute_embeddings(self):
        """Precomputa embeddings para todos los documentos"""
        if self.document_embeddings is not None:
            return

        print("Computando embeddings para documentos...")
        self.document_embeddings = []

        for i, doc in enumerate(self.documents):
            try:
                text = doc.get('text', '')
                result = genai.embed_content(
                    model="models/embedding-001",
                    content=text,
                    task_type="retrieval_document"
                )
                self.document_embeddings.append(result['embedding'])

                if (i + 1) % 10 == 0:
                    print(
                        f"Procesados {i + 1}/{len(self.documents)} documentos")

            except Exception as e:
                print(f"Error procesando documento {i}: {e}")
                # Usar embedding vacío en caso de error
                self.document_embeddings.append([0] * 768)  # Dimensión típica

        print("Embeddings computados completamente")

    def search_relevant_content(self, query: str, k: int = 5) -> List[
        Dict[str, Any]]:
        """Busca contenido relevante usando embeddings"""

        # Asegurar que los embeddings estén precomputados
        if self.document_embeddings is None:
            self._precompute_embeddings()

        # Generar embedding para la query
        try:
            result = genai.embed_content(
                model="models/embedding-001",
                content=query,
                task_type="retrieval_query"
            )
            query_embedding = result['embedding']
        except Exception as e:
            print(f"Error generando embedding para query: {e}")
            return self._fallback_search(query, k)

        # Calcular similitudes
        similarities = []
        for i, doc_embedding in enumerate(self.document_embeddings):
            # Similitud coseno
            dot_product = np.dot(query_embedding, doc_embedding)
            norm_query = np.linalg.norm(query_embedding)
            norm_doc = np.linalg.norm(doc_embedding)

            if norm_query != 0 and norm_doc != 0:
                similarity = dot_product / (norm_query * norm_doc)
            else:
                similarity = 0

            similarities.append((i, similarity))

        # Ordenar por similitud
        similarities.sort(key=lambda x: x[1], reverse=True)

        # Crear resultados
        results = []
        for idx, similarity in similarities[:k]:
            doc = self.documents[idx]
            results.append({
                'text': doc.get('text', ''),
                'metadata': {k: v for k, v in doc.items() if k != 'text'},
                'similarity': similarity
            })

        return results

    def _fallback_search(self, query: str, k: int = 5) -> List[Dict[str, Any]]:
        """Búsqueda simple basada en palabras clave (fallback)"""
        query_words = query.lower().split()

        results = []
        for doc in self.documents:
            text = doc.get('text', '').lower()
            score = sum(1 for word in query_words if word in text)

            if score > 0:
                results.append({
                    'text': doc.get('text', ''),
                    'metadata': {k: v for k, v in doc.items() if k != 'text'},
                    'score': score
                })

        # Ordenar por relevancia
        results.sort(key=lambda x: x['score'], reverse=True)
        return results[:k]

    def search_by_tags(self, tags: List[str], k: int = 5) -> List[
        Dict[str, Any]]:
        """Busca específicamente por etiquetas"""
        results = []

        for doc in self.documents:
            doc_tags = doc.get('etiquetas', [])
            matches = len(set(tags) & set(doc_tags))

            if matches > 0:
                results.append({
                    'text': doc.get('text', ''),
                    'metadata': {k: v for k, v in doc.items() if k != 'text'},
                    'tag_matches': matches
                })

        # Ordenar por número de coincidencias
        results.sort(key=lambda x: x['tag_matches'], reverse=True)
        return results[:k]

    def search_by_tone(self, desired_tone: str, k: int = 5) -> List[
        Dict[str, Any]]:
        """Busca por tono específico"""
        results = []

        for doc in self.documents:
            doc_tone = doc.get('tono', '').lower()

            if desired_tone.lower() in doc_tone:
                results.append({
                    'text': doc.get('text', ''),
                    'metadata': {k: v for k, v in doc.items() if k != 'text'}
                })

        return results[:k]
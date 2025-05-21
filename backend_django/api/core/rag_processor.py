import os
import json
from pathlib import Path
from typing import List, Dict, Any, Optional
from django.conf import settings

from langchain.schema import Document
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from langchain_community.vectorstores import FAISS # Usar langchain_community para componentes de terceros


class RAGProcessor:
    """
    Procesa documentos desde archivos JSONL en una base de conocimiento,
    crea embeddings usando Google GenAI y guarda el índice FAISS
    persistentemente para su uso posterior en recuperación (RAG).

    Esta clase se enfoca únicamente en el proceso de ingestión y guardado,
    no en la consulta del vector store.
    """

    def __init__(self,
                 knowledge_base_path: Optional[Path] = None,
                 vector_store_path: Optional[Path] = None):
        """
        Inicializa el procesador RAG.

        Configura las rutas de la base de conocimiento y el vector store,
        y prepara el modelo de embeddings de Google GenAI.

        Args:
            knowledge_base_path: Ruta al directorio que contiene los archivos JSONL.
                                 Por defecto: settings.BASE_DIR / "api" / "knowledge_base".
            vector_store_path: Ruta al directorio donde se guardará el índice FAISS.
                               Por defecto: settings.BASE_DIR / "api" / "vector_store".

        Raises:
            ValueError: Si la variable de entorno 'GOOGLE_API_KEY' no está configurada.
            Exception: Si ocurre un error durante la inicialización de embeddings.
        """
        # Configurar las rutas usando Path para manejo robusto de directorios
        self.knowledge_base_path = knowledge_base_path or Path(settings.BASE_DIR) / "api" / "knowledge_base"
        self.vector_store_path = vector_store_path or Path(settings.BASE_DIR) / "api" / "vector_store"

        # Asegurarse de que el directorio donde se guardará el vector store exista
        os.makedirs(self.vector_store_path, exist_ok=True)
        print(f"Directorio de la base de conocimiento: {self.knowledge_base_path}")
        print(f"Directorio de guardado del vector store: {self.vector_store_path}")

        # Verificar que la API Key de Google esté disponible en el entorno
        api_key = os.getenv('GOOGLE_API_KEY')
        if not api_key:
            raise ValueError("GOOGLE_API_KEY no encontrada en variables de entorno.")

        # Inicializar el modelo de Embeddings de Google GenAI con LangChain
        # LangChain GoogleGenerativeAIEmbeddings lee GOOGLE_API_KEY automáticamente
        try:
            self.embeddings = GoogleGenerativeAIEmbeddings(model="models/text-embedding-004")
            print("Embeddings de Google Generative AI (models/text-embedding-004) inicializados.")
        except Exception as e:
             print(f"Error al inicializar GoogleGenerativeAIEmbeddings: {e}")
             raise # Re-lanzar la excepción si la inicialización falla


    def _load_documents(self) -> List[Document]:
        """
        Carga los documentos de origen desde archivos JSONL.
        Cada línea en un archivo JSONL se trata como un documento.
        Crea objetos Document de LangChain con el texto y metadatos.
        """
        documents = []
        print(f"Cargando documentos desde {self.knowledge_base_path}...")

        # Recorrer todos los archivos con extensión .jsonl en el directorio especificado
        for file_path in self.knowledge_base_path.glob("*.jsonl"):
            try:
                with open(file_path, 'r', encoding='utf-8') as file:
                    # Procesar cada línea del archivo JSONL
                    for line_number, line in enumerate(file, 1):
                        line = line.strip()
                        if not line: # Saltar líneas completamente vacías
                            continue

                        try:
                            data: Dict[str, Any] = json.loads(line)
                            # Asegurarse de que el campo 'text' existe y no está vacío
                            if 'text' in data and data['text'].strip():
                                # Crear un objeto Document para LangChain
                                # page_content: Contenido principal del documento (el texto)
                                # metadata: Información adicional (origen, línea, etiquetas, etc.)
                                document = Document(
                                    page_content=data['text'],
                                    metadata={
                                        # Incluir todos los campos del JSON excepto 'text' como metadatos
                                        **{k: v for k, v in data.items() if k != 'text'},
                                        'source_file': str(file_path.name), # Nombre del archivo de origen
                                        'line_number': line_number # Número de línea en el archivo
                                    }
                                )
                                documents.append(document)
                            else:
                                print(f"Advertencia: La línea {line_number} en {file_path.name} no contiene texto válido ('text' vacío o faltante). Ignorada.")

                        except json.JSONDecodeError:
                            print(f"Error: No se pudo parsear JSON en {file_path.name}:{line_number}. La línea fue ignorada.")
                        except Exception as e:
                            print(f"Error inesperado procesando la línea {line_number} en {file_path.name}: {e}. Línea ignorada.")

            except FileNotFoundError:
                print(f"Advertencia: Archivo no encontrado - {file_path}. Saltando.")
            except Exception as e:
                print(f"Error al leer el archivo {file_path}: {e}. Archivo saltado.")

        print(f"Total de documentos de LangChain cargados y formateados: {len(documents)}")
        return documents

    def _split_documents(self, documents: List[Document]) -> List[Document]:
        """
        Divide la lista de documentos de LangChain en fragmentos (chunks).
        Basado en análisis de chunking, se usa RecursiveCharacterTextSplitter
        con un tamaño de chunk de 400 caracteres y sin solapamiento.
        """
        if not documents:
            print("No hay documentos para dividir en chunks.")
            return []

        print(f"Dividiendo {len(documents)} documentos en chunks (chunk_size=400, chunk_overlap=0)...")

        # Inicializar el splitter recursivo de caracteres
        # Busca separadores como saltos de línea para dividir texto
        # chunk_size: Tamaño máximo aproximado de cada chunk en unidades de length_function (caracteres)
        # chunk_overlap: Cuántos caracteres se repiten al inicio del siguiente chunk
        # length_function: Cómo medir el tamaño (len para caracteres)
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=400,
            chunk_overlap=0, # Sin solapamiento según recomendación para mejorar IoU
            length_function=len
        )

        # Aplicar el splitter a la lista de documentos
        chunks = text_splitter.split_documents(documents)

        print(f"Total de chunks creados: {len(chunks)}")
        return chunks

    def _create_faiss_index(self, chunks: List[Document]) -> FAISS:
        """
        Crea un índice FAISS (Facebook AI Similarity Search) a partir de los chunks.
        Utiliza los embeddings de Google GenAI para generar las representaciones vectoriales.
        """
        if not chunks:
            raise ValueError("No hay chunks disponibles para crear el índice FAISS.")

        print(f"Creando índice FAISS a partir de {len(chunks)} chunks usando embeddings de Google GenAI...")

        # Crear el vector store FAISS directamente desde los chunks y el modelo de embeddings.
        # FAISS.from_documents maneja la generación de embeddings para cada chunk internamente.
        try:
            vector_store = FAISS.from_documents(chunks, self.embeddings)
            print("Índice FAISS creado exitosamente.")
            return vector_store
        except Exception as e:
            print(f"Error al crear el índice FAISS: {e}")
            raise # Re-lanzar la excepción si falla la creación del índice


    def _save_vector_store(self, vector_store: FAISS):
        """
        Guarda el índice FAISS y su información asociada localmente en el disco.
        Se guarda en el directorio especificado durante la inicialización.
        """
        print(f"Guardando índice FAISS en el directorio: {self.vector_store_path}...")
        try:
            # save_local guarda varios archivos (index, docstore, etc.) necesarios para cargar el índice
            vector_store.save_local(self.vector_store_path)
            print("Índice FAISS guardado exitosamente.")
        except Exception as e:
            print(f"Error al intentar guardar el índice FAISS en {self.vector_store_path}: {e}")
            raise # Re-lanzar la excepción para indicar que el proceso falló


    def process_and_save_vector_store(self):
        """
        Método principal para ejecutar el pipeline de procesamiento:
        1. Carga documentos de origen desde JSONL.
        2. Divide los documentos en chunks.
        3. Crea el índice FAISS usando embeddings.
        4. Guarda el índice FAISS en disco.
        """
        print("\n--- Iniciando Proceso de Construcción y Guardado del Vector Store FAISS ---")
        try:
            # Paso 1: Cargar documentos
            documents = self._load_documents()
            if not documents:
                print("Proceso terminado: No se cargaron documentos válidos para procesar.")
                return

            # Paso 2: Dividir documentos en chunks
            chunks = self._split_documents(documents)
            if not chunks:
                 print("Proceso terminado: No se crearon chunks a partir de los documentos.")
                 return

            # Paso 3: Crear el índice FAISS
            vector_store = self._create_faiss_index(chunks)

            # Paso 4: Guardar el índice FAISS
            self._save_vector_store(vector_store)

            print("\n--- Proceso de Construcción y Guardado del Vector Store Completado Exitosamente ---")

        except ValueError as ve:
            # Errores esperados como falta de API Key o datos
            print(f"\nError de Configuración o Datos: {ve}")
            print("--- Proceso Fallido ---")
        except Exception as e:
            # Capturar cualquier otro error inesperado
            print(f"\nOcurrió un Error Inesperado Durante el Proceso: {e}")
            print("--- Proceso Fallido ---")
# backend_django/api/apps.py
from typing import Optional

from django.apps import AppConfig
from django.conf import settings
import os  # Importar os
from pathlib import Path
import sys

from langchain_community.vectorstores import FAISS

# Importar la clase RAGProcessor que contiene la lógica de carga
from .core.rag_processor import RAGProcessor
# Importar el tipo para el type hinting
from langchain_core.vectorstores import VectorStoreRetriever


class ApiConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'api'

    # Atributo para almacenar el retriever cargado
    vector_store_retriever: Optional[VectorStoreRetriever] = None

    def ready(self):
        """
        Método llamado una vez cuando Django carga la aplicación.
        Aquí cargamos el retriever del vector store, solo en el proceso principal
        del servidor que va a manejar solicitudes.
        """
        # Lista de comandos de manage.py que NO deben cargar el vector store NUNCA
        SKIP_LOAD_COMMANDS = ['makemigrations', 'migrate', 'collectstatic',
                              'test', 'shell', 'dbshell']

        # Verificar si estamos en un comando manage.py que debemos saltar la carga
        # sys.argv[0] es el script (manage.py), sys.argv[1] es el comando
        is_manage_command = len(sys.argv) > 1 and sys.argv[
            1] in SKIP_LOAD_COMMANDS

        # Además, para runserver con reloader, la carga debe ocurrir SOLO en el proceso con RUN_MAIN='true'
        is_main_runserver_process = 'runserver' in sys.argv and os.environ.get(
            'RUN_MAIN') == 'true'

        # Cargar el retriever si NO es un comando de skip Y SI es el proceso principal de runserver
        # O si es un comando que sí requiere la carga (aunque ahora solo runserver_main lo necesita)
        # Simplificando: cargar si es el proceso principal de runserver Y no está en la lista de skips explícitos
        # O cargar si no es un comando de manage.py en absoluto (ej. wsgi/asgi server) - pero runserver es el caso principal de dev

        # La lógica más simple para desarrollo es: cargar SI es el proceso principal de runserver,
        # y saltar SI es cualquier otro comando de manage.py que no sea el principal de runserver.
        # Los servidores de producción como Gunicorn/uWSGI no configuran RUN_MAIN de esta manera,
        # ahí ready() solo se ejecuta una vez por worker, que es el comportamiento deseado.

        # En desarrollo con `runserver`:
        # Proceso Padre (reloader): sys.argv incluye 'runserver', os.environ.get('RUN_MAIN') es None -> SALTA
        # Proceso Hijo (servidor): sys.argv incluye 'runserver', os.environ.get('RUN_MAIN') es 'true' -> NO SALTA -> CARGA
        # Otros comandos: sys.argv incluye el comando, os.environ.get('RUN_MAIN') es None -> SALTA si está en SKIP_LOAD_COMMANDS

        should_skip_load = is_manage_command or (
                    'runserver' in sys.argv and os.environ.get(
                'RUN_MAIN') != 'true')
        print(
            f"DEBUG: is_manage_command={is_manage_command}, 'runserver' in sys.argv={'runserver' in sys.argv}, RUN_MAIN={os.environ.get('RUN_MAIN')}, should_skip_load={should_skip_load}")

        if should_skip_load:
            print("Saltando carga...")
            # Aunque se salte, por consistencia, asigna None al atributo de CLASE
            ApiConfig.vector_store_retriever = None
            return

        # --- Lógica de carga ---
        try:
            print("DEBUG: Procediendo con la carga del vector store...")
            vector_store_dir = Path(settings.BASE_DIR) / "api" / "vector_store"

            if not vector_store_dir.exists():
                print(
                    f"Advertencia: Directorio del vector store no encontrado en {vector_store_dir}. El retriever NO se cargó. La generación de barajas FALLARÁ hasta que se cree el índice (correr manage.py process_rag).")
                # Si no existe, deja el atributo de CLASE como None
                ApiConfig.vector_store_retriever = None
                return

            print(
                f"DEBUG: Directorio {vector_store_dir} encontrado. Intentando cargar...")
            embeddings_model = RAGProcessor._initialize_embeddings()
            print("DEBUG: Embeddings inicializados para la carga.")

            # Cargar el vector store
            vector_store = FAISS.load_local(folder_path=vector_store_dir,
                                            embeddings=embeddings_model,
                                            allow_dangerous_deserialization=True)
            print("DEBUG: FAISS cargado.")

            # Convertir a retriever
            retriever = vector_store.as_retriever()
            print("DEBUG: Retriever creado.")

            # *** EL CAMBIO CLAVE: ASIGNAR AL ATRIBUTO DE CLASE DIRECTAMENTE ***
            ApiConfig.vector_store_retriever = retriever
            print("DEBUG: ApiConfig.vector_store_retriever asignado.")

        except Exception as e:
            print(f"ERROR CRÍTICO durante la carga: {e}", file=sys.stderr)
            # Si hay un error, asigna None al atributo de CLASE
            ApiConfig.vector_store_retriever = None
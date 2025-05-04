# backend_django/api/firebase_config.py
import firebase_admin
from firebase_admin import credentials, auth
import os

# Variable para almacenar si Firebase ya está inicializado
_firebase_initialized = False


def initialize_firebase():
    """Inicializa Firebase Admin SDK si aún no está inicializado"""
    global _firebase_initialized

    if not _firebase_initialized:
        try:
            # Obtener la ruta desde variable de entorno
            cred_path = os.getenv('FIREBASE_CREDENTIALS_PATH')

            if cred_path and os.path.exists(cred_path):
                cred = credentials.Certificate(cred_path)
                firebase_admin.initialize_app(cred)
            else:
                # Fallback al archivo local
                local_path = os.path.join(os.path.dirname(__file__), '..',
                                          'firebase-credentials.json')
                if os.path.exists(local_path):
                    cred = credentials.Certificate(local_path)
                    firebase_admin.initialize_app(cred)
                else:
                    # Último fallback - inicialización sin credenciales (para testing)
                    firebase_admin.initialize_app()

            _firebase_initialized = True
        except ValueError:
            # Firebase ya está inicializado
            _firebase_initialized = True

    return firebase_admin.get_app()


def verify_id_token(id_token):
    """Verifica un ID Token de Firebase"""
    try:
        # Verifica el token
        decoded_token = auth.verify_id_token(id_token)
        # Extrae el UID del usuario
        uid = decoded_token['uid']
        return uid
    except Exception as e:
        # En caso de error, devolvemos None
        print(f"Error verificando token: {e}")
        return None
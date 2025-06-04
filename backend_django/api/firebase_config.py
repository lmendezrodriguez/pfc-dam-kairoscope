"""
Configuración y utilidades para Firebase Admin SDK.
Gestiona la inicialización con múltiples fuentes de credenciales y verificación de tokens.
"""
import os
import logging
import firebase_admin
from firebase_admin import credentials, auth

logger = logging.getLogger('api.core')

# Flag global para evitar múltiples inicializaciones
_firebase_initialized = False


def initialize_firebase():
    """
    Inicializa Firebase Admin SDK con fallbacks de credenciales.
    Utiliza un sistema de prioridades: variable de entorno > archivo local > modo testing.
    """
    global _firebase_initialized

    if _firebase_initialized:
        return firebase_admin.get_app()

    try:
        # Prioridad 1: Ruta especificada en variable de entorno
        cred_path = os.getenv('FIREBASE_CREDENTIALS_PATH')
        if cred_path and os.path.exists(cred_path):
            cred = credentials.Certificate(cred_path)
            firebase_admin.initialize_app(cred)
            logger.info(
                f"Firebase initialized with env credentials: {cred_path}")
        else:
            # Prioridad 2: Archivo de credenciales en directorio local
            local_path = os.path.join(os.path.dirname(__file__), '..',
                                      'firebase-credentials.json')
            if os.path.exists(local_path):
                cred = credentials.Certificate(local_path)
                firebase_admin.initialize_app(cred)
                logger.info("Firebase initialized with local credentials file")
            else:
                # Prioridad 3: Inicialización sin credenciales para testing
                firebase_admin.initialize_app()
                logger.warning(
                    "Firebase initialized without credentials (testing mode)")

        _firebase_initialized = True

    except ValueError as e:
        # Maneja el caso común de app ya inicializada
        if "already exists" in str(e):
            logger.debug("Firebase app already initialized")
            _firebase_initialized = True
        else:
            logger.error(f"Firebase initialization error: {e}")
            raise
    except Exception as e:
        logger.error(f"Unexpected Firebase initialization error: {e}")
        raise

    return firebase_admin.get_app()


def verify_id_token(id_token):
    """
    Verifica Firebase ID Token y retorna UID del usuario.
    Maneja diferentes tipos de errores de token de forma granular.
    """
    if not id_token:
        logger.warning("Empty ID token provided")
        return None

    try:
        # Decodifica y verifica el token con Firebase
        decoded_token = auth.verify_id_token(id_token)
        uid = decoded_token['uid']
        logger.debug(f"Token verified successfully for user: {uid}")
        return uid

    except auth.InvalidIdTokenError:
        logger.warning("Invalid Firebase ID token format")
        return None
    except auth.ExpiredIdTokenError:
        logger.warning("Expired Firebase ID token")
        return None
    except Exception as e:
        logger.error(f"Token verification error: {e}")
        return None
import firebase_admin
from firebase_admin import credentials, auth

# Variable para almacenar si Firebase ya está inicializado
_firebase_initialized = False


def initialize_firebase():
    """Inicializa Firebase Admin SDK si aún no está inicializado"""
    global _firebase_initialized

    if not _firebase_initialized:
        # Por ahora, usamos el modo development (sin credenciales reales)
        # En producción, deberías usar un archivo de credenciales
        try:
            firebase_admin.initialize_app()
            _firebase_initialized = True
        except ValueError:
            # Firebase ya está inicializado
            pass

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
        return None
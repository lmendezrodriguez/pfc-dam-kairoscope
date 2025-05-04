# backend_django/api/views.py
import json
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods
from .firebase_config import initialize_firebase, verify_id_token
from .models import UserProfile

# Inicializar Firebase al cargar el módulo
initialize_firebase()


def get_or_create_user_profile(firebase_uid):
    """Obtiene o crea un UserProfile basado en el firebase_uid"""
    try:
        user_profile = UserProfile.objects.get(firebase_uid=firebase_uid)
    except UserProfile.DoesNotExist:
        # Si no existe, lo creamos
        user_profile = UserProfile.objects.create(firebase_uid=firebase_uid)

    return user_profile


@csrf_exempt
@require_http_methods(["POST"])
def create_deck(request):
    try:
        # Parsear el JSON del body
        data = json.loads(request.body)

        # Obtener el token
        token = data.get('token')

        if not token:
            return JsonResponse({'error': 'Token requerido'}, status=400)

        # Verificar el token con Firebase
        firebase_uid = verify_id_token(token)

        if not firebase_uid:
            return JsonResponse({'error': 'Token inválido'}, status=401)

        # Obtener o crear el UserProfile
        user_profile = get_or_create_user_profile(firebase_uid)

        # Por ahora, devolvemos un JSON hardcodeado (aún no guardamos Deck en BD)
        return JsonResponse({
            'status': 'success',
            'message': 'Deck creado exitosamente',
            'deck': {
                'id': 1,
                'name': 'Deck de prueba',
                'user': firebase_uid,
                'created_at': '2025-04-23T12:00:00Z'
            }
        }, status=201)

    except json.JSONDecodeError:
        return JsonResponse({'error': 'JSON inválido'}, status=400)
    except Exception as e:
        return JsonResponse({'error': str(e)}, status=500)
import json
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods


@csrf_exempt  # Por ahora, deshabilitamos CSRF para facilitar las pruebas
@require_http_methods(["POST"])  # Solo permite POST
def create_deck(request):
    try:
        # Parsear el JSON del body
        data = json.loads(request.body)

        # Verificar que el token existe
        token = data.get('token')

        if not token:
            return JsonResponse({'error': 'Token requerido'}, status=400)

        # Por ahora, simulamos una verificación exitosa
        # En el siguiente paso implementaremos la verificación real
        firebase_uid = 'test-uid-123'

        # Por ahora, devolvemos un JSON hardcodeado
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

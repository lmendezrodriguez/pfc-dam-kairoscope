# backend_django/api/views.py
import json
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods
from .firebase_config import initialize_firebase, verify_id_token
from .models import UserProfile, Deck, Card
from .core.deck_generator import DeckGenerator

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

        # Obtener parámetros de generación
        discipline = data.get('discipline', 'Arte')
        block_description = data.get('blockDescription', 'Bloqueo general')
        chosen_color = data.get('color', '#000000')

        # Generar la baraja usando DeckGenerator
        generator = DeckGenerator()
        generated_deck = generator.generate_deck(
            discipline=discipline,
            block_description=block_description,
            color=chosen_color,
            num_cards=123
        )

        # Obtener o crear el UserProfile
        user_profile = get_or_create_user_profile(firebase_uid)

        # Verificar límite de barajas (8 máximo por usuario)
        if user_profile.decks.count() >= 8:
            return JsonResponse({
                'error': 'Has alcanzado el límite de 8 barajas'
            }, status=400)

        # Crear Deck en la base de datos
        deck = Deck.objects.create(
            user=user_profile,
            name=generated_deck['name'],
            discipline=discipline,
            block_description=block_description,
            chosen_color=chosen_color
        )

        # Crear Cards en la base de datos
        for strategy_text in generated_deck['strategies']:
            Card.objects.create(
                deck=deck,
                text=strategy_text
            )

        # Devolver respuesta en formato correcto
        return JsonResponse({
            'status': 'success',
            'message': 'Deck creado exitosamente',
            'deck': {
                'id': deck.id,
                'name': deck.name,
                'user': firebase_uid,
                'created_at': deck.created_at.isoformat()
            }
        }, status=201)

    except json.JSONDecodeError:
        return JsonResponse({'error': 'JSON inválido'}, status=400)
    except Exception as e:
        return JsonResponse({'error': str(e)}, status=500)
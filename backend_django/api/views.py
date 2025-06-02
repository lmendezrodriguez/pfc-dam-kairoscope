"""
Vistas de la API para gestión de barajas de estrategias oblicuas.
Implementa endpoints RESTful con autenticación Firebase y generación IA.
"""
import json
import logging
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods
from .firebase_config import initialize_firebase, verify_id_token
from .models import UserProfile, Deck, Card
from .core.deck_generator import DeckGenerator
from .apps import ApiConfig

logger = logging.getLogger('api.views')

# Inicializar Firebase al cargar el módulo
initialize_firebase()


def get_or_create_user_profile(firebase_uid):
    """
    Obtiene o crea UserProfile por firebase_uid.
    Patrón Get-or-Create para vincular usuarios Firebase con la BD local.
    """
    try:
        user_profile = UserProfile.objects.get(firebase_uid=firebase_uid)
        logger.debug(f"Found existing user profile: {firebase_uid}")
    except UserProfile.DoesNotExist:
        user_profile = UserProfile.objects.create(firebase_uid=firebase_uid)
        logger.info(f"Created new user profile: {firebase_uid}")

    return user_profile

@csrf_exempt
def deck_handler(request):
    """
    Maneja GET (listar) y POST (crear) para /api/deck/
    Dispatcher que delega según el método HTTP.
    """
    if request.method == 'GET':
        return list_decks(request)
    elif request.method == 'POST':
        return create_deck(request)
    else:
        return JsonResponse({'error': 'Método no permitido'}, status=405)

@csrf_exempt # Deshabilitar CSRF para pruebas locales
def create_deck(request):
    """
    Crea baraja de estrategias personalizadas con autenticación Firebase.
    Proceso: validar token → verificar límites → generar con IA → persistir → responder.
    """
    logger.info("Deck creation request received")

    # Verificar que el vector store RAG esté disponible
    if ApiConfig.vector_store_retriever is None:
        logger.error("Vector store not loaded - service unavailable")
        return JsonResponse(
            {'error': 'Vector store no cargado. El servicio no está listo.'},
            status=503
        )

    try:
        data = json.loads(request.body)
        logger.debug(f"Request data keys: {list(data.keys())}")

        # Autenticación Firebase mediante ID Token
        token = data.get('token')
        if not token:
            logger.warning("Request missing authentication token")
            return JsonResponse({'error': 'Token requerido'}, status=400)

        firebase_uid = verify_id_token(token)
        if not firebase_uid:
            logger.warning("Invalid Firebase token provided")
            return JsonResponse({'error': 'Token inválido'}, status=401)

        logger.info(f"Authenticated user: {firebase_uid}")

        # Extraer parámetros de generación del cliente Android
        discipline = data.get('discipline', 'Arte')
        block_description = data.get('blockDescription', 'Bloqueo general')
        chosen_color = data.get('color', '#000000')

        logger.debug(f"Generation params - discipline: {discipline}, color: {chosen_color}")

        # Verificar límite de barajas por usuario antes de generar
        user_profile = get_or_create_user_profile(firebase_uid)
        current_deck_count = user_profile.decks.count()

        if current_deck_count >= 8:
            logger.warning(f"User {firebase_uid} exceeded deck limit ({current_deck_count}/8)")
            return JsonResponse({
                'error': 'Has alcanzado el límite de 8 barajas'
            }, status=400)

        # Generar baraja usando LLM/RAG con el vector store cargado
        logger.info("Starting deck generation with LLM")
        generator = DeckGenerator(retriever=ApiConfig.vector_store_retriever)
        generated_deck = generator.generate_deck(
            discipline=discipline,
            block_description=block_description,
            color=chosen_color,
            num_cards=123
        )
        logger.info(f"Generated deck: {generated_deck['name']}")

        # Persistir baraja en base de datos
        deck = Deck.objects.create(
            user=user_profile,
            name=generated_deck['name'],
            discipline=discipline,
            block_description=block_description,
            chosen_color=chosen_color
        )

        # Crear cartas en lote, filtrando estrategias vacías
        cards = [
            Card(deck=deck, text=strategy_text)
            for strategy_text in generated_deck['strategies']
            if strategy_text and strategy_text.strip()
        ]
        Card.objects.bulk_create(cards)
        logger.info(f"Created deck {deck.id} with {len(cards)} cards")

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
        logger.error("Invalid JSON in request body")
        return JsonResponse({'error': 'JSON inválido'}, status=400)
    except Exception as e:
        logger.error(f"Unexpected error creating deck: {e}")
        return JsonResponse({'error': str(e)}, status=500)


def list_decks(request):
    """
    Lista las barajas del usuario autenticado.
    Utiliza Authorization header con Bearer token.
    """
    logger.info("Deck list request received")
    logger.debug(f"Headers received: {dict(request.headers)}")

    try:
        # Autenticación Firebase mediante Authorization header
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            logger.warning("Missing or invalid Authorization header")
            return JsonResponse({'error': 'Token requerido'}, status=401)

        token = auth_header.split(' ')[1]
        firebase_uid = verify_id_token(token)
        if not firebase_uid:
            logger.warning("Invalid Firebase token provided")
            return JsonResponse({'error': 'Token inválido'}, status=401)

        logger.info(f"Listing decks for user: {firebase_uid}")

        # Obtener perfil de usuario y sus barajas
        user_profile = get_or_create_user_profile(firebase_uid)
        decks = user_profile.decks.all()

        # Serializar respuesta con datos resumidos de las barajas
        decks_data = [{
            'id': deck.id,
            'name': deck.name,
            'discipline': deck.discipline,
            'chosen_color': deck.chosen_color,
            'created_at': deck.created_at.isoformat(),
            'card_count': deck.cards.count()
        } for deck in decks]

        return JsonResponse({
            'status': 'success',
            'decks': decks_data,
            'total': len(decks_data)
        })

    except Exception as e:
        logger.error(f"Error listing decks: {e}")
        return JsonResponse({'error': str(e)}, status=500)


@csrf_exempt
def deck_detail_handler(request, deck_id):
    """
    Maneja GET (detalle) y DELETE para /api/deck/{deck_id}/
    Dispatcher que delega según el método HTTP.
    """
    if request.method == 'GET':
        return get_deck_detail(request, deck_id)
    elif request.method == 'DELETE':
        return delete_deck(request, deck_id)
    else:
        return JsonResponse({'error': 'Método no permitido'}, status=405)


def get_deck_detail(request, deck_id):
    """
    Obtiene detalles completos de una baraja con sus cartas.
    Incluye todas las estrategias para visualización y uso en la app.
    """
    logger.info(f"Deck detail request for deck_id: {deck_id}")

    try:
        # Autenticación Firebase
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            logger.warning("Missing or invalid Authorization header")
            return JsonResponse({'error': 'Token requerido'}, status=401)

        token = auth_header.split(' ')[1]
        firebase_uid = verify_id_token(token)
        if not firebase_uid:
            logger.warning("Invalid Firebase token provided")
            return JsonResponse({'error': 'Token inválido'}, status=401)

        logger.info(f"Getting deck detail for user: {firebase_uid}")

        # Verificar que el usuario sea propietario de la baraja
        user_profile = get_or_create_user_profile(firebase_uid)
        try:
            deck = user_profile.decks.get(id=deck_id)
        except Deck.DoesNotExist:
            logger.warning(f"Deck {deck_id} not found for user {firebase_uid}")
            return JsonResponse({'error': 'Baraja no encontrada'}, status=404)

        # Cargar todas las cartas de la baraja
        cards = deck.cards.all()

        # Serializar respuesta completa con todas las cartas
        deck_data = {
            'id': deck.id,
            'name': deck.name,
            'discipline': deck.discipline,
            'block_description': deck.block_description,
            'chosen_color': deck.chosen_color,
            'created_at': deck.created_at.isoformat(),
            'card_count': len(cards),
            'cards': [{'id': card.id, 'text': card.text} for card in cards]
        }

        return JsonResponse({
            'status': 'success',
            'deck': deck_data
        })

    except Exception as e:
        logger.error(f"Error getting deck detail: {e}")
        return JsonResponse({'error': str(e)}, status=500)


def delete_deck(request, deck_id):
    """
    Elimina una baraja del usuario autenticado.
    Las cartas se eliminan automáticamente por CASCADE.
    """
    logger.info(f"Deck delete request for deck_id: {deck_id}")

    try:
        # Autenticación Firebase
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            logger.warning("Missing or invalid Authorization header")
            return JsonResponse({'error': 'Token requerido'}, status=401)

        token = auth_header.split(' ')[1]
        firebase_uid = verify_id_token(token)
        if not firebase_uid:
            logger.warning("Invalid Firebase token provided")
            return JsonResponse({'error': 'Token inválido'}, status=401)

        logger.info(f"Deleting deck for user: {firebase_uid}")

        # Verificar propiedad y eliminar
        user_profile = get_or_create_user_profile(firebase_uid)
        try:
            deck = user_profile.decks.get(id=deck_id)
        except Deck.DoesNotExist:
            logger.warning(f"Deck {deck_id} not found for user {firebase_uid}")
            return JsonResponse({'error': 'Baraja no encontrada'}, status=404)

        # Eliminar baraja y sus cartas asociadas
        deck_name = deck.name
        deck.delete()
        logger.info(
            f"Deck '{deck_name}' deleted successfully for user {firebase_uid}")

        return JsonResponse({
            'status': 'success',
            'message': f'Baraja "{deck_name}" eliminada correctamente'
        })

    except Exception as e:
        logger.error(f"Error deleting deck: {e}")
        return JsonResponse({'error': str(e)}, status=500)
# backend_django/api/views.py
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
   """Obtiene o crea UserProfile por firebase_uid."""
   try:
       user_profile = UserProfile.objects.get(firebase_uid=firebase_uid)
       logger.debug(f"Found existing user profile: {firebase_uid}")
   except UserProfile.DoesNotExist:
       user_profile = UserProfile.objects.create(firebase_uid=firebase_uid)
       logger.info(f"Created new user profile: {firebase_uid}")

   return user_profile


@csrf_exempt
@require_http_methods(["POST"])
def create_deck(request):
   """Crea una baraja de estrategias personalizadas."""
   logger.info("Deck creation request received")

   # Asegurarse de que el retriever está cargado antes de proceder
   if ApiConfig.vector_store_retriever is None:
       return JsonResponse(
           {'error': 'Vector store no cargado. El servicio no está listo.'},
           status=503
       )

   try:
       data = json.loads(request.body)
       logger.debug(f"Request data keys: {list(data.keys())}")

       # --- DESARROLLO: Token verification disabled ---
       # TODO: Uncomment for production
       # token = data.get('token')
       # if not token:
       #     logger.warning("Request missing authentication token")
       #     return JsonResponse({'error': 'Token requerido'}, status=400)
       #
       # firebase_uid = verify_id_token(token)
       # if not firebase_uid:
       #     logger.warning("Invalid Firebase token provided")
       #     return JsonResponse({'error': 'Token inválido'}, status=401)

       # Desarrollo: usar UID ficticio
       firebase_uid = "dev_test_user"
       logger.info(f"Development mode - using test user: {firebase_uid}")

       # Extraer parámetros de generación
       discipline = data.get('discipline', 'Arte')
       block_description = data.get('blockDescription', 'Bloqueo general')
       chosen_color = data.get('color', '#000000')

       logger.debug(f"Generation params - discipline: {discipline}, color: {chosen_color}")

       # Verificar límite de barajas
       user_profile = get_or_create_user_profile(firebase_uid)
       current_deck_count = user_profile.decks.count()

       if current_deck_count >= 8:
           logger.warning(f"User {firebase_uid} exceeded deck limit ({current_deck_count}/8)")
           return JsonResponse({
               'error': 'Has alcanzado el límite de 8 barajas'
           }, status=400)

       # Generar baraja usando LLM/RAG
       logger.info("Starting deck generation with LLM")
       generator = DeckGenerator(retriever=ApiConfig.vector_store_retriever)
       generated_deck = generator.generate_deck(
           discipline=discipline,
           block_description=block_description,
           color=chosen_color,
           num_cards=123
       )
       logger.info(f"Generated deck: {generated_deck['name']}")

       # Crear Deck en la base de datos
       deck = Deck.objects.create(
           user=user_profile,
           name=generated_deck['name'],
           discipline=discipline,
           block_description=block_description,
           chosen_color=chosen_color
       )

       # Crear cartas en batch
       cards = [
           Card(deck=deck, text=strategy_text)
           for strategy_text in generated_deck['strategies']
           if strategy_text
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
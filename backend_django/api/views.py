# backend_django/api/views.py
import json
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods
# NO NECESITAS IMPORTAR initialize_firebase y verify_id_token si no los vas a usar.
# Pero los dejo comentados por si acaso quieres volver a habilitarlos.
# from .firebase_config import initialize_firebase, verify_id_token
from .models import UserProfile, Deck, Card
from .core.deck_generator import DeckGenerator
from .apps import ApiConfig

# Si no usas Firebase, no necesitas inicializarlo aquí.
# initialize_firebase()


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
    # Asegurarse de que el retriever está cargado antes de proceder
    if ApiConfig.vector_store_retriever is None:
        # Si la carga falló al inicio, devolvemos un error claro
        return JsonResponse(
            {'error': 'Vector store no cargado. El servicio no está listo.'},
            status=503  # Service Unavailable
        )

    try:
        # Parsear el JSON del body
        data = json.loads(request.body)

        # --- START: DESHABILITAR VERIFICACIÓN DE TOKEN PARA PRODUCCIÓN/TEST ---
        # Estas líneas son las que obtienen y verifican el token.
        # Las comentamos para deshabilitar la autenticación Firebase en este endpoint.

        # # Obtener el token del body de la solicitud
        # token = data.get('token')
        #
        # # Si se requiere token y no se proporciona, retornar error 400
        # if not token:
        #     return JsonResponse({'error': 'Token requerido'}, status=400)
        #
        # # Verificar el token con Firebase Authentication
        # try:
        #     firebase_uid = verify_id_token(token)
        # except Exception as e: # Captura errores específicos de verificación si es posible
        #      print(f"Error verificando token Firebase: {e}")
        #      return JsonResponse({'error': 'Token inválido o error de verificación'}, status=401)
        #
        # # Si la verificación falla (devuelve None o similar), retornar error 401
        # if not firebase_uid:
        #     return JsonResponse({'error': 'Token inválido'}, status=401)

        # Usar un UID ficticio para simular un usuario autenticado.
        # Esta línea reemplaza toda la lógica de verificación de token comentada arriba.
        # Puedes cambiar "fake_production_uid" por cualquier string que desees usar como UID ficticio.
        firebase_uid = "fake_production_uid_by"
        # --- END: DESHABILITAR VERIFICACIÓN DE TOKEN ---


        # Obtener parámetros de generación desde el JSON parseado
        discipline = data.get('discipline', 'Arte')
        block_description = data.get('blockDescription', 'Bloqueo general')
        chosen_color = data.get('color', '#000000')

        # Generar la baraja usando DeckGenerator
        # Asegúrate de que ApiConfig.vector_store_retriever está realmente cargado en apps.py
        generator = DeckGenerator(retriever=ApiConfig.vector_store_retriever)
        generated_deck = generator.generate_deck(
            discipline=discipline,
            block_description=block_description,
            color=chosen_color,
            num_cards=123
        )

        # Obtener o crear el UserProfile asociado al UID (ficticio en este caso)
        user_profile = get_or_create_user_profile(firebase_uid)

        # Verificar límite de barajas para este usuario (el UID ficticio compartirá el límite)
        # Considera si quieres que el UID ficticio tenga un límite o no.
        # Si no quieres límite para el UID ficticio, podrías añadir un if:
        # if firebase_uid != "fake_production_uid_bypass" and user_profile.decks.count() >= 8:
        #     return JsonResponse({'error': 'Has alcanzado el límite de 8 barajas'}, status=400)
        # Tal como está, el UID ficticio TAMBIÉN tendrá un límite de 8 barajas.
        if user_profile.decks.count() >= 8:
             return JsonResponse({
                 'error': 'Has alcanzado el límite de 8 barajas'
             }, status=400)


        # Crear Deck en la base de datos asociado al user_profile (del UID ficticio)
        deck = Deck.objects.create(
            user=user_profile,
            name=generated_deck['name'],
            discipline=discipline,
            block_description=block_description,
            chosen_color=chosen_color
        )

        # Crear Cards en la base de datos
        for strategy_text in generated_deck['strategies']:
            # Filtra posibles strings vacías o nulas si el generador a veces las produce
            if strategy_text:
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
                # Devolvemos el UID que se usó (el ficticio)
                'user': firebase_uid,
                'created_at': deck.created_at.isoformat()
                # 'metadata': generated_deck.get('metadata', {})
                # Incluir metadatos si los hay (descomentar si es necesario)
            }
        }, status=201)

    except json.JSONDecodeError:
        # Error si el body no es un JSON válido
        return JsonResponse({'error': 'JSON inválido en el body de la solicitud'}, status=400)
    except Exception as e:
        # Captura cualquier otro error inesperado durante el proceso
        # Es bueno loggear este error 'e' en un entorno real
        print(f"Error inesperado en create_deck: {e}")
        return JsonResponse({'error': str(e)}, status=500)
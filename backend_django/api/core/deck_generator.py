"""
Generador de barajas de estrategias oblicuas usando LangChain y RAG.
Combina recuperación de documentos con generación de IA para crear estrategias personalizadas.
"""
import os
import logging
import json
from typing import Dict, List, Any

from langchain_openai import ChatOpenAI
from langchain_core.prompts import PromptTemplate
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain.chains.retrieval import create_retrieval_chain
from langchain_core.vectorstores import VectorStoreRetriever

from api.core import RAGProcessor

logger = logging.getLogger('api.core')


class DeckGenerator:
    """
    Generador de barajas de estrategias oblicuas usando LangChain, RAG y OpenAI.
    Utiliza un retriever pre-cargado del vector store para contexto y un LLM para generación.
    """

    def __init__(self, retriever: VectorStoreRetriever):
        """
        Inicializa el generador con un retriever de documentos.
        
        Args:
            retriever: Retriever de LangChain con el índice FAISS cargado.
        """
        if not retriever:
            logger.error("No retriever provided to DeckGenerator")
            raise ValueError(
                "Se requiere un retriever cargado para inicializar DeckGenerator.")
        self.retriever = retriever

        # Configurar modelo LLM con API key desde variables de entorno
        api_key = os.getenv('OPENAI_API_KEY')
        if not api_key:
            logger.error("OPENAI_API_KEY not found in environment")
            raise ValueError(
                "OPENAI_API_KEY no encontrada en variables de entorno.")

        # Inicializar ChatGPT con parámetros optimizados para creatividad
        self.llm = ChatOpenAI(
            model="gpt-4.1",
            temperature=0.85,  # Alta creatividad para estrategias oblicuas
        )

        logger.info("DeckGenerator initialized with OpenAI LLM")

        # Configurar plantillas de prompts y cadenas de procesamiento
        self._setup_prompts()
        self._setup_chains()

    def _setup_prompts(self):
        """
        Configura las plantillas de prompts para generación de estrategias y nombres.
        Define el estilo y estructura de las estrategias oblicuas.
        """
        logger.debug("Setting up prompt templates")

        # Prompt detallado para generación de estrategias oblicuas
        self.strategies_prompt = PromptTemplate.from_template(
            """Eres un generador de estrategias oblicuas, diseñadas para romper bloqueos creativos mediante el pensamiento lateral y las ideas tangenciales, no soluciones directas. Te inspiras en el estilo de Brian Eno y Peter Schmidt.

        Contexto creativo:
        - Disciplina: {discipline} (Tu campo de trabajo actual)
        - Descripción del bloqueo: {block_description} (Cómo percibes el bloqueo)
        - Color del bloqueo: {color} (El código HEX del color que evoca la sensación del bloqueo)

        Información de referencia para inspiración (NO COPIES NUNCA DIRECTAMENTE):
        {context}

        ESTRUCTURA OBLIGATORIA:
        - **1-15 palabras máximo** por estrategia (obligatorio variación de longitud)
        - **Sin explicaciones adicionales**
        - **Economía verbal extrema**: cada palabra cuenta
        - Una acción clara e inmediata

        TIPOS DE ESTRATEGIA (distribuir aleatoriamente):
        **Por Función:**
        - DESTRUCTIVA: Eliminar, cortar, abandonar, destruir
        - TRANSFORMATIVA: Cambiar, invertir, distorsionar, convertir
        - PERCEPTUAL: Nueva perspectiva, contexto, punto de vista
        - LIMITANTE: Restricciones, reglas, limitaciones
        - LIBERADORA: Romper reglas, aceptar errores, libertad

        **Por Sintaxis (variar):**
        1. Comando directo: "Invierte", "Elimina lo obvio"
        2. Pregunta provocadora: "¿Qué estás evitando?", "¿Es demasiado seguro?"
        3. Declaración paradójica: "Los errores son invitaciones"
        4. Metáfora activa: "Piensa como agua", "Conviértete en observador"
        5. Opciones múltiples: "Añade -resta -nada"
        6. Un solo sustantivo: "Silencio"

        **Por Aplicabilidad:**
        - 90% Universal: Aplicable a cualquier disciplina creativa (NO mencionar "{discipline}" directamente)
        - 10% Específica: Sutilmente inspirada por el contexto creativo

        **Intensidad (distribuir):**
        - 30% Suave: Cambios sutiles, reflexivos
        - 40% Moderada: Alteraciones significativas
        - 20% Alta: Transformaciones dramáticas
        - 10% Extrema: Destrucción/reconstrucción total

        **Tonos (variar aleatoriamente):**
        Disruptivo, contemplativo, paradójico, poético, técnico, filosófico, absurdo, directo, metafórico, urgente

        REGLAS DE ORO:
        1. **Interrupción obligatoria**: Debe romper el patrón actual
        2. **Acción inmediata**: Aplicable ahora mismo
        3. **Ambigüedad productiva**: Permite múltiples interpretaciones
        4. **Sin jerga especializada**: Entendible universalmente
        5. **Anti-método**: Contradice enfoques convencionales
        6. **NO repetir conceptos**: Cada estrategia debe ser única
        7. **Evita mencionar la disciplina directamente**: Usa términos generales como "trabajo", "proceso", "creación"
        8. **Nunca copies ejemplos de este prompt como estrategias

        Devuelve tu respuesta como un objeto JSON válido con esta estructura exacta:
        {{"estrategias": ["primera estrategia", "segunda estrategia", ...]}}

        Genera exactamente {num_cards} estrategias en el array."""
        )

        # Prompt para generar nombres evocativos de barajas
        self.name_prompt = PromptTemplate.from_template(
            """Genera un nombre corto en español (máximo 4 palabras) evocativo para una baraja de estrategias creativas oblicuas inspirada por:
            - Disciplina: {discipline}
            - Color: {color}
            - No puedes nombrar directamente la disciplina (ni sinónimos) ni el color.

    Solo responde con el nombre, nada más. No incluyas explicaciones ni comillas."""
        )

    def _setup_chains(self):
        """
        Configura las cadenas de LangChain para procesamiento RAG.
        Conecta el retriever con los prompts y el modelo LLM.
        """
        logger.debug("Setting up LangChain chains")

        # Cadena simple para generar nombres de barajas
        self.name_chain = self.name_prompt | self.llm

        # Cadena que combina documentos recuperados con el prompt de estrategias
        self.strategies_document_chain = create_stuff_documents_chain(
            self.llm, self.strategies_prompt
        )

        # Cadena completa RAG: recuperación + generación
        self.retrieval_chain = create_retrieval_chain(
            self.retriever,
            self.strategies_document_chain
        )

    def generate_deck(self, discipline: str, block_description: str,
                      color: str, num_cards: int = 123) -> Dict[str, Any]:
        """
        Genera una baraja completa de estrategias oblicuas.
        Combina generación de estrategias con naming automático.
        """
        logger.info(
            f"Starting deck generation: {discipline}, {num_cards} cards")

        try:
            # Generar estrategias usando el pipeline RAG
            strategies = self._generate_with_langchain(
                discipline, block_description, color, num_cards
            )
        except Exception as e:
            logger.error(f"Failed to generate strategies with LangChain: {e}")
            raise

        # Generar nombre creativo para la baraja
        try:
            deck_name = self._generate_deck_name(discipline, color)
        except Exception as e:
            logger.error(f"Failed to generate deck name: {e}")
            raise

        logger.info(
            f"Generated deck '{deck_name}' with {len(strategies)} strategies")

        return {
            'name': deck_name,
            'strategies': strategies,
            'metadata': {
                'discipline': discipline,
                'block_description': block_description,
                'color': color,
                'requested_cards': num_cards,
                'generated_with': 'langchain'
            }
        }

    def _generate_with_langchain(self, discipline: str, block_description: str,
                                 color: str, num_cards: int) -> List[str]:
        """
        Genera estrategias usando LangChain y RAG.
        Recupera documentos relevantes y los usa como contexto para el LLM.
        """

        logger.info(
            f"Generating {num_cards} strategies for discipline='{discipline}', block='{block_description}', color='{color}'")

        try:
            # Construir query para recuperación de documentos relevantes
            retriever_query = f"Estrategias oblicuas para {discipline}: {block_description}"

            # Usar retriever mixto para obtener diversidad de documentos
            logger.debug(f"Retriever query: {retriever_query}")
            mixed_docs = RAGProcessor.create_mixed_retriever(
                self.retriever.vectorstore,
                retriever_query,
                k_sim=10, k_div=15, k_random=10  # Mezcla de similitud, diversidad y aleatoriedad
            )
            logger.debug(
                f"Retrieved {len(mixed_docs)} mixed documents (sim+div+random)")

            # Log de documentos recuperados para debugging
            for i, doc in enumerate(mixed_docs):
                logger.debug(f"Doc {i + 1}: {doc.page_content[:100]}...")

            # Preparar input para la cadena RAG
            chain_input = {
                "input": retriever_query,
                "discipline": discipline,
                "block_description": block_description,
                "color": color,
                "num_cards": num_cards,
            }

            # Ejecutar cadena completa: recuperación + generación
            result = self.retrieval_chain.invoke(chain_input)
            raw_response = result.get("answer", "").strip()

            # Parsear respuesta JSON del LLM
            try:
                logger.debug(f"Raw LLM response: {raw_response[:200]}...")

                def extract_json_from_response(response: str) -> str:
                    """Extrae JSON limpio de respuesta, manejando markdown."""
                    clean = response.strip()

                    # Manejar bloques de código markdown
                    if clean.startswith('```json') and clean.endswith('```'):
                        clean = clean[7:-3].strip()
                    elif clean.startswith('```') and clean.endswith('```'):
                        clean = clean[3:-3].strip()

                    return clean

                clean_response = extract_json_from_response(raw_response)
                parsed_json = json.loads(clean_response)

                strategies = parsed_json.get("estrategias", [])

                # Validar estructura de respuesta
                if not isinstance(strategies, list):
                    raise ValueError("El campo 'estrategias' no es una lista")

                # Limpiar estrategias vacías
                strategies = [s.strip() for s in strategies if s and s.strip()]
                logger.info(
                    f"Successfully parsed JSON with {len(strategies)} strategies")

            except (json.JSONDecodeError, ValueError) as e:
                logger.warning(
                    f"JSON parsing failed: {e}, falling back to line split")
                logger.debug(f"Failed raw response: {raw_response}")
                # Fallback: dividir por líneas si JSON falla
                strategies = [s.strip() for s in raw_response.split('\n') if
                              s.strip()]

            logger.info(f"Generated {len(strategies)} valid strategies")
            return strategies[:num_cards]

        except Exception as e:
            logger.error(f"Error during LangChain generation: {e}")
            raise

    def _generate_deck_name(self, discipline: str, color: str) -> str:
        """
        Genera un nombre evocativo para la baraja.
        Utiliza la disciplina y color como inspiración.
        """
        logger.debug(
            f"Generating deck name for discipline='{discipline}', color='{color}'")

        try:
            result = self.name_chain.invoke({
                "discipline": discipline,
                "color": color
            })
            # Limpiar nombre generado de comillas o espacios extra
            name = result.content.strip().strip('"')
            logger.debug(f"Generated deck name: '{name}'")
            return name
        except Exception as e:
            logger.error(f"Error generating deck name: {e}")
            raise
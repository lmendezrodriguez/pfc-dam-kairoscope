import os
import logging
from typing import Dict, List, Any

from langchain_core.prompts import PromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain.chains.retrieval import create_retrieval_chain
from langchain_core.vectorstores import VectorStoreRetriever

from api.core import RAGProcessor

logger = logging.getLogger('api.core')


class DeckGenerator:
    """
    Generador de barajas de estrategias oblicuas usando LangChain, RAG y Gemini.
    Depende de un retriever pre-cargado del vector store.
    """

    def __init__(self, retriever: VectorStoreRetriever):
        """
        Inicializa el generador de barajas.

        Args:
            retriever: Un retriever de LangChain cargado con el índice FAISS.
                       Debe ser el retriever del índice creado previamente.
        """
        if not retriever:
            logger.error("No retriever provided to DeckGenerator")
            raise ValueError(
                "Se requiere un retriever cargado para inicializar DeckGenerator.")
        self.retriever = retriever

        # Cargar configuración de la API de Google y el LLM
        api_key = os.getenv('GOOGLE_API_KEY')
        if not api_key:
            logger.error("GOOGLE_API_KEY not found in environment")
            raise ValueError(
                "GOOGLE_API_KEY no encontrada en variables de entorno.")

        # Inicializar el modelo LLM usando LangChain
        self.llm = ChatGoogleGenerativeAI(
            model="gemini-2.5-flash-preview-05-20",
            temperature=0.8,
            top_p=0.9,
            model_kwargs={
                "response_mime_type": "application/json"
            }
        )
        logger.info("DeckGenerator initialized with Gemini LLM")

        # Configurar las plantillas de prompts
        self._setup_prompts()
        # Configurar las cadenas de LangChain
        self._setup_chains()

    def _setup_prompts(self):
        """Configura las plantillas de prompts para la generación."""
        logger.debug("Setting up prompt templates")

        # Plantilla para generar estrategias
        self.strategies_prompt = PromptTemplate.from_template(
            """Eres un generador de estrategias oblicuas, diseñadas para romper bloqueos creativos mediante el pensamiento lateral y las ideas tangenciales, no soluciones directas. Te inspiras en el estilo de Brian Eno y Peter Schmidt.

    Contexto creativo:
    - Disciplina: {discipline} (Tu campo de trabajo actual)
    - Descripción del bloqueo: {block_description} (Cómo percibes el bloqueo)
    - Color del bloqueo: {color} (El código HEX del color que evoca la sensación del bloqueo)

    Información de referencia para inspiración (NO COPIES DIRECTAMENTE):
    {context}

    Basándote en la disciplina, la descripción del bloqueo, el color y la información de referencia como INSPIRACIÓN, genera {num_cards} estrategias oblicuas que:
    1. Sean concisas, evocativas y ambiguas.
    2. Fomenten el pensamiento lateral y la experimentación.
    3. Resuenen conceptualmente con el bloqueo (disciplina, descripción, color).
    4. Mantengan un tono poético, abstracto o filosófico.
    5. NUNCA sean soluciones directas al bloqueo descrito.

    Devuelve tu respuesta como un objeto JSON válido con esta estructura exacta:
    {{"estrategias": ["primera estrategia", "segunda estrategia", ...]}}

    Genera exactamente {num_cards} estrategias en el array."""
        )

        # El name_prompt permanece igual
        self.name_prompt = PromptTemplate.from_template(
            """Genera un nombre corto (máximo 4 palabras), poético y evocativo para una baraja de estrategias creativas oblicuas inspirada por:
    - Disciplina: {discipline}
    - Color: {color}
    - Naturaleza: Lateral, experimental

    Solo responde con el nombre, nada más. No incluyas explicaciones ni comillas."""
        )

    def _setup_chains(self):
        """Configura las cadenas de LangChain para la generación."""
        logger.debug("Setting up LangChain chains")

        # Cadena para generar el nombre de la baraja
        self.name_chain = self.name_prompt | self.llm

        # Cadena para combinar documentos y generar respuesta (estrategias)
        self.strategies_document_chain = create_stuff_documents_chain(
            self.llm, self.strategies_prompt
        )

        # Cadena de recuperación completa (combina el retriever con la cadena de documentos)
        self.retrieval_chain = create_retrieval_chain(
            self.retriever,
            self.strategies_document_chain
        )

    def generate_deck(self, discipline: str, block_description: str,
                      color: str, num_cards: int = 123) -> Dict[str, Any]:
        """Genera una baraja completa de estrategias oblicuas"""
        logger.info(
            f"Starting deck generation: {discipline}, {num_cards} cards")

        try:
            # Generar estrategias usando LangChain
            strategies = self._generate_with_langchain(
                discipline, block_description, color, num_cards
            )
        except Exception as e:
            logger.error(f"Failed to generate strategies with LangChain: {e}")
            # TODO: Implement better fallback strategy generation
            raise

        # Generar nombre de baraja
        try:
            deck_name = self._generate_deck_name(discipline, color)
        except Exception as e:
            logger.error(f"Failed to generate deck name: {e}")
            # TODO: Implement fallback deck naming
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
        """Genera estrategias usando LangChain y RAG"""
        import json
        import logging

        logger = logging.getLogger('api.core')
        logger.info(
            f"Generating {num_cards} strategies for discipline='{discipline}', block='{block_description}', color='{color}'")

        try:
            retriever_query = f"Estrategias oblicuas para {discipline}: {block_description}"

            # Log retriever debugging info
            logger.debug(f"Retriever query: {retriever_query}")
            mixed_docs = RAGProcessor.create_mixed_retriever(
                self.retriever.vectorstore,
                # Acceder al vector store desde el retriever
                retriever_query,
                k_sim=5, k_div=5, k_random=5
            )
            logger.debug(
                f"Retrieved {len(mixed_docs)} mixed documents (sim+div+random)")

            for i, doc in enumerate(mixed_docs):
                logger.debug(f"Doc {i + 1}: {doc.page_content[:100]}...")

            chain_input = {
                "input": retriever_query,
                "discipline": discipline,
                "block_description": block_description,
                "color": color,
                "num_cards": num_cards,
            }

            # Ejecutar la cadena de recuperación y generación
            result = self.retrieval_chain.invoke(chain_input)
            raw_response = result.get("answer", "").strip()

            # NUEVO: Parsear respuesta JSON
            try:
                logger.debug(f"Raw LLM response: {raw_response[:200]}...")

                def extract_json_from_response(response: str) -> str:
                    """Extrae JSON de respuesta, manejando markdown o JSON directo."""
                    clean = response.strip()

                    # Si está en markdown code block
                    if clean.startswith('```json') and clean.endswith('```'):
                        # Extraer contenido entre ```json y ```
                        clean = clean[7:-3].strip()
                    elif clean.startswith('```') and clean.endswith('```'):
                        # Markdown genérico, extraer contenido
                        clean = clean[3:-3].strip()

                    return clean

                clean_response = extract_json_from_response(raw_response)
                parsed_json = json.loads(clean_response)

                strategies = parsed_json.get("estrategias", [])

                # Validar que tenemos una lista de strings
                if not isinstance(strategies, list):
                    raise ValueError("El campo 'estrategias' no es una lista")

                # Limpiar strings vacíos
                strategies = [s.strip() for s in strategies if s and s.strip()]
                logger.info(
                    f"Successfully parsed JSON with {len(strategies)} strategies")

            except (json.JSONDecodeError, ValueError) as e:
                logger.warning(
                    f"JSON parsing failed: {e}, falling back to line split")
                logger.debug(f"Failed raw response: {raw_response}")
                # Fallback al método anterior si JSON falla
                strategies = [s.strip() for s in raw_response.split('\n') if
                              s.strip()]

            logger.info(f"Generated {len(strategies)} valid strategies")
            return strategies[:num_cards]

        except Exception as e:
            logger.error(f"Error during LangChain generation: {e}")
            raise

    def _generate_deck_name(self, discipline: str, color: str) -> str:
        """Genera un nombre evocativo para la baraja"""
        logger.debug(
            f"Generating deck name for discipline='{discipline}', color='{color}'")

        try:
            result = self.name_chain.invoke({
                "discipline": discipline,
                "color": color
            })
            name = result.content.strip().strip('"')
            logger.debug(f"Generated deck name: '{name}'")
            return name
        except Exception as e:
            logger.error(f"Error generating deck name: {e}")
            raise
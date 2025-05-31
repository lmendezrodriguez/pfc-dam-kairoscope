import os
import random
from typing import Dict, List, Any, Optional

from langchain_core.prompts import PromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI, \
    GoogleGenerativeAIEmbeddings  # Puede que necesites importar Embeddings si quieres inicializarlo aquí, aunque mejor fuera.
from langchain_core.runnables import RunnableSequence
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain.chains.retrieval import create_retrieval_chain
from langchain_core.vectorstores import \
    VectorStoreRetriever  # Importar para type hinting



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
            raise ValueError(
                "Se requiere un retriever cargado para inicializar DeckGenerator.")
        self.retriever = retriever

        # Cargar configuración de la API de Google y el LLM
        api_key = os.getenv('GOOGLE_API_KEY')
        if api_key:
            # NOTA: La configuración global genai.configure() ya no es necesaria si solo usas LangChain
            self.use_llm = True

            # Inicializar el modelo LLM usando LangChain
            self.llm = ChatGoogleGenerativeAI(
                model="gemini-2.5-flash-preview-05-20",
                temperature=0.8,  # Puede ajustarse para más/menos creatividad
                top_p=0.9,
                max_output_tokens=4096
            )
        else:
            print(
                "Warning: GOOGLE_API_KEY no encontrada. Usando generación fallback.")
            self.use_llm = False
            # Si no hay LLM, el retriever tampoco se usará, pero la clase aún necesita el retriever
            # pasado para no fallar en _setup_chains, aunque _setup_chains se llama solo si use_llm es True.
            # Podrías añadir un check self.retriever is None si el fallback fuera más complejo.

        # Estrategias predefinidas para fallback (simplificadas)
        self.fallback_strategies = [
            "Abraza la limitación como una puerta a la creatividad inesperada.",
            "Cambia el medio. Si estás dibujando, describe con palabras.",
            "Invierte el proceso. Empieza por el final.",
            "Usa el error como una oportunidad.",
            "Repite algo.",
            "Substrae.",
            "Añade algo más simple.",
            "Elige el punto más difícil.",
            "No hagas nada por un tiempo.",
            "Mira de cerca los detalles.",
            "Considera un error anterior como una directriz.",
            "Pinta solo una cosa.",
            "Deshaz lo obvio.",
            "Ve al extremo.",
            "Haz una lista de todo lo que harías normalmente, no lo hagas.",
            "Solo haz las cosas más aburridas primero.",
            "No construyas una casa, construye una pared.",
            "¿Qué haría tu oponente?",
            "Imagina una audiencia.",
            "Piensa en lo que falta.",
            "Trabaja a una velocidad diferente.",
        ]  # Agregadas algunas más para tener variedad

        # Configurar las plantillas de prompts
        self._setup_prompts()

        # Configurar las cadenas de LangChain (solo si hay LLM)
        if self.use_llm:
            self._setup_chains()

    def _setup_prompts(self):
        """Configura las plantillas de prompts para la generación."""

        # Plantilla para generar estrategias
        # NOTA: Prompt ajustado para enfocarse en inspiración y pensamiento lateral.
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
 6. NO usen guiones, asteriscos, viñetas, numeración o formato adicional. Cada estrategia debe ser una línea independiente.

 Genera exactamente {num_cards} líneas."""
        )

        # Plantilla para generar el nombre de la baraja (puede quedar similar)
        self.name_prompt = PromptTemplate.from_template(
            """Genera un nombre corto (máximo 4 palabras), poético y evocativo para una baraja de estrategias creativas oblicuas inspirada por:
 - Disciplina: {discipline}
 - Color: {color}
 - Naturaleza: Lateral, experimental

 Solo responde con el nombre, nada más. No incluyas explicaciones ni comillas."""
        )

    def _setup_chains(self):
        """Configura las cadenas de LangChain para la generación."""

        # Cadena para generar el nombre de la baraja
        self.name_chain = self.name_prompt | self.llm

        # Cadena para combinar documentos y generar respuesta (estrategias)
        self.strategies_document_chain = create_stuff_documents_chain(
            self.llm, self.strategies_prompt
        )

        # Cadena de recuperación completa (combina el retriever con la cadena de documentos)
        # Ahora usamos el retriever que se pasó en el constructor
        self.retrieval_chain = create_retrieval_chain(
            self.retriever,
            self.strategies_document_chain
        )

    def generate_deck(self, discipline: str, block_description: str,
                      color: str, num_cards: int = 123) -> Dict[str, Any]:
        """Genera una baraja completa de estrategias oblicuas"""

        if self.use_llm and self.retriever:  # Asegurarse de que tenemos LLM y retriever
            try:
                # Usar LangChain para generar estrategias
                strategies = self._generate_with_langchain(
                    discipline, block_description, color, num_cards
                )
            except Exception as e:
                print(f"Error con LangChain/Retrieval: {e}")
                # Si la generación con LLM/RAG falla, usamos el fallback
                strategies = self._generate_fallback_strategies(num_cards)
        else:
            # Si no hay LLM o retriever disponible desde el inicio, usamos fallback
            strategies = self._generate_fallback_strategies(num_cards)

        # Generar nombre de baraja (usar LLM si está disponible, si no fallback)
        deck_name = self._generate_deck_name(discipline, color)

        return {
            'name': deck_name,
            'strategies': strategies,
            'metadata': {
                'discipline': discipline,
                'block_description': block_description,
                'color': color,
                'requested_cards': num_cards,
                'generated_with': 'langchain' if (
                            self.use_llm and self.retriever and len(
                        strategies) > 0 and strategies[
                                0] not in self.fallback_strategies) else 'fallback'
                # Heurística simple para ver si se usó LLM vs Fallback real
            }
        }

    def _generate_with_langchain(self, discipline: str, block_description: str,
                                 color: str, num_cards: int) -> List[str]:
        """Genera estrategias usando LangChain y RAG"""

        print(
            f"Generando {num_cards} estrategias para disciplina='{discipline}', bloqueo='{block_description}', color='{color}'...")
        try:
            # --- FIX START ---
            # Decide what text to use as the query for the retriever.
            # Combine discipline and description, or just use description.
            retriever_query = f"Estrategias oblicuas para {discipline}: {block_description}"
            # Or simply: retriever_query = block_description

            # Prepare the input dictionary for the retrieval chain.
            # It MUST include the key the retriever uses ('input' by default)
            # AND all the keys needed by the final prompt template.
            chain_input = {
                "input": retriever_query,
                # This is the query text for the retriever
                "discipline": discipline,
                # These are needed by the prompt template
                "block_description": block_description,
                "color": color,
                "num_cards": num_cards,
                # 'context' will be automatically added by the retrieval_chain
                # based on the retriever's results using "input"
            }

            # Ejecutar la cadena de recuperación y generación
            # .invoke() handles retrieval (using "input"), adds "context",
            # formats the prompt (using all keys), and calls the LLM.
            result = self.retrieval_chain.invoke(chain_input)
            # --- FIX END ---

            # The result dictionary should contain 'input', 'context', and 'answer'
            raw_strategies = result.get("answer", "").strip().split(
                '\n')  # Use .get for safety

            # Limpiar y validar las estrategias generadas por el LLM
            strategies = [s.strip() for s in raw_strategies if s.strip()]

            # Limitar al número solicitado
            return strategies[:num_cards]

        except Exception as e:
            print(f"Error durante la generación con LangChain/Gemini: {e}")
            raise  # Re-lanzar para que generate_deck pueda usar el fallback

    # Simplificamos la lógica de fallback para solo tomar N aleatorias o repetir si es necesario
    def _generate_fallback_strategies(self, num_cards: int) -> List[str]:
        """Genera un número determinado de estrategias de fallback aleatorias."""
        print(f"Generando {num_cards} estrategias de fallback...")
        fallback_count = len(self.fallback_strategies)

        if num_cards <= fallback_count:
            # Si necesitamos menos o igual que las disponibles, barajamos y tomamos N
            return random.sample(self.fallback_strategies, num_cards)
        else:
            # Si necesitamos más, repetimos las disponibles de forma aleatoria
            result = []
            while len(result) < num_cards:
                result.extend(random.sample(self.fallback_strategies,
                                            min(num_cards - len(result),
                                                fallback_count)))
            return result[:num_cards]

    def _generate_deck_name(self, discipline: str, color: str) -> str:
        """Genera un nombre evocativo para la baraja"""
        if self.use_llm:
            try:
                # Usar LangChain para generar el nombre
                result = self.name_chain.invoke({
                    "discipline": discipline,
                    "color": color
                })
                # --- MODIFICACIÓN AQUÍ ---
                # Acceder al contenido del mensaje usando .content en lugar de ["text"]
                name = result.content.strip().strip('"')
                # --- FIN MODIFICACIÓN ---

                # Opcional: Limitar a 4 palabras si el LLM no cumple la instrucción
                # name_parts = name.split()
                # return " ".join(name_parts[:4])
                return name
            except Exception as e:
                print(f"Error generando nombre con LangChain: {e}")
                # Este print aparece en tu output, lo cual confirma que el error ocurre aquí.
                # Aquí podrías decidir si re-lanzar el error o retornar un nombre de fallback
                # Retornar fallback parece lo más seguro si el LLM falla solo en el nombre.
                return self._generate_fallback_name(discipline, color) # Llamar al fallback si falla el LLM


        # Fallback simple si LLM no está disponible desde el inicio
        return self._generate_fallback_name(discipline, color)

    def _generate_fallback_name(self, discipline: str, color: str) -> str:
        """Genera un nombre de fallback simple."""
        prefixes = ["Estrategias", "Caminos", "Rutas", "Exploraciones",
                    "Reflexiones"]
        suffixes = ["Oblicuas", "Creativas", "Tangenciales", "Laterales",
                    "Inesperadas"]
        discipline_hints = [discipline.capitalize(), "Del Bloqueo", "Del Color"] # Capitalizar disciplina
        fallback_name = f"{random.choice(prefixes)} {random.choice(suffixes)}"
        if random.random() > 0.4 and discipline_hints:  # Añadir un hint del input a veces
             # Asegurarse de que el hint es una palabra simple si es un campo libre
             hint = random.choice(discipline_hints).split()[0] # Tomar solo la primera palabra si hay espacios
             fallback_name += f" {hint}"
        return fallback_name[:30].strip() # Cortar para mantenerlo corto
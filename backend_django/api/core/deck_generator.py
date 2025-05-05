# backend_django/api/core/deck_generator.py
import os
import random
from typing import Dict, List, Any
import google.generativeai as genai
from .rag_processor import RAGProcessor


class DeckGenerator:
    def __init__(self):
        self.rag_processor = RAGProcessor()

        # Configurar Google Gemini
        api_key = os.getenv('GOOGLE_API_KEY')
        if api_key:
            genai.configure(api_key=api_key)
            self.use_gemini = True
        else:
            print(
                "Warning: GOOGLE_API_KEY no encontrada. Usando generación fallback.")
            self.use_gemini = False

        # Estrategias predefinidas para fallback
        self.fallback_strategies = [
            "Abraza la limitación como una puerta a la creatividad inesperada.",
            "Cambia el medio. Si estás dibujando, describe con palabras.",
            "Invierte el proceso. Empieza por el final.",
            "Usa el error como material de trabajo.",
            "Pregúntate qué no harías. Luego házlo.",
            "Aplica una limitación absurda a tu trabajo actual.",
            "Busca lo incompleto en lo perfecto.",
            "Abandona temporalmente la lógica.",
            "Magnifica un detalle trivial.",
            "Permite que el azar decida el siguiente paso.",
            "Traduce tu problema a otro lenguaje artístico.",
            "Trabaja en silencio absoluto.",
            "Identifica patrones ocultos en el caos.",
            "Encuentra belleza en lo imperfecto.",
            "Cambia de perspectiva: observa desde arriba.",
            "Acelera incómodamente el proceso.",
            "Ralentiza extremadamente cada movimiento.",
            "Usa una herramienta incorrectamente.",
            "Aplica la regla de tres: reduce todo a tres elementos.",
            "Destruye para crear: elimina lo perfecto.",
            "Busca lo sagrado en lo cotidiano.",
            "Trabaja con los ojos cerrados.",
            "Imita el estilo de tu enemigo creativo.",
            "Convierte el obstáculo en herramienta.",
            "Permite que el dolor sea tu maestro.",
            "Escribe/crea como si fuera tu última obra.",
            "Busca conexiones imposibles.",
            "Abre puertas que crees cerradas.",
            "Deja que el silencio hable.",
            "Convierte lo invisible en materia."
        ]

    def generate_deck(self, discipline: str, block_description: str,
                      color: str, num_cards: int = 123) -> Dict[str, Any]:
        """Genera una baraja completa de estrategias oblicuas"""

        # Buscar inspiración en la knowledge base
        inspiration = self.rag_processor.search_relevant_content(
            block_description, k=8)

        if self.use_gemini:
            try:
                strategies = self._generate_with_gemini(discipline,
                                                        block_description,
                                                        color, inspiration,
                                                        num_cards)
            except Exception as e:
                print(f"Error con Gemini: {e}")
                strategies = self._generate_fallback_strategies(num_cards)
        else:
            strategies = self._generate_fallback_strategies(num_cards)

        # Generar nombre de baraja
        deck_name = self._generate_deck_name(discipline, color)

        return {
            'name': deck_name,
            'strategies': strategies,
            'metadata': {
                'discipline': discipline,
                'block_description': block_description,
                'color': color,
                'generated_with': 'gemini' if self.use_gemini else 'fallback'
            }
        }

    def _generate_with_gemini(self, discipline: str, block_description: str,
                              color: str,
                              inspiration: List[Dict], num_cards: int) -> List[
        str]:
        """Genera estrategias usando Google Gemini"""

        # Crear prompt contextual
        inspiration_text = "\n".join([
            f"- {result['text']} (Autor: {result['metadata'].get('autor', 'Anónimo')})"
            for result in inspiration
        ])

        prompt = f"""Eres un generador de estrategias oblicuas al estilo de Brian Eno y Peter Schmidt.

Contexto creativo:
- Disciplina: {discipline}
- Bloqueo descrito: {block_description}
- Color inspirador: {color}

Inspiración desde la knowledge base:
{inspiration_text}

Genera {num_cards} estrategias oblicuas que:
1. Sean concisas pero evocativas
2. Desafíen el pensamiento convencional
3. Conecten con el bloqueo específico de {discipline}
4. Tengan un tono poético y experimental
5. Incorporen la sensibilidad artística de las inspiraciones

Formato: una estrategia por línea, sin numeración ni formato adicional.
NO uses guiones, asteriscos o viñetas."""

        # Configurar el modelo Gemini
        model = genai.GenerativeModel('models/gemini-1.5-pro-latest')

        # Generar respuesta
        response = model.generate_content(
            prompt,
            generation_config=genai.types.GenerationConfig(
                temperature=0.8,
                top_p=0.8,
                top_k=40,
                max_output_tokens=2048
            )
        )

        strategies = response.text.strip().split('\n')

        # Limpiar estrategias
        strategies = [s.strip() for s in strategies if
                      s.strip() and not s.startswith('-')]

        # Si necesitamos más estrategias
        if len(strategies) < num_cards:
            remaining = num_cards - len(strategies)

            additional_prompt = f"""Genera {remaining} estrategias oblicuas adicionales para {discipline}.
Deben ser únicas y diferentes a las anteriores.
Formato simple: una por línea, sin formato extra."""

            additional_response = model.generate_content(
                additional_prompt,
                generation_config=genai.types.GenerationConfig(
                    temperature=0.9,
                    max_output_tokens=1024
                )
            )

            additional_strategies = additional_response.text.strip().split(
                '\n')
            additional_strategies = [s.strip() for s in additional_strategies
                                     if s.strip()]
            strategies.extend(additional_strategies)

        return strategies[:num_cards]

    def _generate_fallback_strategies(self, num_cards: int) -> List[str]:
        """Genera estrategias usando el conjunto predefinido"""
        if num_cards > len(self.fallback_strategies):
            strategies = self.fallback_strategies.copy()

            # Crear variaciones simples
            variations = []
            for strategy in self.fallback_strategies:
                variations.append(strategy.replace(".", " momentáneamente."))
                variations.append(strategy.replace("el",
                                                   "tu") if "el" in strategy else strategy)
                variations.append(strategy + " Sin planificación.")

            strategies.extend(variations)
            strategies = strategies[:num_cards]
        else:
            strategies = random.sample(self.fallback_strategies, num_cards)

        return strategies

    def _generate_deck_name(self, discipline: str, color: str) -> str:
        """Genera un nombre evocativo para la baraja"""
        if self.use_gemini:
            try:
                model = genai.GenerativeModel('models/gemini-1.5-pro-latest')

                prompt = f"""Genera un nombre corto y poético para una baraja de estrategias creativas.
Contexto:
- Disciplina: {discipline}
- Color: {color}

El nombre debe ser evocativo y memorable, máximo 4 palabras.
Debe capturar el espíritu experimental de las estrategias oblicuas.
Ejemplo de nombres: "Senderos Oblicuos", "Ecos Creativos", "Prismas Narrativos"
Solo responde con el nombre, nada más."""

                response = model.generate_content(
                    prompt,
                    generation_config=genai.types.GenerationConfig(
                        temperature=0.7,
                        max_output_tokens=50
                    )
                )

                return response.text.strip()

            except Exception as e:
                print(f"Error generando nombre con Gemini: {e}")

        # Fallback simple
        prefixes = ["Estrategias", "Caminos", "Rutas", "Exploraciones"]
        suffixes = ["Oblicuas", "Creativas", "Experimentales", "Esenciales"]
        return f"{random.choice(prefixes)} {random.choice(suffixes)}"
"""
Módulo responsable de generar nombres para barajas de estrategias.
"""
import os
import random
from typing import Optional

from langchain.prompts import PromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain.chains import LLMChain


class DeckNamer:
    """Genera nombres evocativos para barajas de estrategias."""

    def __init__(self, api_key: Optional[str] = None):
        """
        Inicializa el generador de nombres.

        Args:
            api_key: API key para Google. Si es None, se busca en variables de entorno.
        """
        # Verificar API key
        self.api_key = api_key or os.getenv('GOOGLE_API_KEY')
        if self.api_key:
            # Configurar LLM con LangChain
            self.llm = ChatGoogleGenerativeAI(
                model="gemini-1.5-pro",
                google_api_key=self.api_key,
                temperature=0.7,
                # Menor temperatura para nombres más enfocados
                max_output_tokens=50  # Nombres cortos
            )
            self.use_llm = True
        else:
            print(
                "DeckNamer: ADVERTENCIA - No se encontró GOOGLE_API_KEY. Usando fallback.")
            self.use_llm = False

    def generate_name(self, discipline: str, color: str) -> str:
        """
        Genera un nombre evocativo para una baraja.

        Args:
            discipline: La disciplina creativa
            color: Color elegido (formato hex)

        Returns:
            Nombre generado para la baraja
        """
        if self.use_llm:
            try:
                return self._generate_with_langchain(discipline, color)
            except Exception as e:
                print(f"DeckNamer: Error con LangChain: {e}")
                return self._generate_fallback_name(discipline)
        else:
            return self._generate_fallback_name(discipline)

    def _generate_with_langchain(self, discipline: str, color: str) -> str:
        """
        Genera un nombre usando LangChain.

        Args:
            discipline: La disciplina creativa
            color: Color elegido

        Returns:
            Nombre para la baraja
        """
        # Crear plantilla para el nombre
        template = """Genera un nombre corto y poético para una baraja de estrategias creativas.
Contexto:
- Disciplina: {discipline}
- Color: {color}

El nombre debe ser evocativo y memorable, máximo 4 palabras.
Debe capturar el espíritu experimental de las estrategias oblicuas.
Ejemplo de nombres: "Senderos Oblicuos", "Ecos Creativos", "Prismas Narrativos"
Solo responde con el nombre, nada más."""

        # Crear prompt
        prompt = PromptTemplate(
            input_variables=["discipline", "color"],
            template=template
        )

        # Crear cadena LLM
        chain = LLMChain(llm=self.llm, prompt=prompt)

        # Ejecutar cadena
        result = chain.run(
            discipline=discipline,
            color=color
        )

        # Limpiar resultado
        return result.strip()

    def _generate_fallback_name(self, discipline: str) -> str:
        """
        Genera un nombre simple cuando falla la generación con LLM.

        Args:
            discipline: La disciplina creativa

        Returns:
            Nombre para la baraja
        """
        prefixes = ["Estrategias", "Caminos", "Rutas", "Exploraciones",
                    "Momentos", "Visiones"]
        middle_words = ["de", "para", "hacia", "en"]

        if random.random() < 0.5:
            return f"{random.choice(prefixes)} {random.choice(middle_words)} {discipline}"
        else:
            suffixes = ["Oblicuas", "Creativas", "Experimentales",
                        "Esenciales", "Divergentes"]
            return f"{random.choice(prefixes)} {random.choice(suffixes)}"
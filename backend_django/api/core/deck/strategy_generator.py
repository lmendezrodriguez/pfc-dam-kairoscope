"""
Módulo responsable de generar estrategias oblicuas individuales.
"""
import os
import random
from typing import List, Optional

from langchain.prompts import PromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain.chains import LLMChain
from langchain.schema import Document

from ..rag.rag_processor import RAGProcessor


class StrategyGenerator:
    """Genera estrategias oblicuas basadas en parámetros y contenido de RAG."""

    def __init__(self, api_key: Optional[str] = None):
        """
        Inicializa el generador de estrategias.

        Args:
            api_key: API key para Google. Si es None, se busca en variables de entorno.
        """
        self.rag_processor = RAGProcessor()

        # Verificar API key
        self.api_key = api_key or os.getenv('GOOGLE_API_KEY')
        if self.api_key:
            # Configurar LLM con LangChain
            self.llm = ChatGoogleGenerativeAI(
                model="gemini-1.5-pro",
                google_api_key=self.api_key,
                temperature=0.8,
                top_p=0.8,
                top_k=40,
                max_output_tokens=2048
            )
            self.use_llm = True
        else:
            print(
                "StrategyGenerator: ADVERTENCIA - No se encontró GOOGLE_API_KEY. Usando fallback.")
            self.use_llm = False

        # Cargar estrategias de fallback
        self.fallback_strategies = self._load_fallback_strategies()

    def _load_fallback_strategies(self) -> List[str]:
        """
        Carga estrategias de fallback desde la knowledge base.

        Returns:
            Lista de estrategias oblicuas
        """
        # Buscar estrategias específicamente por etiquetas
        docs = self.rag_processor.search_by_tags(
            ["instruments", "creativity", "strategy"], k=50)

        if not docs:
            # Si no encuentra con etiquetas, buscar por término genérico
            docs = self.rag_processor.search_relevant_content(
                "oblique strategies eno", k=50)

        # Extraer estrategias de los documentos
        strategies = [doc.page_content for doc in docs if
                      doc.page_content.strip()]

        # Si no se encontraron estrategias, proporcionar algunas por defecto
        if not strategies:
            print(
                "StrategyGenerator: No se encontraron estrategias en la knowledge base. Usando predeterminadas.")
            return [
                "Abraza la limitación como una puerta a la creatividad inesperada.",
                "Cambia el medio. Si estás dibujando, describe con palabras.",
                "Invierte el proceso. Empieza por el final.",
                "Usa el error como material de trabajo.",
                "Pregúntate qué no harías. Luego házlo.",
                "Aplica una limitación absurda a tu trabajo actual.",
                "Busca lo incompleto en lo perfecto.",
                "Abandona temporalmente la lógica.",
                "Magnifica un detalle trivial.",
                "Permite que el azar decida el siguiente paso."
            ]

        return strategies

    def generate_strategies(self, discipline: str, block_description: str,
                            color: str, inspiration_docs: List[Document],
                            num_strategies: int) -> List[str]:
        """
        Genera estrategias oblicuas personalizadas.

        Args:
            discipline: La disciplina creativa
            block_description: Descripción del bloqueo
            color: Color elegido (formato hex)
            inspiration_docs: Documentos de inspiración de RAG
            num_strategies: Número de estrategias a generar

        Returns:
            Lista de estrategias generadas
        """
        if self.use_llm:
            try:
                return self._generate_with_langchain(
                    discipline, block_description, color, inspiration_docs,
                    num_strategies
                )
            except Exception as e:
                print(f"StrategyGenerator: Error con LangChain: {e}")
                return self._generate_fallback_strategies(num_strategies)
        else:
            return self._generate_fallback_strategies(num_strategies)

    def _generate_with_langchain(self, discipline: str, block_description: str,
                                 color: str, inspiration_docs: List[Document],
                                 num_strategies: int) -> List[str]:
        """
        Genera estrategias usando LangChain y el modelo configurado.

        Args:
            discipline: La disciplina creativa
            block_description: Descripción del bloqueo
            color: Color elegido
            inspiration_docs: Documentos de inspiración
            num_strategies: Número de estrategias a generar

        Returns:
            Lista de estrategias generadas
        """
        # Convertir documentos a texto de inspiración
        inspiration_text = "\n".join([
            f"- {doc.page_content} (Fuente: {doc.metadata.get('autor', 'Anónimo')})"
            for doc in inspiration_docs
        ])

        # Crear plantilla para el prompt
        template = """Eres un generador de estrategias oblicuas al estilo de Brian Eno y Peter Schmidt.

Contexto creativo:
- Disciplina: {discipline}
- Bloqueo descrito: {block_description}
- Color inspirador: {color}

Inspiración desde la knowledge base:
{inspiration_text}

Genera {num_strategies} estrategias oblicuas que:
1. Sean concisas pero evocativas
2. Desafíen el pensamiento convencional
3. Conecten con el bloqueo específico de {discipline}
4. Tengan un tono poético y experimental
5. Incorporen la sensibilidad artística de las inspiraciones

Formato: una estrategia por línea, sin numeración ni formato adicional.
NO uses guiones, asteriscos o viñetas."""

        # Crear objeto de plantilla LangChain
        prompt = PromptTemplate(
            input_variables=["discipline", "block_description", "color",
                             "inspiration_text", "num_strategies"],
            template=template
        )

        # Crear cadena LLM
        chain = LLMChain(llm=self.llm, prompt=prompt)

        # Ejecutar la cadena
        response = chain.run(
            discipline=discipline,
            block_description=block_description,
            color=color,
            inspiration_text=inspiration_text,
            num_strategies=num_strategies
        )

        # Procesar la respuesta
        strategies = response.strip().split('\n')

        # Limpiar estrategias
        strategies = [s.strip() for s in strategies if
                      s.strip() and not s.startswith('-')]

        # Si necesitamos más estrategias
        if len(strategies) < num_strategies:
            remaining = num_strategies - len(strategies)

            # Crear plantilla para estrategias adicionales
            additional_template = """Genera {remaining} estrategias oblicuas adicionales para {discipline}.
Deben ser únicas y diferentes a las anteriores.
Formato simple: una por línea, sin formato extra."""

            additional_prompt = PromptTemplate(
                input_variables=["discipline", "remaining"],
                template=additional_template
            )

            additional_chain = LLMChain(llm=self.llm, prompt=additional_prompt)

            additional_response = additional_chain.run(
                discipline=discipline,
                remaining=remaining
            )

            additional_strategies = additional_response.strip().split('\n')
            additional_strategies = [s.strip() for s in additional_strategies
                                     if s.strip()]
            strategies.extend(additional_strategies)

        return strategies[:num_strategies]

    def _generate_fallback_strategies(self, num_strategies: int) -> List[str]:
        """
        Genera estrategias usando el conjunto predefinido (fallback).

        Args:
            num_strategies: Número de estrategias a generar

        Returns:
            Lista de estrategias
        """
        if num_strategies > len(self.fallback_strategies):
            # Si necesitamos más estrategias de las que tenemos, creamos variaciones
            strategies = self.fallback_strategies.copy()

            # Crear variaciones simples
            variations = []
            for strategy in self.fallback_strategies:
                variations.append(strategy.replace(".", " momentáneamente."))
                variations.append(strategy.replace("el",
                                                   "tu") if "el" in strategy else strategy)
                variations.append(strategy + " Sin planificación.")
                variations.append("Intenta: " + strategy)
                variations.append("Considera: " + strategy)

            strategies.extend(variations)
            # Eliminar posibles duplicados
            strategies = list(set(strategies))

            # Si aún necesitamos más, repetir algunas aleatoriamente
            while len(strategies) < num_strategies:
                strategies.append(random.choice(self.fallback_strategies))

            return strategies[:num_strategies]
        else:
            # Si tenemos suficientes, seleccionamos aleatoriamente
            return random.sample(self.fallback_strategies, num_strategies)
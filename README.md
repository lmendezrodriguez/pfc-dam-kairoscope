# 🔮 kAIroscope

<img src="https://raw.githubusercontent.com/lmendezrodriguez/pfc-dam-kairoscope/main/kairoscope%20rectangular.png" alt="kAIroscope Logo" width="400">

![Badge de estado](https://img.shields.io/badge/STATUS-EN%20DESARROLLO-green)
![Badge de licencia](https://img.shields.io/badge/license-MIT-blue)

> *"Desbloquea tu imaginación"*  
> Una reiterpretación de las Estrategias Oblicuas para la era de la IA

## Índice
- [Descripción del proyecto](#descripción-del-proyecto)
- [Estado del proyecto](#estado-del-proyecto)
- [Características y demostración](#características-y-demostración)
- [Acceso al proyecto](#acceso-al-proyecto)
- [Cómo abrir y ejecutar el proyecto](#cómo-abrir-y-ejecutar-el-proyecto)
- [Tecnologías utilizadas](#tecnologías-utilizadas)
- [Personas desarrolladoras del proyecto](#personas-desarrolladoras-del-proyecto)
- [Licencia](#licencia)

## Descripción del Proyecto
kAIroscope es una aplicación Android que reinventa las  **Estrategias Oblicuas** de Brian Eno y Peter Schmidt (1975) para la era de la inteligencia artificial. 

Cuando la inspiración se desvanece y te enfrentas a la página en blanco, kAIroscope genera barajas completamente personalizadas de **123 cartas únicas** con estrategias creativas adaptadas a tu disciplina artística, tu bloqueo específico y tu estado emocional. Cada carta es una ventana hacia el pensamiento lateral que necesitas para romper patrones y descubrir nuevas perspectivas.

**🎓 Proyecto de Fin de Ciclo** - Desarrollo de Aplicaciones Multiplataforma (DAM) - Afundacion A Coruña 2025

## Estado del Proyecto
🚧 **En construcción activa** 🚧

Funcionalidades principales implementadas y funcionales.

## Características y Demostración
- 🔐 `Autenticación Firebase`: Registro y login seguro con gestión de sesiones
- 🤖 `Generación IA personalizada`: Powered by OpenAi con sistema RAG avanzado
- 🎨 `Personalización profunda`: Por disciplina creativa, tipo de bloqueo y color emocional
- 📚 `Biblioteca personal`: Hasta 8 barajas únicas guardadas por usuario
- 🎲 `Magia aleatoria`: Extrae cartas al azar cuando necesites inspiración
- ⚡ `Gestión completa`: Crear, visualizar, usar y eliminar barajas sin límites
- 🌙 `Experiencia premium`: Modo oscuro y tipografías personalizadas

*"El azar te guía cuando la lógica te abandona"*

## Acceso al Proyecto
**Para acceder al código fuente:**

    git clone https://github.com/lmendezrodriguez/pfc-dam-kairoscope

## Cómo abrir y ejecutar el proyecto

### Requisitos previos
- Android Studio Iguana (2023.2) o superior
- Python 3.8+
- Java Development Kit 17
- 🔑 **Credenciales necesarias**: Firebase Project configurado + OpenAI API Key

### 🐍 Backend (Django)

    cd backend_django
    python -m venv venv
    source venv/bin/activate
    # En Windows: venv\Scripts\activate
    pip install -r requirements.txt
    python manage.py migrate

**🧠 Preparación del conocimiento (RAG/Vector Store):**

Antes de generar estrategias, necesitas construir la base de conocimiento que alimentará la IA:

    python manage.py process_rag

Este comando personalizado procesa y vectoriza todo el corpus de estrategias oblicuas, manifiestos artísticos y textos creativos, creando el vector store FAISS que permite la generación contextual inteligente.

**Iniciar servidor:**

    python manage.py runserver

### 📱 Frontend (Android)
1. Abrir `android_app/` en Android Studio
2. Añadir tu `google-services.json` en `app/src/`
3. Sincronizar proyecto con Gradle Files
4. Ejecutar en emulador Android o dispositivo físico

### 🔐 Variables de entorno necesarias

    # backend_django/.env
    OPENAI_API_KEY=tu_clave_openai
    FIREBASE_CREDENTIALS_PATH=ruta/a/firebase-credentials.json

## Tecnologías utilizadas

### 🎨 Frontend
- **Android Nativo** (Java) con arquitectura MVVM
- **Material Design 3** con temas personalizados
- **Firebase Authentication** para identidad segura
- **Retrofit + OkHttp** para comunicación API
- **Navigation Component** para flujos intuitivos

### ⚙️ Backend
- **Django** con Vistas Basadas en Funciones (FBVs)
- **LangChain** para orquestación RAG/LLM avanzada
- **OpenAI** como cerebro generativo
- **FAISS** para búsqueda vectorial semántica
- **SQLite** para persistencia y gestión de usuarios

### 🧬 Sistema RAG/LLM
La magia ocurre aquí: un sistema de **Recuperación Aumentada por Generación** que combina:
- Corpus diverso (estrategias originales + manifiestos artísticos + literatura) de datos estructurados y no estructurados
- Tres tipos de búsqueda vectorial inteligente para contexto relevante: por similitud, por divergencia y al azar
- Prompt engineering especializado para mantener el carácter "oblicuo"
- Generación personalizada según disciplina y bloqueo específico

## Personas desarrolladoras del proyecto
| Lucía Méndez Rodríguez |
| :---: |
| *Filóloga outlier adentrándose en el mundo del desarrollo* |
| *Estudiante DAM - Especializándose en IA aplicada* |

> *"Este proyecto une mi pasión por la lengua y la literatura con mi curiosidad por la IA y el desarrollo, haciendo el determinismo de la programación un poco más indeterminado."*

**📚 Contexto académico**: Proyecto de Fin de Ciclo (PFC) - DAM 2025 - Centro FP Afundación A Coruña

## Licencia
Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE.md](LICENSE.md) para detalles.

---
### 🎭 Inspiración Original
*Basado en las "Oblique Strategies" de **Brian Eno** y **Peter Schmidt** (1975)*  
*"Más de 100 cartas, cada una con una frase críptica o aforismo, diseñadas para ayudar a los artistas a romper bloqueos creativos a través del pensamiento lateral."*

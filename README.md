# 🔮 kAIroscope

Una aplicación Android que genera barajas personalizadas de estrategias oblicuas para superar bloqueos creativos, utilizando IA generativa y técnicas RAG (Retrieval-Augmented Generation).

## 📋 Descripción

kAIroscope combina el concepto de "Estrategias Oblicuas" de Brian Eno y Peter Schmidt con tecnología de IA moderna para ofrecer sugerencias creativas adaptadas a tu disciplina, tipo de bloqueo y estado de ánimo.

La aplicación te permite:
- Crear barajas personalizadas de 123 estrategias
- Guardar múltiples barajas según diferentes contextos creativos
- Explorar estrategias aleatorias cuando necesites inspiración

## 🏗️ Arquitectura

### Frontend (Android)
- **Patrón MVVM** (Model-View-ViewModel)
- **Firebase Authentication** para gestión de usuarios
- **Material Design 3** con tema personalizado y tipografías
- Diseño responsive para diferentes tamaños de pantalla

### Backend (Django)
- **Firebase Admin SDK** para verificación de tokens
- **Modelos Django** para persistencia de datos (UserProfile, Deck, Card)
- **LLM/RAG** para generación de contenido personalizado
- Vistas basadas en funciones para los endpoints API

## 🔧 Stack Tecnológico

- **Android:** Java, Retrofit, LiveData, Navigation Component
- **Backend:** Python, Django, Firebase Admin
- **Generación IA:** Gemini Pro, RAG (knowledge base embebida)
- **Persistencia:** SQLite (Django ORM)

## 🛠️ Desarrollo

Este proyecto forma parte del Proyecto Final de Ciclo de Desarrollo de Aplicaciones Multiplataforma (DAM). La implementación sigue prácticas modernas de desarrollo:

- Control de versiones Git con convenciones claras de commits
- Separación de responsabilidades (repository, viewmodel, fragments)
- Tipado fuerte en todas las capas
- Diseño UI/UX cuidado y consistente

## 🔄 API

La aplicación se comunica con el backend Django mediante endpoints REST:

- `POST /api/decks/` - Crear una nueva baraja (requiere autenticación)
- `GET /api/decks/` - Obtener todas las barajas del usuario
- `GET /api/decks/{id}/` - Obtener detalles de una baraja específica
- `DELETE /api/decks/{id}/` - Eliminar una baraja

## 📱 Compatibilidad

- Android 8.0 (API 26) o superior
- Diseño adaptable a tablets y teléfonos
- Soporte para modo claro/oscuro

## 👨‍💻 Autor

Lucía Méndez Rodríguez - Estudiante de Desarrollo de Aplicaciones Multiplataforma (DAM)

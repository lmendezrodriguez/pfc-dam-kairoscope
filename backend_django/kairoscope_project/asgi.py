"""
Configuración ASGI para el proyecto kairoscope_project.

Expone el callable ASGI como una variable a nivel de módulo llamada ``application``.
ASGI permite el manejo de conexiones asíncronas (WebSockets, HTTP/2, etc.).

For more information on this file, see
https://docs.djangoproject.com/en/5.2/howto/deployment/asgi/
"""

import os

from django.core.asgi import get_asgi_application

# Configura el módulo de settings por defecto para ASGI
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'kairoscope_project.settings')

# Crea la aplicación ASGI que será utilizada por el servidor
application = get_asgi_application()
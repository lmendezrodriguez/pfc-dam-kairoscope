"""
Configuración WSGI para el proyecto kairoscope_project.

Expone el callable WSGI como una variable a nivel de módulo llamada ``application``.
WSGI es el estándar para aplicaciones web síncronas en Python.

For more information on this file, see
https://docs.djangoproject.com/en/5.2/howto/deployment/wsgi/
"""

import os

from django.core.wsgi import get_wsgi_application

# Configura el módulo de settings por defecto para WSGI
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'kairoscope_project.settings')

# Crea la aplicación WSGI que será utilizada por el servidor web
application = get_wsgi_application()
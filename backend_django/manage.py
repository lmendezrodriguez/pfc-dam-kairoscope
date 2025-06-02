#!/usr/bin/env python
"""
Utilidad de línea de comandos de Django para tareas administrativas.
Este es el punto de entrada principal para ejecutar comandos de gestión de Django.
"""
import os
import sys


def main():
    """
    Ejecuta tareas administrativas de Django.
    Configura el módulo de settings y delega la ejecución al sistema de comandos de Django.
    """
    # Establece el módulo de configuración por defecto si no está definido
    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'kairoscope_project.settings')
    
    try:
        from django.core.management import execute_from_command_line
    except ImportError as exc:
        # Error detallado si Django no está instalado o no está en el PYTHONPATH
        raise ImportError(
            "Couldn't import Django. Are you sure it's installed and "
            "available on your PYTHONPATH environment variable? Did you "
            "forget to activate a virtual environment?"
        ) from exc
    
    # Ejecuta el comando pasado por argumentos de línea de comandos
    execute_from_command_line(sys.argv)


if __name__ == '__main__':
    main()
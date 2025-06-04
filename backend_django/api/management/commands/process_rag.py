#!/usr/bin/env python
"""Utilidad de línea de comandos de Django para tareas administrativas."""
import os
import sys


def main():
   """Ejecuta tareas administrativas de Django."""
   os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'kairoscope_project.settings')
   try:
       from django.core.management import execute_from_command_line
   except ImportError as exc:
       raise ImportError(
           "No se pudo importar Django. ¿Estás seguro de que está instalado y "
           "disponible en tu variable de entorno PYTHONPATH? ¿Se te olvidó "
           "activar el entorno virtual?"
       ) from exc
   execute_from_command_line(sys.argv)


if __name__ == '__main__':
   main()
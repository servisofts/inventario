#!/bin/bash


# ESTO ES SOLO PARA UBUNTU

# Buscar el PID del proceso que escucha en el puerto 10031
PID=$(sudo ss -lptn 'sport = :10039' | grep -oP 'pid=\K\d+')

# Verificar si se encontró un PID
if [ -z "$PID" ]; then
  echo "No se encontró ningún proceso escuchando en el puerto 10039."
else
  echo "Terminando el proceso con PID $PID que escucha en el puerto 10039."
  # Terminar el proceso
  sudo kill -9 $PID
fi




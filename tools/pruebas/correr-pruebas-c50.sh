#!/bin/sh
# C50 · pruebas de la regla ReglaActualizacionG (G atadas a la corrida), sin JUnit ni red.
# Uso (desde la raíz del repo, después de compilar: mvn -o -q compile o package):
#   sh tools/pruebas/correr-pruebas-c50.sh
# Compila src/test/java/.../ReglaActualizacionGPruebas.java contra target/classes y la corre; sale con 1 si algo falla.
set -e
raiz=$(cd "$(dirname "$0")/../.." && (pwd -W 2>/dev/null || pwd))
out="$raiz/target/pruebas-c50"
mkdir -p "$out"
sep=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) sep=";";; esac
javac -nowarn -encoding UTF-8 -cp "$raiz/target/classes" -d "$out" \
  "$raiz/src/test/java/com/saa/ejb/rpr/service/ReglaActualizacionGPruebas.java"
java -cp "$out$sep$raiz/target/classes" com.saa.ejb.rpr.service.ReglaActualizacionGPruebas

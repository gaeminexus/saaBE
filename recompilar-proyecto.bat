@echo off
echo ========================================
echo    Recompilando Proyecto SaaBE
echo ========================================
echo.

echo [1/4] Limpiando directorio target...
if exist target rmdir /s /q target
mkdir target
mkdir target\classes
echo OK - Directorio target limpio

echo.
echo [2/4] Esperando a que Eclipse detecte los cambios...
timeout /t 3 /nobreak >nul

echo.
echo [3/4] Tocando archivos fuente para forzar recompilacion...
cd src\main\java
for /r %%f in (*.java) do (
    copy /b "%%f" +,, >nul 2>&1
)
cd ..\..\..
echo OK - Archivos fuente actualizados

echo.
echo [4/4] Esperando a que Eclipse compile (30 segundos)...
echo      Observa la barra de progreso en Eclipse (abajo a la derecha)
timeout /t 30 /nobreak

echo.
echo ========================================
echo   Recompilacion completada
echo ========================================
echo.
echo SIGUIENTE PASO:
echo 1. Verifica en Eclipse que no haya errores (vista Problems)
echo 2. Reinicia WildFly desde Eclipse
echo 3. Ejecuta: test-reporte-endpoint.ps1
echo.
pause

@echo off
echo Refrescando proyecto Maven...
echo.

REM Crear directorio target si no existe
if not exist "target" mkdir target
if not exist "target\classes" mkdir target\classes
if not exist "target\classes\META-INF" mkdir target\classes\META-INF

REM Copiar recursos
echo Copiando recursos...
if exist "src\main\resources" (
    xcopy /E /Y "src\main\resources\*" "target\classes\"
)

REM Mostrar estructura del proyecto
echo.
echo Estructura actual del proyecto:
tree /F /A src\main\java\com\saa\ws\rest\asoprep 2>nul
echo.

echo Proyecto refrescado. 
echo Por favor, en Eclipse:
echo 1. Haz clic derecho en el proyecto ^> Maven ^> Reload Projects
echo 2. O presiona Alt+F5 para Update Project
echo 3. Marca "Force Update of Snapshots/Releases"
echo.
pause
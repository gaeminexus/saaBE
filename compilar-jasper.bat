@echo off
REM Compila .jrxml a .jasper, fuera de WildFly.
REM Ver docs\logica-negocio\tsr\DISENO-REPORTES-CONCILIACION-BANCARIA.md #5 y CLAUDE.md ("Reportes").
REM
REM Uso:
REM   compilar-jasper.bat                                  -> solo los .jrxml que NO tienen .jasper
REM   compilar-jasper.bat ruta\al\REPORTE.jrxml [otro.jrxml ...]  -> recompila esos, siempre (caso real: lo acabas de editar)
REM   compilar-jasper.bat --forzar                         -> recompila TODO bajo rep\ (cambio de version de Jasper/harness)
REM No es por fecha de modificacion: despues de un git checkout todos los mtimes quedan iguales
REM y comparar por fecha recompilaria de mas, metiendo .jasper ajenos (de otro equipo) en tu commit.
REM
REM No toca las dependencias del WAR de produccion: usa el pom.xml aparte de tools\jasper,
REM que es el UNICO lugar del repo donde entra jasperreports-jdt.
REM
REM Requiere Maven en el PATH (correr "mvn -v" primero si no estas seguro en esta maquina).

setlocal

set REPO_ROOT=%~dp0
set TOOLS_DIR=%REPO_ROOT%tools\jasper
set BUILD_DIR=%TOOLS_DIR%\target\classes
set CP_FILE=%TOOLS_DIR%\target\classpath.txt

echo === 1/3: armando el classpath (mvn dependency:build-classpath) ===
call mvn -f "%TOOLS_DIR%\pom.xml" -q dependency:build-classpath -Dmdep.outputFile="%CP_FILE%"
if errorlevel 1 (
    echo ERROR: no se pudo armar el classpath. Revisa que Maven este en el PATH ^(mvn -v^) y que
    echo jasperreports-jdt este disponible ^(mvn dependency:get -Dartifact=net.sf.jasperreports:jasperreports-jdt:7.0.3^).
    exit /b 1
)
REM "set /p VAR=<archivo" trunca en Windows cmd.exe alrededor de los 1024 caracteres --
REM silenciosamente, sin error -- y este classpath supera los 3700. "for /f" no tiene ese
REM limite (confirmado: 40 entradas completas vs 12 con set /p, 2026-09-08, ver el hallazgo
REM completo en CLAUDE.md/"Reportes"). Con set /p, jasperreports-jdt y ecj -los ULTIMOS de la
REM lista- quedaban afuera segun el momento, y el compilador JDT fallaba de forma intermitente.
for /f "usebackq delims=" %%C in ("%CP_FILE%") do set "CLASSPATH=%%C"

echo === 2/3: compilando el harness ===
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
javac -encoding UTF-8 -cp "%CLASSPATH%" -d "%BUILD_DIR%" "%TOOLS_DIR%\src\tools\jasper\CompilarJasper.java"
if errorlevel 1 (
    echo ERROR: el harness no compilo.
    exit /b 1
)

echo === 3/3: compilando los .jrxml de src\main\resources\rep ===
pushd "%REPO_ROOT%"
java -cp "%BUILD_DIR%;%CLASSPATH%" tools.jasper.CompilarJasper %*
set RESULTADO=%errorlevel%
popd

exit /b %RESULTADO%

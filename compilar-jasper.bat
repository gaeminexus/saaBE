@echo off
REM Compila todos los .jrxml de src\main\resources\rep\** a .jasper, fuera de WildFly.
REM Ver docs\logica-negocio\tsr\DISENO-REPORTES-CONCILIACION-BANCARIA.md #5 y CLAUDE.md ("Reportes").
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
set /p CLASSPATH=<"%CP_FILE%"

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

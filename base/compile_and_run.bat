@echo off
REM Compile all Java sources into out and run the main class with provided arguments.
REM Usage: compile_and_run.bat [<dblp.xml.gz> <dblp.dtd>] [--limit=1000000]
REM Script generated with IA.
cd /d %~dp0
SETLOCAL ENABLEDELAYEDEXPANSION
set "DEFAULT_XML=dblp-2026-01-01.xml.gz"
set "DEFAULT_DTD=dblp.dtd"
if not exist out mkdir out
javac -d out *.java
if errorlevel 1 (
  echo Compilation failed.
  exit /b 1
)
echo Compilation réussie.
if "%~1"=="" (
  echo Aucun argument fourni, utilisation de %DEFAULT_XML% %DEFAULT_DTD%
  set "ARGS=%DEFAULT_XML% %DEFAULT_DTD%"
) else (
  set "ARGS=%*"
)
java -Xmx4g -cp out DblpParsingDemo %ARGS%
ENDLOCAL

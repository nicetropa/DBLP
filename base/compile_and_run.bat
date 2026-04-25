@echo off
REM Compile all Java sources into out and run the main class with provided arguments.
REM Usage: compile_and_run.bat [<dblp.xml.gz> <dblp.dtd>] --task=1|2 [--limit=1000000]
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
  goto :runjava
)

if "%~1"=="--task" (
  if "%~2"=="1" (
    echo Aucun XML/DTD fourni, utilisation de %DEFAULT_XML% %DEFAULT_DTD%
    set "remaining="
    set /A skip=2
    for %%A in (%*) do (
      if !skip! EQU 0 (
        set "remaining=!remaining! %%A"
      ) else (
        set /A skip-=1
      )
    )
    set "ARGS=%DEFAULT_XML% %DEFAULT_DTD% --task=1!remaining!"
    goto :runjava
  )
  if "%~2"=="2" (
    echo Aucun XML/DTD fourni, utilisation de %DEFAULT_XML% %DEFAULT_DTD%
    set "remaining="
    set /A skip=2
    for %%A in (%*) do (
      if !skip! EQU 0 (
        set "remaining=!remaining! %%A"
      ) else (
        set /A skip-=1
      )
    )
    set "ARGS=%DEFAULT_XML% %DEFAULT_DTD% --task=2!remaining!"
    goto :runjava
  )
)

if "%~1"=="--task=1" (
  if "%~2"=="" (
    echo Aucun XML/DTD fourni, utilisation de %DEFAULT_XML% %DEFAULT_DTD%
    set "ARGS=%DEFAULT_XML% %DEFAULT_DTD% %*"
  ) else (
    set "ARGS=%*"
  )
  goto :runjava
)

if "%~1"=="--task=2" (
  if "%~2"=="" (
    echo Aucun XML/DTD fourni, utilisation de %DEFAULT_XML% %DEFAULT_DTD%
    set "ARGS=%DEFAULT_XML% %DEFAULT_DTD% %*"
  ) else (
    set "ARGS=%*"
  )
  goto :runjava
)

set "ARGS=%*"

:runjava
java -Xmx4g -cp out DblpParsingDemo %ARGS%
ENDLOCAL

@echo off
set "JAVA_HOME=d:\work\LyttleNametag-main\jdk21_extracted\jdk-21.0.11+10"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/3] Preparando EligiusNametag...
del /Q "run\plugins\EligiusNametag*.jar"
if exist "run\plugins\EligiusNametag\" rmdir /S /Q "run\plugins\EligiusNametag"
copy /Y "build\libs\EligiusNametag-1.5.2.jar" "run\plugins\"

echo [2/3] Verificando PaperMC (Servidor)...
powershell -Command "if (!(Test-Path 'run\paper.jar')) { Invoke-WebRequest -Uri 'https://api.papermc.io/v2/projects/paper/versions/1.21.1/builds/131/downloads/paper-1.21.1-131.jar' -OutFile 'run\paper.jar' }"

echo [3/3] Iniciando el servidor de pruebas en la carpeta run...
cd run
java -Xmx4G -Xms4G -jar paper.jar nogui

@echo off
set "JAVA_HOME=d:\work\LyttleNametag-main\jdk21_extracted\jdk-21.0.11+10"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/4] Preparando EligiusNametag...
copy /Y "build\libs\EligiusNametag-1.3.1.jar" "run\plugins\EligiusNametag-1.3.1.jar"

echo [2/4] Verificando ProtocolLib...
powershell -Command "if (!(Test-Path 'run\plugins\ProtocolLib.jar')) { Invoke-WebRequest -Uri 'https://github.com/dmulloy2/ProtocolLib/releases/download/5.3.0/ProtocolLib.jar' -OutFile 'run\plugins\ProtocolLib.jar' }"

echo [3/4] Verificando PaperMC (Servidor)...
powershell -Command "if (!(Test-Path 'run\paper.jar')) { Invoke-WebRequest -Uri 'https://api.papermc.io/v2/projects/paper/versions/1.21.1/builds/131/downloads/paper-1.21.1-131.jar' -OutFile 'run\paper.jar' }"

echo [4/4] Iniciando el servidor de pruebas en la carpeta run...
cd run
java -Xmx4G -Xms4G -jar paper.jar nogui

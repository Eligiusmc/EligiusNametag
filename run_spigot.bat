@echo off

if not exist "run_spigot" mkdir run_spigot
if not exist "run_spigot\plugins" mkdir run_spigot\plugins

echo [1/3] Preparando EligiusNametag para Spigot...
del /Q "run_spigot\plugins\EligiusNametag*.jar"
if exist "run_spigot\plugins\EligiusNametag\" rmdir /S /Q "run_spigot\plugins\EligiusNametag"
copy /Y "build\libs\EligiusNametag-1.5.2.jar" "run_spigot\plugins\"

echo [2/3] Verificando Spigot (Servidor)...
powershell -Command "if (!(Test-Path 'run_spigot\spigot.jar')) { Write-Host 'Descargando Spigot 1.21.1 (esto tomara un momento)...'; Invoke-WebRequest -Uri 'https://download.getbukkit.org/spigot/spigot-1.21.1.jar' -OutFile 'run_spigot\spigot.jar' }"

echo [3/3] Iniciando el servidor de pruebas SPIGOT en la carpeta run_spigot...
cd run_spigot
java -Xmx4G -Xms4G -jar spigot.jar nogui

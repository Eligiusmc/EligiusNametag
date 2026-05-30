@echo off
echo ====================================================================
echo  EligiusNametag - GitHub Repo Settings Configurator
echo ====================================================================
echo.
echo Asegurate de haber ejecutado 'gh auth login' antes de correr esto.
echo Configurando permisos usando la API de GitHub CLI...
echo.

echo 1. Otorgando permisos de "Read/Write" a los GitHub Actions (Para crear Releases)...
gh api -X PUT /repos/Eligiusmc/EligiusNametag/actions/permissions/workflow -f default_workflow_permissions="write" -F can_approve_pull_request_reviews=true

echo 2. Habilitando GitHub Pages para recibir subidas desde GitHub Actions...
gh api --method POST -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28" /repos/Eligiusmc/EligiusNametag/pages -f build_type="workflow"

echo.
echo ====================================================================
echo Configuracion Terminada. Si no viste errores rojos de GH, esta todo listo.
echo Puedes cerrar esta ventana.
pause

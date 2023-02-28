@echo off

if exist eula.txt (
    goto :run
) else (
    set /p accept_eula=Do you accept the Minecraft EULA? (Y/N)
    if /i "%accept_eula%"=="Y" (
        echo eula=true > eula.txt
    ) else (
        echo You must accept the Minecraft EULA to continue.
        pause
        exit
    )
)

:run
echo Starting...
%ARGUMENTSTEMPLATE%
pause
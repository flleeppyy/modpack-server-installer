@echo off && doskey #=REM
cls

# Check if eula.txt exists
if exist eula.txt (
    echo "eula.txt found"
) else (
    echo "eula.txt not found"
    # Ask the user if they accept the Mojang EULA
    echo "Do you accept the Mojang EULA (https://account.mojang.com/documents/minecraft_eula) (y/n)"
    set answer=%~s0
    if "%answer%" == "y" (
        echo "eula.txt created"
        echo "eula=true" > eula.txt
    ) else (
        echo "eula.txt not created"
        echo "Please accept the EULA before running the game"
        exit /b 1
    )
)
%ARGUMENTSTEMPLATE%
pause
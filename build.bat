@echo off
setlocal
cd /d "%~dp0"
if exist out\classes rmdir /s /q out\classes
mkdir out\classes
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out\classes @sources.txt
if errorlevel 1 exit /b 1
echo Build success.
echo Run with: run.bat

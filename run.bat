@echo off
setlocal
cd /d "%~dp0"
if not exist out\classes call build.bat
java -cp out\classes com.familyledger.LedgerApplication 8080

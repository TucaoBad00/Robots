@echo off
rem Compila e executa o projeto (requer JDK 21+ no PATH)
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out -sourcepath src src\steering\engine\Window.java
if errorlevel 1 exit /b 1
java -cp out steering.engine.Window

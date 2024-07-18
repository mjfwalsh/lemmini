@echo off

SET dependencies=good

where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
	echo Java not found
	SET dependencies=bad
) ELSE (
	echo Found Java
)

where javac >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
	echo Java compiler not found
	SET dependencies=bad
) ELSE (
	echo Found Java compiler
)

where jar >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
	echo Jar maker not found
	SET dependencies=bad
) ELSE (
	echo Found jar maker
)

IF %dependencies% == bad (
	ECHO Dependancies failed
	ECHO You may need to check your PATH
	EXIT /B
)

IF EXIST build RD /S /Q build
IF EXIST Lemmini.jar DEL Lemmini.jar

IF EXIST build (
	ECHO Failed to delete build directory
	EXIT /B
)

IF EXIST Lemmini.jar (
	ECHO Failed to delete existing jar file
	EXIT /B
)

MKDIR build

IF NOT EXIST build (
	ECHO Failed to create build directory
	EXIT /B
)

ECHO Running Java Compiler...
javac -d ./build Lemmini.java
if %ERRORLEVEL% NEQ 0 (
	ECHO failed
	EXIT /B
)
echo done

ECHO Making Jar File...
jar cfe Lemmini.jar Lemmini -C build . -C . background.gif crc.ini disclaimer.htm extract.ini lemmini.png LemminiIcon.png patch
if %ERRORLEVEL% NEQ 0 (
	ECHO failed
	EXIT /B
)
echo done

echo Completed!
pause

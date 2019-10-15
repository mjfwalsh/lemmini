@echo off

SET dependencies=good

where perl >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
	echo Perl not found
	SET dependencies=bad
)

where javac >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
	echo Javac not found
	SET dependencies=bad
)

where jar >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
	echo Jar maker not found
	SET dependencies=bad
) 

IF %dependencies% == bad (
	ECHO Dependancies failed
	ECHO You may need to check your PATH
	EXIT /B
)

perl update_compile_time.pl
if %ERRORLEVEL% NEQ 0 EXIT /B

perl preprocessor.pl generic
if %ERRORLEVEL% NEQ 0 EXIT /B

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
javac -encoding UTF8 -d ./build Lemmini.java
if %ERRORLEVEL% NEQ 0 (
	ECHO failed
	EXIT /B
)
echo done

ECHO Making Jar File...
jar cfe Lemmini.jar Lemmini -C build . -C . background.gif crc.ini disclaimer.htm extract.ini lemmini.png patch
if %ERRORLEVEL% NEQ 0 (
	ECHO failed
	EXIT /B
)
echo done


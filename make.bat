@echo off

SET dependencies=good
SET /A PERLORJS=0

where perl >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
	echo Perl not found
) ELSE (
	echo Found Perl
	set /A PERLORJS=PERLORJS+1
)

where cscript >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
	echo JavaScript not found
) ELSE (
	echo Found JavaScript
	set /A PERLORJS=PERLORJS+10
)

if %PERLORJS% == 0 (
	SET dependencies=bad
)

where javac >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
	echo Javac not found
	SET dependencies=bad
) ELSE (
	echo Found Java Compiler
)

where jar >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
	echo Jar maker not found
	SET dependencies=bad
) ELSE (
	echo Found Jar Maker
)

IF %dependencies% == bad (
	ECHO Dependancies failed
	ECHO You may need to check your PATH
	EXIT /B
)

if %PERLORJS% GTR 5 (
	ECHO Running JavaScript Preprocessor...
	cscript /nologo preprocessor.js
	if %ERRORLEVEL% NEQ 0 EXIT /B
) ELSE (
	ECHO Running Perl Preprocessor...
	perl preprocessor.pl generic
	if %ERRORLEVEL% NEQ 0 EXIT /B
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
javac -encoding UTF8 -d ./build Lemmini.java
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
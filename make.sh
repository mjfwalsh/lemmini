#!/bin/sh

#  Copyright 2019 Michael J. Walsh
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# Functions

compile_java()
{
	if [ -d build ]; then
		rm -fR build
	fi

	echo Running Java Compiler...
	mkdir -p build && javac -encoding UTF8 -d ./build Lemmini.java

	if [ "$?" != "0" ]; then
		echo Build Failed
		exit 1
	fi
}

make_jar()
{
	if [ -f Lemmini.jar ]; then
		rm Lemmini.jar
	fi

	if ! [ -d build ]; then
		compile_java
	fi

	echo Making Jar File...
	jar cfe Lemmini.jar Lemmini -C build . -C . background.gif crc.ini disclaimer.htm extract.ini lemmini.png LemminiIcon.png patch

	if [ "$?" != "0" ]; then
		echo Failed to make jar
		exit 1
	fi
}

run_java()
{
	if ! [ -d build ]; then
		compile_java
	fi

	echo Running Java Code...
	java -cp ".:build" Lemmini
}

make_mac_app()
{
	if [ -d Lemmini.app ]; then
		rm -fR Lemmini.app
	fi

	if [ -d tmpapp ]; then
		rm -fR tmpapp
	fi

	if ! [ -f Lemmini.jar ]; then
		if ! [ -f build/Lemmini.class ]; then
			compile_java
		fi
		make_jar
	fi

	echo Making Mac App...

	mkdir -p tmpapp/Contents/MacOS
	mkdir tmpapp/Contents/Resources
	cp Info.plist tmpapp/Contents/
	cp Lemmini.icns tmpapp/Contents/Resources/
	mv Lemmini.jar tmpapp/Contents/Resources/

	cat <<'EOF' > tmpapp/Contents/MacOS/Lemmini
#!/bin/sh
FOO=`dirname "$0"`
cd "$FOO/../Resources"
export JAVA_HOME=`/usr/libexec/java_home -Fv 1.7 2> /dev/null`
if [ "$JAVA_HOME" = "" ]; then
	exec osascript -e 'display alert "Lemmini needs Java 1.7 to work" as critical buttons {"Ok"}'
else
	exec java "-Xdock:icon=Lemmini.icns" -jar Lemmini.jar
fi
EOF

	chmod 755 tmpapp/Contents/MacOS/Lemmini

	mv tmpapp Lemmini.app
}

install_mac_app()
{
	if ! [ -d Lemmini.app ]; then
		make_mac_app
	fi

	echo Installing Mac App...
	mv Lemmini.app /Applications/ || echo Failed to move app to /Applications/
}

make_clean()
{
	echo Cleaning Install directory...
	if [ -d build ]; then rm -fR build; fi
	if [ -f Lemmini.jar ]; then rm Lemmini.jar; fi
	if [ -d Lemmini.app ]; then rm -fR Lemmini.app; fi
	if [ -d tmpapp ]; then rm -fR tmpapp; fi
}

print_usage()
{
		cat << EOF

Usage: ./make.sh command

All commands complete any required earlier steps, with the effect that a
./make install will compile the java code, make a jar file, put it in an
app wrapper and move the app to the Applications folder.

Main Commands:
compile     Compile the java code
jar         Make a jar file
clean       Delete the files and folders creeated by this script (if any)
            Non including the installed application.

Mac Commands:
app         Make a simple mac app wrapper for the jar
install     Move the app to the /Applications folder

Other Commands:
test        Delete the existing build, compile and run
run         Compile (if necessary) and run

EOF

}

# main
system=`uname`

if [ "$#" = "0" ]; then
	print_usage
	exit 0
fi

if [ "$1" = "run" ]; then
	run_java
elif [ "$1" = "jar" ]; then
	make_jar
elif [ "$1" = "app" ]; then
	if [ "$system" = "Darwin" ]; then
		make_mac_app
	else
		echo This function is only supported on Mac
	fi
elif [ "$1" = "compile" ]; then
	compile_java
elif [ "$1" = "test" ]; then
	compile_java
	run_java
elif [ "$1" = "clean" ]; then
	make_clean
elif [ "$1" = "install" ]; then
	if [ "$system" = "Darwin" ]; then
		install_mac_app
	else
		echo This function is only supported on Mac
	fi
else
	print_usage
fi



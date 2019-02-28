#!/bin/sh

#  Copyright 2018 Michael J. Walsh
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

	# Updating Compile Datastamp...
	./update_compile_time.pl

	echo Running Java Compiler...
	mkdir -p build && javac -d ./build Lemmini.java

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
	jar cfe Lemmini.jar Lemmini -C build . -C . background.gif crc.ini disclaimer.htm extract.ini lemmini.png patch

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

	echo \#\!/bin/sh > tmpapp/Contents/MacOS/Lemmini
	echo FOO=\`dirname \"\$0\"\` >> tmpapp/Contents/MacOS/Lemmini
	echo cd \"\$FOO/../Resources\" >> tmpapp/Contents/MacOS/Lemmini
	echo java \"-Xdock:icon=\$FOO/../Resources/Lemmini.icns\" -jar Lemmini.jar >> tmpapp/Contents/MacOS/Lemmini
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

Usage: ./make.sh command [nolook]

All commands complete any required earlier steps, with the effect that a
./make install will compile the java code, make a jar file, put it in an
app wrapper and move the app to the Applications folder.

Lemmini can only compile under the Java 1.7 JDK at the moment. By default this
script will look for the Java 1.7 JDK in the /Library/Java/JavaVirtualMachines/
directory. To disable this give the second command as nolook, and set the
JAVA_HOME environmental variable yourself.

Main Commands:
compile     Compile the java code
jar         Make a jar file
app         Make a simple mac app wrapper for the jar
install     Move the app to the /Applications folder
clean       Delete the files and folders creeated by this script (if any)
            Non including the installed application.

Other Commands:
test        Delete the existing build, compile and run
run         Compile and run without making a jar

To uninstall completely delete to following:
/Applications/Lemmini.app
~/Library/Application Support/Lemmini

EOF

}

# main

if [ "$#" = "0" ]; then
	print_usage
	exit 0
fi

# Find the most recent version of the 1.7 JDK.
# And yes I do know I shouldn't use ls in scripts but
# it just seems a bit pointless here.

if ! [ "$2" = "nolook" ]; then
	export JAVA_HOME=`ls -1d /Library/Java/JavaVirtualMachines/jdk1.7*.jdk/Contents/Home 2> /dev/null | sort -Vr | head -n 1`

	if [ "$JAVA_HOME" = ""  ]; then
		echo Can\'t find Java 1.7 jdk
		exit
	fi
fi


if [ "$1" = "run" ]; then
	run_java
elif [ "$1" = "jar" ]; then
	make_jar
elif [ "$1" = "app" ]; then
	make_mac_app
elif [ "$1" = "test" ]; then
	compile_java
	run_java
elif [ "$1" = "clean" ]; then
	make_clean
elif [ "$1" = "install" ]; then
	install_mac_app
else
	print_usage
fi



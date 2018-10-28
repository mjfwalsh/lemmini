#!/bin/sh
rm -fR build
mkdir -p build
javac -d ./build Lemmini.java

if [ "$?" = "0" ]
then
	if [ "$1" = "run" ]
	then
		java Lemmini
		java -cp ".:build" Lemmini
	else
		jar cvfe Lemmini.jar Lemmini -C build . -C . background.gif crc.ini disclaimer.htm extract.ini icon_32.png lemmini.png patch
	fi
fi



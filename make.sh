#!/bin/sh
rm -fR build
mkdir -p build
javac -d ./build Lemmini.java

if [ "$?" = "0" ]
then
	cd build

	ln -s ../patch patch
	ln ../background.gif background.gif
	ln ../crc.ini crc.ini
	ln ../disclaimer.htm disclaimer.htm
	ln ../extract.ini extract.ini
	ln ../icon_32.png icon_32.png
	ln ../lemmini.png lemmini.png

	if [ "$1" = "run" ]
	then
		java Lemmini
	else
		jar -cvmf ../Manifest.txt ../Lemmini.jar *
	fi
fi



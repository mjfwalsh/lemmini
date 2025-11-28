#!/bin/sh

#  Copyright 2025 Michael J. Walsh
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

set -e

compile_java()
{
    echo 'Running Java Compiler...'

    if ! mkdir -p build || ! javac -d ./build Lemmini.java
    then
        echo 'Build Failed'
        exit 1
    fi
}

make_jar()
{
    rm -f Lemmini.jar

    if ! test -d build
    then
        compile_java
    fi

    echo 'Making Jar File...'

    if ! jar cfe Lemmini.jar Lemmini -C build . -C . background.gif crc.ini \
            disclaimer.htm extract.ini lemmini.png LemminiIcon.png patch
    then
        echo 'Failed to make jar'
        exit 1
    fi
}

run_java()
{
    if ! test -d build
    then
        compile_java
    fi

    echo 'Running Java Code...'
    java -cp ".:build" Lemmini
}

make_mac_app()
{
    rm -fR Lemmini.app tmpapp

    if ! test -f Lemmini.jar
    then
        if ! test -f build/Lemmini.class
        then
            compile_java
        fi
        make_jar
    fi

    echo 'Making Mac App...'

    mkdir -p tmpapp/Contents/MacOS
    mkdir tmpapp/Contents/Resources
    cp Info.plist tmpapp/Contents/
    cp Lemmini.icns tmpapp/Contents/Resources/
    mv Lemmini.jar tmpapp/Contents/Resources/

    cat <<'EOF' > tmpapp/Contents/MacOS/Lemmini
#!/bin/sh

macos_dir=$(dirname "$0")

cd "$macos_dir/../Resources" || exit 1

msg=$(java "-Xdock:icon=Lemmini.icns" -jar Lemmini.jar 2>&1)
if test $? -eq 0
then
    exit 0
fi

osascript - "$msg" <<'EOS'
on run argv
    display alert (item 1 of argv) as critical buttons {"Ok"}
end run
EOS

exit 1

EOF

    chmod 755 tmpapp/Contents/MacOS/Lemmini
    mv tmpapp Lemmini.app
}

install_mac_app()
{
    if ! test -d Lemmini.app
    then
        make_mac_app
    fi

    echo 'Installing Mac App...'
    mv Lemmini.app /Applications/ || echo 'Failed to move app to /Applications/'
}

make_clean()
{
    echo 'Cleaning Install directory...'
    rm -fr build Lemmini.jar Lemmini.app tmpapp
}

mac_only_func()
{
    if test "$(uname)" != "Darwin"
    then
        echo 'This function is only supported on Mac'
        exit 1
    fi
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

case "$1" in
    run)
        run_java
        ;;

    jar)
        make_jar
        ;;

    app)
        mac_only_func
        make_mac_app
        ;;

    compile)
        compile_java
        ;;

    test)
        compile_java
        run_java
        ;;

    clean)
        make_clean
        ;;

    install)
        mac_only_func
        install_mac_app
        ;;

    *)
        print_usage
        ;;
esac

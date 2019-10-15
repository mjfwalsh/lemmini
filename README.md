## Lemmini

This is a fork of Lemmini by Volker Oth (http://lemmini.de). It is released under the Apache License 2.0 and contains source code written by Martin Cameron and Jef Poskanzer.

Lemmini is the attempt to combine the playing sensation of the original Amiga version of Lemmings with the improved graphics of the Win95 port to create the best possible Lemmings experience on all platforms that support a Java virtual machine.

The main changes in this fork are that it supports:
* fullscreen mode including native fullscreen on Mac, and
* granular window resizing (Mac only).

It also also adds the following keyboard shortcuts:
* `Esc` to exit fullscreen mode
* `r` to restart a level

### Compelling from source

#### Mac

You will need a copy of Mac Java 7 in order for the native fullscreen to work. As far as I know it came pre-installed with Mac 10.14 Mojave, but not with 10.15 Catalina.

To compile a jar just `cd` into the base directory and run `./make.sh jar`. It can also produce a simple application wrapper for the jar and place it in the Applications folder: run `./make.sh install`.

The Mac version expects to find the resource folder at `~/Library/Application Support/Lemmini` and the ini file inside. If you wish to migrate, move the files before starting the app.

#### Windows

It should compile with either Java 7 or 8. You'll also need Perl for the compilation scripts (I'm difficult that way).

Double click the `make.bat` script to make the jar.

#### Unix/Linux

The following will probably work.

```
./preprocessor.pl generic
./make.sh jar
```

### Copyright

Copyright (c) Volker Oth 2005-2017. With modifications by Michael J. Walsh 2018-2019. The source code is licensed under the Apache License 2.0.

The Lemmini official website, along with Volker Oth's build and links to the original source code are at: http://lemmini.de

MicroMod and GifEncoder are both released under Three-Clause BSD Licenses:

GifEncoder Library: Copyright (C)1996,1998 by Jef Poskanzer <jef@mail.acme.com>. All rights reserved.

MicroMod: Copyright (c) 2018, Martin Cameron. All rights reserved.
https://github.com/martincameron
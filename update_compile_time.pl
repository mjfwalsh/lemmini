#!/usr/bin/perl

use POSIX qw(strftime);

my $data_stamp = strftime("%-d %B %Y", localtime(time));

print "Updating Compile Datastamp to $data_stamp...";

open(IN, '<', 'Game/Core.java') || fail();
$txt = join '', <IN>;
close IN;

$txt =~ s|private static String releaseString *= *"[^"]*";|private static String releaseString = "Compiled on $data_stamp";| || fail();

open(OUT, '>', 'Game/Core.java') || fail();
print OUT $txt;
close OUT;
print "done.\n";


sub fail {
	print "failed.\n";
	exit;
}
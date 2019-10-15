#!/usr/bin/perl

use Time::localtime;
use strict;

print "Updating Compile Datastamp...";

my @months = qw|January February March April May June July August September October November December|;

my $t = localtime(time);

my $data_stamp = $t->mday . ' ' . $months[$t->mon] . ' ' . ($t->year + 1900);

open(my $IN, '<', 'Game/Core.java') || fail();
my $txt = join '', <$IN>;
close $IN;

$txt =~ s/private static String releaseString *= *"([^"]*)";/private static String releaseString = "Compiled on $data_stamp";/ || fail();

if($1 eq "Compiled on $data_stamp") {
	print "no need\n";
	exit;
}

open(my $OUT, '>', 'Game/Core.java') || fail();
print $OUT $txt;
close $OUT;
print "done.\n";

sub fail {
	print "failed.\n";
	exit;
}
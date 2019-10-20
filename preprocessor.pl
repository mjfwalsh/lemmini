#!/usr/bin/perl

use strict;

# A very simple preprocessor for java source code
# It changes the source code in situ, commenting out some code

my $system;
my $enable;
my $disable;

if($#ARGV == -1) {
	$system = $^O eq 'darwin' ? 'mac' : 'generic';
} elsif($#ARGV == 0 && $ARGV[0] =~ m/^(mac|generic)$/) {
	$system = $ARGV[0];
} else {
	die "Usage: preprocessor.pl (mac|generic|)\n";
}

if($system eq 'mac') {
	$enable = 'MAC';
	$disable  = 'NOT-MAC';
} else {
	$enable = 'NOT-MAC';
	$disable  = 'MAC';
}

# At the moment these are the only files that need preprocessing
my @files = ('Lemmini.java',
'Game/Core.java',
'Game/GraphicsPane.java',
'Extract/Extract.java',
'Extract/FolderDialog.java');

foreach my $file (@files) {
	open(my $IN, '<', $file);
	my $txt = join '', <$IN>;
	close $IN;

	$txt =~ s|[\*/]+IF-$enable[\*/]*|//IF-$enable|g;
	$txt =~ s|[\*/]+END-$enable[\*/]*|//END-$enable|g;

	$txt =~ s|[\*/]+IF-$disable[\*/]*|/\*IF-$disable|g;
	$txt =~ s|[\*/]+END-$disable[\*/]*|//END-$disable\*/|g;

	$txt =~ s|[\*/]+ELSE-IF-$disable[\*/]*|/*ELSE-IF-$disable|g;
	$txt =~ s|[\*/]+ELSE-IF-$enable[\*/]*|//ELSE-IF-$enable*/|g;

	open(my $OUT, '>', $file);
	print $OUT $txt;
	close $OUT;
}

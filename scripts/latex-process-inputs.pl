#!/usr/bin/env perl

# This script determines all files that are recursively \input by a given
# LaTeX file.
#
# This script has two modes:
#  1. Inline mode:  Create a single LaTeX file for the document,
#     by inlining "\input" commands.
#     The result is appropriate to be sent to a publisher.
#       latex-process-inputs.pl main-file.tex > onefile.tex
#     Additionally, the body of each comment is removed, to prevent revealing
#     more information than we want.  (Actually, each comment is replaced by
#     an empty comment, to prevent changes in paragraph breaks.)
#  2. List mode: List all the files that are (transitively) "\input".
#     This can be useful for getting a list of source files in a logical order,
#     for example to be used in a Makefile or Ant buildfile (use -antlist).
#       latex-process-inputs.pl -list main-file.tex
#       latex-process-inputs.pl -antlist main-file.tex

my $debug = 0;
# debug = 1;

# Process command-line arguments
my $list_mode = 0;              # boolean indicating list mode vs. inline mode
my $ant_list_mode = 0;
if (@ARGV[0] eq "-list") {
  $list_mode = 1;
  shift @ARGV;
} elsif (@ARGV[0] eq "-list") {
  $list_mode = 1;
  $ant_list_mode = 1;
  shift @ARGV;
}

if (scalar(@ARGV) > 1) {
  die "Supply exactly one file on the command line (got " . scalar(@ARGV) . ": " . join("", @ARGV) . ")";
}
my @files = ($ARGV[0]);

# I ought to abstract this out into a (recursively-called) procedure.
# inputs
my $text = "";
my $line;
while ($line = <>) {

  # kill comments on the line
  $line =~ s/((^|[^\\])%).*?$/$1/;

  while ($line =~ s/\\(verbatim)?input\{([a-z0-9_\.-]+)\}/___TOKEN___/i) {
    my $verbatim_p = ($1 eq "verbatim");
    my $file = "$2";
    if ($file !~ m/\./) {
      $file .= ".tex";
    }
    push @files, $file;
    if ($debug) { print STDERR "INPUT: $file\n"; }
    open(F, "$file") or die("Can't open $file");
    my @lines = <F>;
    close F;
    my $replace = join('', @lines);
    if ($verbatim_p) {
      $replace = "\\begin{verbatim}\n" .
	$replace .
	"\\end{verbatim}\n";
    } else {
      # kill comments
      $replace =~ s/([^\\]%).*?$/$1/gm;
    }
    $line =~ s/___TOKEN___/$replace/;
  }
  $text .= $line;
}

# kill comments
$text =~ s/([^\\]%).*?$/$1/gm;

# Insert the bibliography.
if ((! $list_mode) && ($text =~ /\\bibliography\{.*?\}/)) {
  my $bbl_file = $files[0];
  $bbl_file =~ s/\.tex$/.bbl/;
  open(BBL, "$bbl_file") or die("Run bibtex (didn't find bbl file $bbl_file)");
  my @bib = <BBL>;
  close BBL;
  my $bib = join('', @bib);
  $text =~ s/\\bibliography\{.*?\}/$bib/i;
}

if ($list_mode) {
  for $file (@files) {
    print "$file\n";
  }
} elsif ($list_mode) {
  for $file (@files) {
    print "      <arg value=\"$file\"/>\n";
  }
} else {
  print $text;
}
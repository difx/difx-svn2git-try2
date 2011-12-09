#!/usr/bin/python
# Cormac Reynolds, December 2011: program to fetch 5 days of EOPs around the
# given date and return in format suitable for appending to .v2d file.
# Takes dates in MJD or VEX format.
# EOP data from http://gemini.gsfc.nasa.gov/solve_save/usno_finals.erp
# Leap second data come from: http://gemini.gsfc.nasa.gov/500/oper/solve_apriori_files/ut1ls.dat

import optparse, re, urllib, espressolib, sys

usage = '''%prog <date>
<date> can either be MJD or VEX time'''
parser = optparse.OptionParser(usage=usage, version='%prog ' + '1.0')
(options, args) = parser.parse_args()

if len(args) != 1:
    parser.print_help()
    parser.error("Give a date in MJD or VEX format")


# get target MJD
targetMJD = args[0];
try:
    targetMJD = float(targetMJD)
except:
    # convert from VEX to MJD if necessary
    targetMJD = espressolib.mjd2vex(targetMJD)


targetJD  = targetMJD + 2400000.5;
tai_utc = None;

# dates before June 1979 not valid (earliest EOPs)
if (targetJD < 2444055.5):
    raise Exception('Date too early. Dates before 1980 not valid')

# fetch EOP data
eop_url = "http://gemini.gsfc.nasa.gov/solve_save/usno_finals.erp"
leapsec_url = "http://gemini.gsfc.nasa.gov/500/oper/solve_apriori_files/ut1ls.dat"

print >>sys.stderr, "Fetching EOP data..."
eop_page = urllib.FancyURLopener().open(eop_url).readlines()
print >>sys.stderr, "...got it."

print >>sys.stderr, "Fetching Leap second data..."
leapsec_page = urllib.FancyURLopener().open(leapsec_url).readlines()
print >>sys.stderr, "...got it.\n";

# parse the leap seconds page
for line in leapsec_page:
    linedate = float(line[17:27])
    if linedate > targetJD:
        break
    else:
        tai_utc = float(line[38:49])

if not tai_utc:
    raise Exception("Leap seconds not found! Check your dates")

# parse the eop page
nlines = 0
for line in eop_page:
    nlines += 1
    # skip the first line, which isn't commented for some reason
    if (nlines == 1):
        continue
    # strip comments
    if (line[0] == '#'):
        continue
    # split the line on whitespace and convert to floats
    eop_fields = line.split()
    eop_fields = [float(field) for field in eop_fields]
    # print an EOP line if we're within 3 days of the target day
    if (abs(eop_fields[0] - targetJD) < 3):
        print "EOP %d { xPole=%f yPole=%f tai_utc=%d ut1_utc=%f }" % (eop_fields[0]-2400000.5,eop_fields[1]/10.,eop_fields[2]/10.,tai_utc,tai_utc+eop_fields[3]/1000000.)

print >>sys.stderr, "Processed %d lines" % nlines;

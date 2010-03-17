if WITH_RPFITS
rpfits_programs = RPFitsReader
else
rpfits_programs = 
endif

bin_PROGRAMS = printDiFX $(rpfits_programs)
dist_bin_SCRIPTS = diffDiFX.py plotDiFX.py

AM_CXXFLAGS = $(DIFXMESSAGE_CFLAGS) $(FXCORR_CFLAGS) $(IPP_CFLAGS) $(RPFITS_CFLAGS)
printDiFX_SOURCES = printDiFX.cpp
printDiFX_LDADD = $(DIFXMESSAGE_LIBS) $(FXCORR_LIBS) $(IPP_LIBS)

if WITH_RPFITS
RPFitsReader_SOURCES = RPFitsReader.cpp
RPFitsReader_LDADD = $(FXCORR_LIBS) $(IPP_LIBS) $(RPFITS_LIBS)
endif

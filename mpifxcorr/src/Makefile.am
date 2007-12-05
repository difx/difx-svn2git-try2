if WITH_SS
mark5_files = \
	nativemk5.cpp \
	mark5dir.cpp
else
mark5_files = \
	nativemk5_stubs.cpp
endif

bin_PROGRAMS = mpifxcorr
mpifxcorr_SOURCES = mpifxcorr.cpp fxmanager.cpp core.cpp datastream.cpp \
	visibility.cpp configuration.cpp uvw.cpp mode.cpp mk5.cpp polyco.cpp \
	$(mark5_files)

# set the include path found by configure
INCLUDES = -I../../rpfits/code/ \
	$(all_includes)

# the library search path.
# mpifxcorr_LDFLAGS = $(all_libraries) 
noinst_HEADERS = fxmanager.h core.h datastream.h architecture.h visibility.h \
	configuration.h uvw.h mk5.h polyco.h mark5dir.h nativemk5.h

mpifxcorr_LDADD = \
	-L/opt/intel/ipp/5.1/ia32/sharedlib/ -L../../rpfits/linux/ \
	-lipps -lguide -lippvm -lrpfits -lg2c

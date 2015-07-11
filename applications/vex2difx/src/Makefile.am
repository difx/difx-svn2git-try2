bin_PROGRAMS = \
	vex2difx \
	vexpeek

LDADD = \
	$(top_builddir)/vex/libvex.la

VEX_DATAMODEL_SRC = \
	vex_antenna.cpp \
	vex_antenna.h \
	vex_basebanddata.cpp \
	vex_basebanddata.h \
	vex_channel.cpp \
	vex_channel.h \
	vex_clock.cpp \
	vex_clock.h \
	vex_datastream.cpp \
	vex_datastream.h \
	vex_eop.cpp \
	vex_eop.h \
	vex_exper.h \
	vex_if.cpp \
	vex_if.h \
	vex_mode.cpp \
	vex_mode.h \
	vex_scan.cpp \
	vex_scan.h \
	vex_setup.cpp \
	vex_setup.h \
	vex_source.cpp \
	vex_source.h \
	vex_subband.cpp \
	vex_subband.h \
	vextables.h \
	vextables.cpp

vex2difx_SOURCES = \
	$(VEX_DATAMODEL_SRC) \
	event.cpp \
	event.h \
	vexload.h \
	vexload.cpp \
	corrparams.h \
	corrparams.cpp \
	freq.h \
	freq.cpp \
	interval.h \
	interval.cpp \
	makejobs.h \
	makejobs.cpp \
	mediachange.h \
	mediachange.cpp \
	parserhelp.h \
	parserhelp.cpp \
	sanitycheck.h \
	sanitycheck.cpp \
	timeutils.h \
	timeutils.cpp \
	util.h \
	util.cpp \
	vex2difx.cpp

# eventually remove event.* from below:

vexpeek_SOURCES = \
	event.cpp \
	event.h \
	$(VEX_DATAMODEL_SRC) \
	vexload.cpp \
	vexload.h \
	timeutils.h \
	timeutils.cpp \
	interval.h \
	interval.cpp \
	util.h \
	util.cpp \
	vexpeek.cpp

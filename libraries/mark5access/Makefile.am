if ENABLE_PYTHON_CTYPES
PYTHON_OPT = python
endif

SUBDIRS = \
	mark5access \
	bbsum \
	examples \
	doc \
        $(PYTHON_OPT)

EXTRA_DIST = \
	TODO \
	vlbi-mark5access.spec.in

if DEBUG
AM_CFLAGS = -g3 -O0
AM_CXXFLAGS = -g3 -O0
else
AM_CFLAGS = -O2
AM_CXXFLAGS = -O2
endif

dist-hook: vlbi-mark5access.spec
	cp vlbi-mark5access.spec $(distdir)

library_includedir = $(includedir)
library_include_HEADERS = mark5access.h

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = mark5access.pc

rpm: distdir
	tardir=$(distdir) && $(am__tar) | GZIP=$(GZIP_ENV) gzip -c >$(distdir).tar.gz
	rpmbuild -tb $(distdir).tar.gz
	$(am__remove_distdir)


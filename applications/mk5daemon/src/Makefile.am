if WITH_STREAMSTOR
streamstor_sources = \
	modules.cpp \
	mark5a.cpp \
	smart.cpp \
	smart.h \
	watchdog.cpp \
	watchdog.h \
	ssversions.cpp
else
streamstor_sources = 
endif

bin_PROGRAMS = \
	sukill \
	mk5daemon

mk5daemon_SOURCES = \
	difx.cpp \
	loadmon.cpp \
	logger.cpp \
	logger.h \
	monitor.cpp \
	vsis.cpp \
	vsis_commands.h \
	vsis_commands.cpp \
	getdir.cpp \
	datacopy.cpp \
	condition.cpp \
	proc.cpp \
	proc.h \
	mk5daemon.cpp \
	mk5daemon.h \
	options.cpp \
	mark6.cpp \
	mark6.h \
	Mark6DiskDevice.h \
	Mark6DiskDevice.cpp \
	Mark6Meta.cpp \
	Mark6Meta.h \
	Mark6Module.cpp \
	Mark6Module.h \
	$(streamstor_sources)

mk5daemon_LDADD = -ludev

sukill_SOURCES = \
	proc.cpp \
	proc.h \
	sukill.cpp
	

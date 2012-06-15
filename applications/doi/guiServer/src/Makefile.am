INCLUDES = -I$(top_srcdir)

bin_PROGRAMS = \
	guiServer

# testClient won't get installed.  If this is not desired, add it to the 
# bin_PROGRAMS above instead.
noinst_PROGRAMS = \
	testClient

guiServer_SOURCES = \
	ServerSideConnection.h \
	PacketExchange.h \
	guiServer.cpp \
	startDifx.cpp \
	diagnostic.cpp \
	difxFileTransfer.cpp \
	difxFileOperation.cpp \
	vex2difxRun.cpp \
	machinesDefinition.cpp \
	MachinesMonitorConnection.h \
	JobMonitorConnection.h

testClient_SOURCES = \
	ServerSideConnection.h \
	PacketExchange.h \
	testClient.cpp


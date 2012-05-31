
INCDIRS = -I.

CFLAGS += -g $(INCDIRS)

HFILES = ServerSideConnection.h PacketExchange.h

CFILES = guiServer.cpp startDifx.cpp diagnostic.cpp difxFileTransfer.cpp difxFileOperation.cpp vex2difxRun.cpp

all:  guiServer testClient


guiServer : ${CFILES} ${HFILES}
	g++ ${CFLAGS} -I/usr/local/swc/difx/include ${CFILES} -o guiServer -lexpat -pthread /usr/local/swc/difx/lib/libdifxmessage.a
	
testClient : testClient.cpp ${HFILES}
	g++ ${CFLAGS} testClient.cpp -o testClient -pthread
	
clean : 
	rm -f guiServer testClient *.o *~

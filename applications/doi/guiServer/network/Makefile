
CFLAGS += -g -I..

HFILES = GenericSocket.h TCPSocket.h TCPServer.h TCPClient.h \
         UDPSocket.h

all:  clientTest serverTest udpTest


clientTest : clientTest.cpp ${HFILES}
	g++ ${CFLAGS} clientTest.cpp -o clientTest -pthread

serverTest : serverTest.cpp ${HFILES}
	g++ ${CFLAGS} serverTest.cpp -o serverTest -pthread
	
udpTest : udpTest.cpp $(HFILES)
	g++ $(CFLAGS) udpTest.cpp -o udpTest -pthread

clean : 
	rm -f clientTest serverTest udpTest *.o *~

#include <string>
#include <vector>

#include "mark6diskdevice.h"

/**
 * Custom exception class for reporting Mark6 related errors
 */
class Mark6Exception : public std::exception 
{
private:
    std::string err_msg;

public:
    Mark6Exception(std::string msg) : err_msg(msg) {};
    ~Mark6Exception() throw() {};
    const char *what() const throw() { return this->err_msg.c_str(); };
};

/**
 * Custom exception class for reporting Mark6 related mount errors
 */
class Mark6MountException : public std::exception 
{
private:
    std::string err_msg;

public:
    Mark6MountException(std::string msg) : err_msg(msg) {};
    ~Mark6MountException() throw() {};
    const char *what() const throw() { return this->err_msg.c_str(); };
};


class Mark6
{
private:
	
	int fd;		// file descriptor for the udev monitor
        struct udev *udev_m;
        struct udev_monitor *mon;
	std::vector<Mark6DiskDevice> mountedDevices_m;
	std::vector<std::string> removedDevices_m;
        std::vector<Mark6DiskDevice> newDevices_m;
        std::string mountPath_m;
        std::string slotMSN_m[4];
        void manageDevices();
	void validateMountDevices();
public:
        Mark6();
	~Mark6();
	void pollDevices();
	void validateMountPoints();
	void createMountPoint (std::string path);
        int addPartitionDevice(std::string partitionName);
        bool isMounted(std::string deviceName);
        Mark6DiskDevice *getMountedDevice(std::string deviceName);
        void removeMountedDevice(std::string deviceName);
        void enumerateDevices();
        void cleanUp();
        void createLinks();
        std::vector<Mark6DiskDevice> getMountedDevices() const {
            return mountedDevices_m;
        }

};


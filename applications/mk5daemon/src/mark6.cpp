#include <poll.h>
#include <libudev.h>
#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>
#include <stdexcept>
#include <dirent.h>
#include <sys/stat.h>
#include <errno.h>


#include "mark6.h"
#include "Mark6DiskDevice.h"
#include "Mark6Meta.h"
#include <sys/mount.h>

using namespace std;

/**
 * Constructor
 */
Mark6::Mark6(void)
{
        /* Create the udev object */
        udev_m = udev_new();
        if (!udev_m) {
                throw Mark6Exception (string("Cannot create new udev object.") );
        }
	mon = udev_monitor_new_from_netlink(udev_m, "udev");
        udev_monitor_filter_add_match_subsystem_devtype(mon, "block", NULL);
        udev_monitor_enable_receiving(mon);
        
        // Get the file descriptor (fd) for the monitor. This fd will get passed to select() 
        fd = udev_monitor_get_fd(mon);

	// check if required mount points exists; create them if not        
	validateMountPoints();
        
        cleanUp();
}

/**
 * Destructor
 */
Mark6::~Mark6()
{
	// destroy the udev monitor
	udev_monitor_unref(mon);
	//
	//destroy the udev object
	udev_unref(udev_m);
}

/**
 * Check if a directory exists at the given location. If not it will be created.
 * @param[in] path the full path of he directory to be created
 */
void  Mark6::createMountPoint(string path)
{
	int ret;

	if (opendir(path.c_str()) == NULL)
	{
        	ret = mkdir(path.c_str(), S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH);
		if (ret == 0)
			cout << "created mount point: " << path << endl;
		else if (ret == EACCES)
		{
			throw Mark6Exception (string("no permission to create mount point : " + path) );
		} 
		else
		{
			throw Mark6Exception (string("error creating mount point: " + path ) );
		}			
	}
}

/**
 * Finds the slot associated with module having the given eMSN.
 * NOTE: the slot determined by this function does not neccessarily correspond to the 
 * physical slot in the mark6 machine
 * @param[in] the module eMSN
 * @returns the number of the slot associated with the given eMSN; -1 in case no slot with a matching eMSN can be found
 */
int Mark6::getSlot(string eMSN)
{
    
     for (int iSlot = 0; iSlot < NUMSLOTS; iSlot++) 
        {
            if (modules_m[iSlot].getEMSN() == eMSN)
            {
                return(iSlot);
            }   
        }
     
     return(-1);
}

/**
 * Finds the first slot not containing a mounted module yet
 * NOTE: the slot determined by this function does not neccessarily correspond to the 
 * physical slot in the mark6 machine
 * @returns the number of the first free slot a; -1 in case no free slot is available
 */
int  Mark6::getFreeSlot()
{
    for (int iSlot = 0; iSlot < NUMSLOTS; iSlot++) 
       {
            if (modules_m[iSlot].getEMSN() == "")
            {
                return(iSlot);
            }
    }
    
    return(-1); 
}

/**
 * Loops over all disks in all mounted modules and sets a symbolic link to each mount point at the location expected
 * by the mark6 reading code.
 */
void Mark6::createMountLinks()
{
    // loop over all modules
    for (int slot=0; slot < NUMSLOTS; slot++)
    {
        //loop over all disk devices
        for (int disk=0; disk < modules_m[slot].getNumDiskDevices(); disk++)
        { 
            // check if in list of currently mounted devices 
            if (modules_m[slot].getDiskDevice(disk)->isMounted())
            {
                modules_m[slot].getDiskDevice(disk)->linkDisk("/mnt/disks", slot);
            }
            else
            {
                if (modules_m[slot].getDiskDevice(disk)->unlinkDisk() == EXIT_FAILURE)
                {
                        throw new Mark6Exception("Error removing symbolic links for device " + modules_m[slot].getDiskDevice(disk)->getName());                   
                }
            }
        }
    }
}

/**
 * Manages devices that were added or removed by turning the Mark6 keys. Newly added devices will be mounted; removed devices will be unmounted
 */
void Mark6::manageDevices()
{
    vector<Mark6DiskDevice> tempDevices;
    vector<Mark6DiskDevice> tempRemoveDevices;
    Mark6DiskDevice *disk;
    
    // new devices have been found
    if (newDevices_m.size() > 0)
    {
         tempDevices.swap(newDevices_m);
         
        // validate all new devices
        for(std::vector<Mark6DiskDevice>::size_type i = 0; i != tempDevices.size(); i++) {
       
            // too many partitions for a valid mark6 disk
            if (tempDevices[i].getPartitions().size() > 2)
            {
                cout << "disk " << tempDevices[i].getName() << " has more than 2 partitions. Discarding.";
                continue;
            }
            // if less than 2 partitions keep in list of newdevices  
            else if (tempDevices[i].getPartitions().size() < 2)
            {
                cout << "disk " << tempDevices[i].getName() << " has only 1 partition. Keeping it for now.";
                newDevices_m.push_back(tempDevices[i]);
            }
            else
            {
                createMountPoint(mountPath_m + tempDevices[i].getPartitions()[0].deviceName);
                createMountPoint(mountPath_m + tempDevices[i].getPartitions()[1].deviceName);
                
                tempDevices[i].mountDisk(mountPath_m);
                          
                if (tempDevices[i].isMounted())
                {
                    cout << "mounted " << tempDevices[i].getPartitions()[0].deviceName << " " << tempDevices[i].getPartitions()[1].deviceName << endl;
                    
                    
                    mountedDevices_m.push_back(tempDevices[i]);
                    
                    // check if a disk from the same module has already been mounted
                    int slot = getSlot(tempDevices[i].getMeta().getEMSN());
                    if (slot == -1)
                    {
                        // ...if not get the next free slot
                        if ((slot = getFreeSlot()) == -1)
                        {                 
                            throw new Mark6Exception("Error registering slot for eMSN: " + tempDevices[i].getMeta().getEMSN() + ". All slots already occupied.");
                        }
                    }

                    modules_m[slot].setEMSN(tempDevices[i].getMeta().getEMSN());
                    modules_m[slot].addDiskDevice(tempDevices[i]);
                   
               
                }
                else
                {
                    cout << "mount failed " << tempDevices[i].getPartitions()[0].deviceName << " " << tempDevices[i].getPartitions()[1].deviceName << " will try again" << endl;
                    newDevices_m.push_back(tempDevices[i]);
                }
            } 
        }  
    }
    
    //removed devices
    if (removedDevices_m.size() > 0)
    {
        tempRemoveDevices.swap(removedDevices_m);
        
        // loop over all removed devices
        for(std::vector<Mark6DiskDevice>::size_type i = 0; i != tempRemoveDevices.size(); i++) {
                       
            if ((disk = getMountedDevice(tempRemoveDevices[i].getName())) != NULL)
            {
                // get the module slot           
                int slot = getSlot(disk->getMeta().getEMSN());
                
                // unmount the disk
                disk->unmountDisk(mountPath_m);
                
                if (disk->isMounted() == false)
                {
                    cout << " now is unmounted " << endl;    
                             
                    // remove disk from the vector of mounted devices
                    removeMountedDevice(tempRemoveDevices[i]);
                    cout << "Removed: " << tempRemoveDevices[i].getName() << " in slot: " << slot << endl;
                    
                    if (slot != -1)
                        modules_m[slot].removeDiskDevice(tempRemoveDevices[i]);
                    else
                        throw new Mark6Exception("Trying to remove a disk device that has no associated slot.");
                    
                }
                else
                {
                    // something didn't work try again on the next go
                    removedDevices_m.push_back( tempRemoveDevices[i]);
                    //cout << " now is still mounted ";
                }
            }    
            
        }        
    }
}

/**
 * Loops over all mount points and link locations required to mount the mark6 data and meta partition. A check is
 * performed whether these directories exist. If not they will be created.
*/
void  Mark6::validateMountPoints()
{
        string rootPath = "/mnt/disks/";
        string metaPath = rootPath + ".meta/";
        string tmpRootPath = "/tmp/";
       
	string slots[] = {"1","2","3","4"};
	//string disks[] = {"0","1","2","3","4","5","6","7"};
	//string meta[] = {"0m","1m","2m","3m","4m","5m","6m","7m"};
                
        // create mount points in /tmp
        string path = tmpRootPath + "mark6/";
        createMountPoint(path);
        path += "mnt/";
        createMountPoint(path);
        
        // set the mount path
        mountPath_m = path;
                
        // create directories under /mnt for hosting sybolic links to the mount points 
	createMountPoint(rootPath);
        createMountPoint(metaPath);
      
	for(int i=0; i < 4; i++)
	{
            string path = rootPath + slots[i];
            createMountPoint(rootPath + slots[i]);
            createMountPoint(metaPath + slots[i]);

            /*for(int j=0; j < 8; j++)
            {
                createMountPoint(path + "/" + disks[j]);
                createMountPoint(path + "/" + meta[j]);

            }*/
	}

}

/**
 * Adds a partition with the given name to the corresponding disk device object. 
 * @return 0 in case In case the correspoding disk device does not exist; 1 otherwise
 */
int Mark6::addPartitionDevice(string partitionName)
{    
    // determine corresponding disk device (assuming less than 9 partitions per disk)
    string diskDevice = partitionName.substr(0, partitionName.size()-1);
    
     // check if disk device object already exists for this partition
    for(std::vector<Mark6DiskDevice>::size_type i = 0; i != newDevices_m.size(); i++) {
        if (newDevices_m[i].getName() == diskDevice)
        {
            cout << "Adding partition " << partitionName << " to " << newDevices_m[i].getName() << endl;
            newDevices_m[i].addPartition(partitionName);
            return(1);
        }
    }
    
    return(0);
}

/**
 * Checks whether a disk device with the given device name is currently mounted
 * @return true in case the device is mounted; false otherwise
 */
bool Mark6::isMounted(string deviceName)
{
    // loop over all mounted disks
    for (std::vector<Mark6DiskDevice>::size_type i=0; i < mountedDevices_m.size(); i++)
    {
        if (mountedDevices_m[i].getName() == deviceName)
        {
            return(true);
        }
    }
    
    return(false);
}

/**
 * Check whether the disk device with the given device name is currently mounted and returns it
 * @return the Mark6DiskDevice in case is is currently mounted; NULL otherwise 
 */
Mark6DiskDevice *Mark6::getMountedDevice(string deviceName)
{
    // loop over all mounted disks
    for (std::vector<Mark6DiskDevice>::size_type i=0; i < mountedDevices_m.size(); i++)
    {
        if (mountedDevices_m[i].getName() == deviceName)
        {
            return(&mountedDevices_m[i]);
        }
    }
    
    return(NULL);
}

void Mark6::removeMountedDevice(Mark6DiskDevice device)
{
    
    // loop over all mounted disks
    for( vector<Mark6DiskDevice>::iterator iter = mountedDevices_m.begin(); iter != mountedDevices_m.end(); ++iter )
    {
        if( (*iter).getName() == device.getName() )
        {
            mountedDevices_m.erase( iter );
            break;
        }
    }
}

/**
 * Cleans up any left-over mounts in the temporary mount location (default /tmp/mark6/mnt). Such remining mounts might
 * result from a crash of the mark5daemon or other unexpected events.
 */
void Mark6::cleanUp()
{
    // unmount any remaining devices 
    DIR *pdir = NULL;
    struct dirent *pent = NULL;
    
    
    pdir = opendir (mountPath_m.c_str()); 
    if (pdir == NULL)
    {
        throw new Mark6Exception("Cannot open directory: " + mountPath_m + " for reading ");
    }
	    
    while ((pent = readdir (pdir))) 
    {
        if (pent == NULL)
        { 
            throw new Mark6Exception("Cannot read directory: " + mountPath_m);
        }
        // otherwise, it was initialised correctly. Let's print it on the console:
        
        string dest = mountPath_m + pent->d_name;
        umount2(dest.c_str(), MNT_FORCE);
    }
    
}

void Mark6::enumerateDevices()
{

    struct udev_enumerate *enumerate;
    struct udev_list_entry *devices, *dev_list_entry;
    struct udev_device *dev;
        
    // obtain the list of currently present block devices

    enumerate = udev_enumerate_new(udev_m);
    udev_enumerate_add_match_subsystem(enumerate, "block");
    udev_enumerate_scan_devices(enumerate);
    devices = udev_enumerate_get_list_entry(enumerate);
    
    // loop over list of currently present block devices
    udev_list_entry_foreach(dev_list_entry, devices) 
    {
        const char *path;
        
        path = udev_list_entry_get_name(dev_list_entry);
        
        dev = udev_device_new_from_syspath(udev_m, path);
        
        
        if(!dev)
        {
            // error receiving device, skip it
            continue;
        }
        string devtype(udev_device_get_devtype(dev));
        
        cout << devtype << path <<endl;
    }
    
    udev_enumerate_unref(enumerate);
    
}

/**
 * Look for devices that were added or removed 
 */
void Mark6::pollDevices()
{

	cout << "Polling for new devices" << endl;

	// create the poll item
	pollfd items[1];
	items[0].fd = udev_monitor_get_fd(mon);
	items[0].events = POLLIN;
	items[0].revents = 0;
	int changeCount = 0;

	// while there are hotplug events to process
	while(poll(items, 1, 50) > 0)
	{
		
	       // receive the relevant device
		udev_device* dev = udev_monitor_receive_device(mon);
		if(!dev)
		{
		    // error receiving device, skip it
		    continue;
		}
		string action(udev_device_get_action(dev));
                string devtype(udev_device_get_devtype(dev));

		cout << "hotplug[" << udev_device_get_action(dev) << "] ";
		cout << udev_device_get_devnode(dev) << ",";
		cout << udev_device_get_subsystem(dev) << ",";
		cout << udev_device_get_devtype(dev) << ",";
                cout << endl;
/*
		cout << " devpath="<< udev_device_get_devpath(dev) << endl;
		cout << " syspath="<< udev_device_get_syspath(dev) << endl;
		cout << " sysname="<< udev_device_get_sysname(dev) << endl;
		cout << " devnode="<< udev_device_get_devnode(dev) << endl;
		cout << " devnum="<< udev_device_get_devnum(dev) << endl;
		
		udev_device  *parent = udev_device_get_parent(dev);
                cout << "parent ";
		cout << " subsystem = " << udev_device_get_subsystem(parent) ;
                cout << " devtype = " << udev_device_get_devtype(parent);
                cout << " devpath="<< udev_device_get_devpath(parent) << endl;
		cout << " syspath="<< udev_device_get_syspath(parent) << endl;
		cout << " sysname="<< udev_device_get_sysname(parent) << endl;
                cout << " devnum = " << udev_device_get_devnum(parent) << endl;
                cout << endl;
*/
		if (action ==  "add")
		{
                    if (devtype == "disk")
                    {
                        // add new disk device
                        Mark6DiskDevice disk(string(udev_device_get_sysname(dev)));
                        newDevices_m.push_back(disk);			
                        changeCount++;
                    }
                    else if (devtype == "partition")
                    {   
                        string partitionName = string(udev_device_get_sysname(dev));
                        // add partition to disk device object
                        if (addPartitionDevice(partitionName) == 0)
                        {
                            throw Mark6Exception (string("New hotplug partition found (" + partitionName + ") without corresponding disk device") );              
                        }
                    }
		}
		else if ((action ==  "remove") && (devtype == "disk"))
		{
			//cout << "remove device action" << endl;
                        Mark6DiskDevice disk(string(udev_device_get_sysname(dev)));
			removedDevices_m.push_back(disk);
			changeCount++;
		}

		// destroy the relevant device
		udev_device_unref(dev);

		// clear the revents
		items[0].revents = 0;
	}

	if (changeCount > 0)
	{
		manageDevices();
                
                createMountLinks();
                
                cout << "Currently mounted devices:" << endl;
                for (std::vector<Mark6DiskDevice>::size_type i=0; i < mountedDevices_m.size(); i++)
                {
                    cout << mountedDevices_m[i].getName() << " " << mountedDevices_m[i].getMeta().getEMSN() << " " << endl;
                }
                cout << endl;
                
                cout << "Currently mounted modules:" << endl;
                for (int iSlot=0; iSlot < 4; iSlot++)
                {
                    cout << "Slot " << iSlot << " = " << modules_m[iSlot].getEMSN() << " (" << modules_m[iSlot].getNumDiskDevices() << " disks)" << endl;
                }
                cout << endl;
	}
        
        //enumerateDevices();
}




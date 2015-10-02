/* 
 * File:   Mark5DiskDevice.cpp
 * Author: oper
 * 
 * Created on 28. September 2015, 14:33
 */

#include "mark6diskdevice.h"
#include "mark6.h"
#include "Mark6Meta.h"
#include <algorithm>
#include <sys/mount.h>
#include <iostream>

using namespace std;

/**
 * Constructor
 * @param deviceName the device name of the disk
 */
Mark6DiskDevice::Mark6DiskDevice(string deviceName) {
    name_m = deviceName;
    isMounted_m = false;
    mountPath_m = "";
    fsType_m = "xfs";
}

vector<std::string> Mark6DiskDevice::getPartitions() const {
    return partitions_m;
}

/**
 * Returns the device name of the disk
 * @return 
 */
std::string Mark6DiskDevice::getName() const {
    return name_m;
}

/**
 * Checks whether the partition with the given name already exists. If not it will be added
 * @param partitionName the name of the partition to add
 */
void Mark6DiskDevice::addPartition(std::string partitionName)
{
     for(vector<string>::size_type i = 0; i != partitions_m.size(); i++) {
         // check if partition already exists
         if (partitions_m[i] == partitionName)
             return;
     }
     
     partitions_m.push_back(partitionName);
     
     // finally sort the partitions
     sort(partitions_m.begin(), partitions_m.end());
}

/**
 * Mounts both partitions of this disk device. The partitions will be mounted under the given mount path plus the device name.
 * e.g. partition /dev/sdb1 will mounted under /mnt/mark6/mnt/sdb1 
 * @param[in] mountPath the path uder which the partitions will be mounted
 * @throws Mark6MountException in case the device cannot be mounted
 * @returns 1 if both partitions were mounted successfully, 0 otherwise
 */
int Mark6DiskDevice::mountDisk(string mountPath)
{
    string source = "";
    string dest = "";
    
    // verify that this disk has two partitions
    if (partitions_m.size() != 2)
        return(0);
    
    // mount both partitions
    for (int i=0; i<2; i++)
    {
        source = "/dev/" + partitions_m[i];
        dest =  mountPath + partitions_m[i];
        
        int ret = mount(source.c_str(), dest.c_str(), fsType_m.c_str(), MS_MGC_VAL | MS_RDONLY , "");
    
        if (ret == -1)
        {
            isMounted_m = false;
            throw Mark6MountException (string("Cannot mount  device " + source + " under " + dest));
        }
    }
   
    // now read metadata
    meta_m.parse(dest);
    
    //eMSN_m = meta_m.getEMSN();
    
    //cout << "test: " << meta_m.getEMSN() << endl;
    
    
    isMounted_m = true;
    
    return(1);
}

void Mark6DiskDevice::unmountDisk(string mountPath)
{
    for (int i=0; i<2; i++)
    {
        string dest =  mountPath + partitions_m[i];
        
        int ret = umount2(dest.c_str(), MNT_FORCE);
    
        if (ret == -1)
        {
            throw Mark6MountException (string("Cannot unmount  device " + dest ));
        }
    }
    
    isMounted_m = false;
}

/**
 *
 * @return true if this disk device is mounted; false otherwise
 */
bool Mark6DiskDevice::isMounted()
{
    return(isMounted_m);
}

void Mark6DiskDevice::setFsType(std::string fsType_m) {
    this->fsType_m = fsType_m;
}

std::string Mark6DiskDevice::getFsType() const {
    return fsType_m;
}

Mark6Meta Mark6DiskDevice::getMeta() const {
    return meta_m;
}

void Mark6DiskDevice::setDiskId(int diskId_m) {
    this->diskId_m = diskId_m;
}

int Mark6DiskDevice::getDiskId() const {
    return diskId_m;
}

void Mark6DiskDevice::setSlotId(int slotId_m) {
    this->slotId_m = slotId_m;
}

int Mark6DiskDevice::getSlotId() const {
    return slotId_m;
}

/*std::string Mark6DiskDevice::getEMSN() const {
    return eMSN_m;
}*/

/**
 * Destructor
 */
Mark6DiskDevice::~Mark6DiskDevice() {
}


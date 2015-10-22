/* 
 * File:   Mark5DiskDevice.cpp
 * Author: oper
 * 
 * Created on 28. September 2015, 14:33
 */

#include "Mark6DiskDevice.h"
#include "mark6.h"
#include "Mark6Meta.h"

#include <algorithm>
#include <iostream>
#include <sstream>
#include <string>
#include <sys/mount.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>

using namespace std;

/**
 * Constructor
 * @param[in] deviceName the device name of the disk
 */
Mark6DiskDevice::Mark6DiskDevice(string deviceName) {
    name_m = deviceName;
    isMounted_m = false;
   // mountPath_m = "";
    fsType_m = "xfs";
    diskId_m = -1;
    slotId_m = -1;
}

vector<Mark6DiskDevice::Mark6Partition> Mark6DiskDevice::getPartitions() const {
    return partitions_m;
}

/**
 * @return the device name of the disk
 */
std::string Mark6DiskDevice::getName() const {
    return name_m;
}



/**
 * Checks whether the partition with the given name already exists. If not it will be added
 * @param[in] partitionName the name of the partition to add
 */
void Mark6DiskDevice::addPartition(std::string partitionName)
{
     for(vector<string>::size_type i = 0; i != partitions_m.size(); i++) {
         // check if partition already exists
         if (partitions_m[i].deviceName == partitionName)
             return;
     }
     
     Mark6Partition partition;
     partition.deviceName = partitionName;
     partition.mountPath = "";
     partition.linkPath = "";
     
     partitions_m.push_back(partition);
     
     // finally sort the partitions
     sort(partitions_m.begin(), partitions_m.end(), Mark6Partition::sortByName);
}

/**
 * Removes the symbolic links to the mount locations of the disk device. 
 * * The following logic is applied:
 * - the first partition is assumed to contain the mark6 data. It is linked under linkPath/slot/disk
 * - the second partition is assumed to contain the meta data. It is linked under linkPath/.meta/slot/disk
 * where slot runs from 1-4, and disk runs from 0-7
 * @return EXIT_SUCCESS in case of success, EXIT_FAILURE otherwise
 */
int Mark6DiskDevice::unlinkDisk()
{     
    int errorCount = 0;
    
    // loop over partitions
    for (unsigned int i=0; i < partitions_m.size(); i++)
    {
        
        struct stat file;
        string linkPath = partitions_m[i].linkPath;
        
        cout << "trying to unlink partition " << i << " on disk " << name_m << " " << linkPath << endl;
        
        // check if partition is linked
        if (linkPath == "")
            continue;
        
        // check if this is really a symbolic link    
        lstat(linkPath.c_str(), &file);
        if (!S_ISLNK(file.st_mode))
        {
            errorCount++;
        }
        
        if( remove( linkPath.c_str() ) != 0 )
        {
            throw new Mark6Exception("Cannot remove symbolic link: " + linkPath);
        }
        cout << " removed symbolic link " << linkPath << endl;
        partitions_m[i].linkPath = "";     
    }
    
    if (errorCount == 0)
        return(EXIT_SUCCESS);
    else
        return(EXIT_FAILURE);
}

/**
 * Creates symbolic links for all partitions on the disk device. The symbolic link is created under the given linkPath
 * and points to the mount point of the associated partition device. 
 * The following logic is applied:
 * - the first partition is assumed to contain the mark6 data. It is linked under linkPath/slot/disk
 * - the second partition is assumed to contain the meta data. It is linked under linkPath/.meta/slot/disk
 * where slot runs from 1-4, and disk runs from 0-7
 * @param[in] linkPath the full path of the root directory under which the symbolic links are to be created
 * @param[in] slot the number of the module slot 
 * @return EXIT_SUCCESS in case of success, EXIT_FAILURE otherwise
 */
int Mark6DiskDevice::linkDisk(std::string linkRoot, int slot)
{       
    
    
    // loop over partitions
    for (unsigned int i=0; i < partitions_m.size(); i++)
    {
        // check if partition is linked already
        if (partitions_m[i].linkPath != "")
            continue;
        // check if partition has been mounted
        if (partitions_m[i].mountPath == "")
            return(EXIT_FAILURE);
        // check if diskId is set
        if (diskId_m == -1)
            return(EXIT_FAILURE);
        
        // build link path
        stringstream ss;
        if (i == 0)
            ss << linkRoot << "/" << slot+1 << "/" <<  diskId_m;
        else if (i ==1)
            ss << linkRoot << "/.meta/" << slot+1 << "/" <<  diskId_m;
        
        string linkPath = ss.str();
        cout << " creating symbolic link " <<  partitions_m[i].mountPath << " to " << linkPath << endl;
        
        if (symlink(partitions_m[i].mountPath.c_str(), linkPath.c_str()) != 0)
        {
            throw new Mark6Exception("Cannot create symbolic link: " + partitions_m[i].mountPath + " -> " +  linkPath);
        }
        
        partitions_m[i].linkPath = linkPath;
        
    }
   
    return(EXIT_SUCCESS);
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
        source = "/dev/" + partitions_m[i].deviceName;
        dest =  mountPath + partitions_m[i].deviceName;
        
        int ret = mount(source.c_str(), dest.c_str(), fsType_m.c_str(), MS_MGC_VAL | MS_RDONLY , "");
    
        if (ret == -1)
        {
            isMounted_m = false;
            throw Mark6MountException (string("Cannot mount  device " + source + " under " + dest));
        }
        
        partitions_m[i].mountPath = dest;
    }
   
    // now read metadata
    meta_m.parse(dest);    
    
    isMounted_m = true;
    //mountPath_m = dest;
    
    return(1);
}

void Mark6DiskDevice::unmountDisk(string mountPath)
{
    for (int i=0; i<2; i++)
    {
        string dest =  mountPath + partitions_m[i].deviceName;
        
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
 * @return true if this disk device including all contained partitions is mounted; false otherwise
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

/*std::string Mark6DiskDevice::getMountPath() const {
    return mountPath_m;
}*/

/*std::string Mark6DiskDevice::getEMSN() const {
    return eMSN_m;
}*/

/**
 * Destructor
 */
Mark6DiskDevice::~Mark6DiskDevice() {
}


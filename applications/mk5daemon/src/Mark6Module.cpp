/* 
 * File:   Mark6Module.cpp
 * Author: Helge Rottmann (MPIfR)
 * 
 * Created on 20. Oktober 2015, 15:08
 */

#include "Mark6Module.h"
#include "Mark6DiskDevice.h"
#include <algorithm>
#include <iostream>

using namespace std;

Mark6Module::Mark6Module() {
    eMSN_m = "";
}

Mark6Module::Mark6Module(const Mark6Module& orig) {
}

Mark6Module::~Mark6Module() {
}

/**
 * Adds th given disk device to the list of devices associated with this module. 
 * @param[in] device the disk device to add
 */
void Mark6Module::addDiskDevice(Mark6DiskDevice device)
{
    device.setDiskId(diskDevices_m.size());
    diskDevices_m.push_back(device);
}

/**
 * Removes the given device from the list of devices associated with the module.
 * If symbolic links were associated with partitions on this device they will be removed.
 * In case the removed device was the last one on the module the module is reset
 * to the initial state.
 * @param device the disk device to add to the module
 */
void Mark6Module::removeDiskDevice(Mark6DiskDevice device)
{    
    // loop over all devices
    for( vector<Mark6DiskDevice>::iterator iter = diskDevices_m.begin(); iter != diskDevices_m.end(); ++iter )
    {
        //find the device to be removed
        if( (*iter).getName() == device.getName() )
        {
            // remove symbolic links maintained to this device
            (*iter).unlinkDisk();
           
            diskDevices_m.erase( iter );         
            break;
        }
    }
   
    // if this was the last disk of the module clear the eMSN
    if (diskDevices_m.size() == 0)
        eMSN_m = "";
}

/**
 * Gets the disk device with at given index position
 * @param[in] index
 * @return the disk device at the given index position; NULL if no device exists at the index position
 */
Mark6DiskDevice *Mark6Module::getDiskDevice(int index)
{
    if (index > diskDevices_m.size())
        return(NULL);
    
    return(&diskDevices_m[index]);
    
}

/**
 * Returns the eMSN of the module
 * @return the eMSN of the module
 */
string Mark6Module::getEMSN()
{
    return(eMSN_m);
}

/**
 * Sets the module eMSN
 * @param eMSN of the module
 */
void Mark6Module::setEMSN(std::string eMSN) {
    eMSN_m = eMSN;
}

/**
 * 
 * @return the number of disk devices associated with this module
 */
int Mark6Module::getNumDiskDevices()
{
    return(diskDevices_m.size());
}


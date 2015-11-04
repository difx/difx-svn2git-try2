/* 
 * File:   Mark6Module.cpp
 * Author: Helge Rottmann (MPIfR)
 * 
 * Created on 20. Oktober 2015, 15:08
 */

#include "Mark6.h"
#include "Mark6Module.h"
#include "Mark6DiskDevice.h"
#include <algorithm>
#include <iostream>
#include <sstream>

using namespace std;

Mark6Module::Mark6Module() {
    eMSN_m = "";
    
    for (int i=0; i < MAXDISKS; i++)
    {
        diskDevices_m[i] = NULL;
    }
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
    
    int pos = device.getPosition();
    
    if (pos == -1)
    {
        throw new Mark6Exception("diskId for device " + device.getName() + " is not set. Cannot add device to module");
    }
    
    if ((pos < 0 ) || (pos > MAXDISKS))
    {
        throw new Mark6Exception("Illegal diskId for device " + device.getName() );
    }
    
    
    diskDevices_m[pos] = new Mark6DiskDevice(device);
    
    cout << " added device " << device.getName() << " at position " << pos << endl;;
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
    for(int i = 0; i < MAXDISKS; i++)
    {
        //find the device to be removed
        if( diskDevices_m[i]->getName() == device.getName() )
        {
            // remove symbolic links maintained to this device
            diskDevices_m[i]->unlinkDisk();
           
            diskDevices_m[i] = NULL;
            break;
        }
    }
   
    // if this was the last disk of the module clear the eMSN
    if (getNumDiskDevices() == 0)
        eMSN_m = "";
}

/**
 * Gets the disk device with at given index position
 * @param[in] index
 * @return the disk device at the given index position; NULL if no device exists at the index position
 */
Mark6DiskDevice *Mark6Module::getDiskDevice(int index)
{
    if ((index < 0 ) || (index > MAXDISKS))
    {
        stringstream message;
        message << "Illegal disk index requested (" << index << ") Must be between 0 and " << MAXDISKS;
        throw new Mark6Exception( message.str());
    }
        
    return(diskDevices_m[index]);
    
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
    int count = 0;
    
    for (int i=0; i < MAXDISKS; i++)
    {
        if (diskDevices_m[i] != NULL)
            count++;
    }
    
    return(count);
}

/**
 * Compares the expected serial numbers found in the meta data against the
 * disk serial numbers obtained via udev during the mounting process.
 * Only if all expected disks are presently mounted completeness of the module
 * is indicated.
 * @return true if the module is complete; false otherwise
 */
bool Mark6Module::isComplete(){
    
    string *serials;
    
    // obtain expected disk serials for this module from the meta data
    // since meta data on all th disks of this module should be identical 
    // let's use the meta from the first not empty disk device
    for (int i=0; i < MAXDISKS; i++)
    {
        if (diskDevices_m[i] != NULL)
        {  
            serials = diskDevices_m[i]->getMeta().getSerials();
            break;
        }
    }
    
    // loop over 
    for (int i=0; i < MAXDISKS; i++)
    {
        cout << "Processing " << serials[i];
        if (serials[i] != "")
        {
            
            if (diskDevices_m[i] != NULL)
            {
                if (diskDevices_m[i]->getSerial() != serials[i])
                {
                    cout << " false" << endl;
                    return(false);
                }
            }
            else
            {
                cout << " false" << endl;
                return(false);
            }
        }
    }
    
    cout << " true" << endl;
    return(true);
}

/* 
 * File:   Mark6Module.cpp
 * Author: Helge Rottmann (MPIfR)
 * 
 * Created on 20. Oktober 2015, 15:08
 */


#include "Mark6Module.h"
#include <algorithm>

using namespace std;

Mark6Module::Mark6Module() {
    eMSN_m = "";
}

Mark6Module::Mark6Module(const Mark6Module& orig) {
}

Mark6Module::~Mark6Module() {
}

void Mark6Module::addDiskDevice(std::string deviceName)
{
    diskDevices_m.push_back(deviceName);
}

void Mark6Module::removeDiskDevice(std::string deviceName)
{
    diskDevices_m.erase( remove( diskDevices_m.begin(), diskDevices_m.end(), deviceName ), diskDevices_m.end() ); 
}

/**
 * Returns the eMSN of the module
 * @return the eMSN of the module
 */
string Mark6Module::getEMSN()
{
    return(eMSN_m);
}


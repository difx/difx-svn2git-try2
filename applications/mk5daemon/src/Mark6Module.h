/* 
 * File:   Mark6Module.h
 * Author: Helge Rottmann (MPIfR)
 *
 * Created on 20. Oktober 2015, 15:08
 */

#ifndef MARK6MODULE_H
#define	MARK6MODULE_H

#include <string>
#include <vector>
#include "Mark6DiskDevice.h"

class Mark6Module {
public:
    Mark6Module();
    Mark6Module(const Mark6Module& orig);
    virtual ~Mark6Module();
    void addDiskDevice(Mark6DiskDevice device);
    void removeDiskDevice(Mark6DiskDevice device);
    Mark6DiskDevice *getDiskDevice(int index);
    std::string getEMSN();
    void setEMSN(std::string eMSN);
    int getNumDiskDevices();
    
private:
    static const int MAXDISKS = 8;   // number of disks per mark6 module
    std::string eMSN_m;
    std::vector<Mark6DiskDevice > diskDevices_m;   // holds the device names for each of the disks
};

#endif	/* MARK6MODULE_H */


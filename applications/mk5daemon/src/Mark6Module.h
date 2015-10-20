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

class Mark6Module {
public:
    Mark6Module();
    Mark6Module(const Mark6Module& orig);
    virtual ~Mark6Module();
    void addDiskDevice(std::string deviceName);
    void removeDiskDevice(std::string deviceName);
    std::string getEMSN();
    
private:
    static const int MAXDISKS = 8;   // number of disks per mark6 module
    std::string eMSN_m;
    std::vector<std::string > diskDevices_m;   // holds the device names for each of the disks
};

#endif	/* MARK6MODULE_H */


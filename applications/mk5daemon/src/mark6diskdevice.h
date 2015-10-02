/* 
 * File:   Mark5DiskDevice.h
 * Author: oper
 *
 * Created on 28. September 2015, 14:33
 */

#ifndef MARK6DISKDEVICE_H
#define	MARK6DISKDEVICE_H

#include <string>
#include <vector>
#include "Mark6Meta.h"

class Mark6DiskDevice {
public:
    Mark6DiskDevice(std::string deviceName);
    virtual ~Mark6DiskDevice();
    void addPartition(std::string partitionName);
    std::vector<std::string> getPartitions() const;
    std::string getName() const;
    int mountDisk(std::string mountPath);
    void unmountDisk(std::string mountPath);
    bool isMounted();
    void setFsType(std::string fsType_m);
    std::string getFsType() const;
    Mark6Meta getMeta() const;
    void setDiskId(int diskId_m);
    int getDiskId() const;
    void setSlotId(int slotId_m);
    int getSlotId() const;
    //std::string getEMSN() const;
private:
    std::string name_m;
    std::vector<std::string> partitions_m;
    bool isMounted_m;
    std::string mountPath_m;
    std::string fsType_m;
    Mark6Meta meta_m;
    int slotId_m;
    int diskId_m;
    
   // std::string eMSN_m;
};

#endif	/* MARK6DISKDEVICE_H */


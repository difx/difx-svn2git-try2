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
    
    struct Mark6Partition
    {
        std::string deviceName;
        std::string mountPath;
        std::string linkPath;
        // sort Mark6Partition by deviceName
        static bool sortByName(const Mark6Partition &lhs, const Mark6Partition &rhs) { return lhs.deviceName < rhs.deviceName; }
    };
    
    bool sortByName(const Mark6Partition &lhs, const Mark6Partition &rhs); 
    
    Mark6DiskDevice(std::string deviceName);
    virtual ~Mark6DiskDevice();
    void addPartition(std::string partitionName);
    std::vector<Mark6Partition> getPartitions() const;
    std::string getName() const;
    int mountDisk(std::string mountPath);
    void unmountDisk(std::string mountPath);
    int linkDisk(std::string linkRoot, int slot);
    int unlinkDisk();
    bool isMounted();
    void setFsType(std::string fsType_m);
    std::string getFsType() const;
    Mark6Meta getMeta() const;
    void setDiskId(long diskId_m);
    long getDiskId() const;
    void setSlotId(int slotId_m);
    int getSlotId() const;
    void setControllerId(int controllerId_m);
    int getControllerId() const;
    void setSerial(std::string serial_m);
    std::string getSerial() const;
    


private:

    
    std::string name_m;
    std::vector<Mark6Partition> partitions_m;
    bool isMounted_m;
    //std::string mountPath_m;
    //std::string linkPath_m;
    std::string fsType_m;
    Mark6Meta meta_m;
    int slotId_m;
    long diskId_m;
    int controllerId_m;
    std::string serial_m;
    
};

#endif	/* MARK6DISKDEVICE_H */


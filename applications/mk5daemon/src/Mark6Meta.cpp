/* 
 * File:   Mark6Meta.cpp
 * Author: Helge Rottmann (MPIfR)
 * 
 * Created on 30. September 2015, 12:23
 */

#include "Mark6Meta.h"
#include "Mark6.h"
#include "Mark6Module.h"
#include <string>
#include <sys/types.h>
#include <sys/stat.h>
#include <fstream>
#include <iostream>
#include <sstream>

#include <map>

using namespace std;

Mark6Meta::Mark6Meta() {
    reset();
}

Mark6Meta::Mark6Meta(const Mark6Meta& orig) {
}

Mark6Meta::~Mark6Meta() {
}

void Mark6Meta::reset()
{
    eMSN_m = "empty";
    for (int i=0; i< Mark6Module::MAXDISKS; i++)
        serials_m[i] = "";
    
}

std::string *Mark6Meta::getSerials()  {
    return serials_m;}


string Mark6Meta::getEMSN() const {
    return eMSN_m;
}

void Mark6Meta::parse(string rootPath)
{
    
    struct stat info;
    string path = "";
    string line;

    // check if directory exists
    if (( stat( rootPath.c_str(), &info ) != 0 ) || (!info.st_mode & S_IFDIR))
    {
        throw new Mark6MountException("The meta directory: " + rootPath + " does not exist");
        
    }
    
    // read contents of eMSN file
    path = rootPath + "/eMSN";
    if (stat( path.c_str(), &info ) != 0 )
    {
        throw new Mark6InvalidMetadata ("The meta file: eMSN does not exist at:" + rootPath);
    }

    ifstream infile(path.c_str());
    infile >> eMSN_m;
    infile.close();
    
    // parse disk serials
    path = rootPath + "/disk_sn";
    if (stat( path.c_str(), &info ) != 0 )
    {
        throw new Mark6InvalidMetadata ("The meta file: disk_sn does not exist at:" + rootPath);
    }
    
    infile.open(path.c_str());
    while (getline(infile, line))
    {
        cout << "Meta " << line << endl;
        string pos = line.substr(0, line.find(":"));
        string serial = line.substr(line.find(":")+1, string::npos);
        
        int index = -1;
        stringstream(pos) >> index;
        
        serials_m[index] = serial;
        
        cout << index << " " << serials_m[index] << endl;
    
        // process pair (a,b)
    }
}
    
    
 


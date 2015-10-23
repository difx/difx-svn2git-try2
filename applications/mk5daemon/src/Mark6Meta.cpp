/* 
 * File:   Mark6Meta.cpp
 * Author: Helge Rottmann (MPIfR)
 * 
 * Created on 30. September 2015, 12:23
 */

#include "Mark6Meta.h"
#include "Mark6.h"
#include <string>
#include <sys/types.h>
#include <sys/stat.h>
#include <fstream>
#include <iostream>

using namespace std;

Mark6Meta::Mark6Meta() {

    eMSN_m = "empty";
}

/*Mark6Meta::Mark6Meta(const Mark6Meta& orig) {
}*/

Mark6Meta::~Mark6Meta() {
}

string Mark6Meta::getEMSN() const {
    return eMSN_m;
}

void Mark6Meta::parse(string rootPath)
{
    //cout << "parsing meta data" << endl;
    struct stat info;
    string path = "";

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
    
    //cout << "end of parse: " << eMSN_m << endl;
   
}


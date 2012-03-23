#! /usr/bin/python
#===========================================================================
# SVN properties (DO NOT CHANGE)
#
# $Id$
# $HeadURL$
# $LastChangedRevision$
# $Author$
# $LastChangedDate$
#
#============================================================================
__author__="Helge Rottmann"

import os
import sys
import tkMessageBox
from Tkinter import *
from tkinter.multilistbox import *
from string import  upper

from difxdb.model import model
from difxdb.difxdbconfig import DifxDbConfig
from difxdb.model.dbConnection import Schema, Connection
from difxdb.business.experimentaction import *
from difxdb.business.versionhistoryaction import *

# minimum database schema version required by comedia
minSchemaMajor = 1
minSchemaMinor = 0

class GenericWindow(object):
    def __init__(self, parent=None,rootWidget=None):
        
        self.rootWidget = rootWidget
        self.parent = parent
        self.config = None
        
class MainWindow(GenericWindow):
    
    def __init__(self, parent=None, rootWidget=None):
        
        # call super class constructor
        super( MainWindow, self ).__init__(parent, rootWidget)

        self.addExperimentDlg = AddExperimentWindow(self, rootWidget)
        
        self.rootWidget.title("expad: Experiment Administration")
        
        self.expEdit = 0
        self.selectedExperiment = None
        self.selectedExpIndex = -1
        self.cboStatusVar = StringVar()
        self.defaultBgColor = self.rootWidget["background"]
        
        self.expStati = []
        for status in  session.query(model.ExperimentStatus).order_by("statuscode").all():
            self.expStati.append(status.experimentstatus)
        
    def show(self):
        
        
        self._setupWidgets()
        self.updateExpListbox()
       
        
    def _setupWidgets(self):
        
        self.rootWidget.rowconfigure(0, weight=1) 
        self.rootWidget.columnconfigure(0, weight=1)     
        
        Button(self.rootWidget, text="Exit", command=self.rootWidget.destroy).grid(row=10,column=10,sticky=E)
        
        # frames
        frmExps = LabelFrame(self.rootWidget, text="Experiments")     
        frmDetail = LabelFrame(self.rootWidget, text="Detail", padx=5)
        frmExps.columnconfigure(0,weight=1)
        frmDetail.columnconfigure(0,weight=1)
        
        #frmExps
        col1 = ListboxColumn("experiment", 25)
        col2 = ListboxColumn("number", 5)
        col3 = ListboxColumn("status", 15) 
        self.grdExps = MultiListbox(frmExps, col1, col2, col3)
        self.grdExps.bindEvent("<ButtonRelease-1>", self.selectExpEvent)
        
        btnAddExp = Button(frmExps, text="Add experiment", command=self.addExperimentDlg.show)
             
        #frmDetail
        Label(frmDetail, text="code: ").grid(row=0,column=0, sticky=W)
        Label(frmDetail, text="number: ").grid(row=1,column=0, sticky=W)
        Label(frmDetail, text="status: ").grid(row=10,column=0, sticky=W)
        self.txtCode = Entry(frmDetail, text = "")
        self.txtNumber = Entry(frmDetail, text = "")
        self.cboStatus = OptionMenu (frmDetail, self.cboStatusVar, *self.expStati ,command=self.onExpDetailChange)
        self.btnUpdate = Button(frmDetail, text="update experiment", command=self.updateExpEvent)
        self.btnDelete = Button(frmDetail, text="delete experiment", command=self.deleteExpEvent)
        
        #arrange widgets on root widget
        frmExps.grid(row=0, column=0, sticky=E+W+N+S)
        frmDetail.grid(row=0, column=10, sticky=E+W+N+S)
        
        #arrange widgets on frmExps 
        self.grdExps.grid(row=0, column=0, sticky=E+W+N+S)
        btnAddExp.grid(row=10,column=0, sticky=E+W)
        
        #arrange widgets on frmDetail
        self.txtCode.grid(row=0, column=1, sticky=E+W)
        self.txtNumber.grid(row=1, column=1, sticky=E+W)
        self.cboStatus.grid(row=10, column=1, sticky=E+W)
        self.btnUpdate.grid(row=15, column=0, columnspan=2, sticky=E+W)
        self.btnDelete.grid(row=20, column=0, columnspan=2, sticky=E+W)
        
        # bind events
        self.txtNumber.bind("<KeyRelease>", self.onExpDetailChange)
        
        self.btnUpdate["state"] = DISABLED
        self.btnDelete["state"] = DISABLED
  
    def onExpDetailChange(self, Event):
        
        self.expEdit = 0
         
        if (self.selectedExperiment is None):
            return
        
        self.expEdit += self.setChangeColor(self.txtNumber, self.txtNumber.get(), self.selectedExperiment.number)
        self.expEdit += self.setChangeColor(self.cboStatus, self.cboStatusVar.get(), self.selectedExperiment.status.experimentstatus)
        
              
        if self.expEdit > 0:
            self.btnUpdate["state"] = NORMAL
            self.btnUpdate["fg"] = "red"
            self.btnUpdate["activeforeground"] = "red"
        else:
            self.btnUpdate["state"] = DISABLED
            
            
        
  
    def setChangeColor(self, component, componentValue, compareValue):
        
        isChange = 0
        color = self.defaultBgColor
        editColor = "red"
        
        
        if (str(compareValue) != componentValue):
            
            component.config(bg = editColor)
            if component.__class__.__name__ == OptionMenu:
                component.config(bg=editColor, activebackground=editColor)
            isChange = 1
        else:   
            component.config(bg = color)
            if component.__class__.__name__ == OptionMenu:
                component.config(bg=color, activebackground=color)

        return isChange
    
    def updateExpListbox(self):
         
        exps = session.query(model.Experiment).order_by(desc(model.Experiment.number)).all()

        self.grdExps.clearData()
        
        for exp in exps: 
            self.grdExps.appendData((exp.code, "%04d" % exp.number, exp.status.experimentstatus))
            
        self.grdExps.update()
        self.grdExps.selection_set(self.selectedExpIndex)
        
        self.getExpDetails()
 
    def getExpDetails(self):
        '''
        Retrieve the  experiment details from the database
        for the currently selected listbox item.
        Update the detail text fields accordingly
        '''
        
        self.txtCode["state"] = NORMAL
        self.txtNumber["state"] = NORMAL
        self.txtCode.delete(0,END)
        self.txtNumber.delete(0,END)
        
        if self.selectedExpIndex == -1:
            self.btnUpdate["state"] = DISABLED
            self.btnDelete["state"] = DISABLED
            self.cboStatus["state"] = DISABLED
            self.txtCode["state"] = DISABLED
            self.txtNumber["state"] = DISABLED
            
            self.selectedExperiment = None
            return
        
        self.btnDelete["state"] = NORMAL
        self.cboStatus["state"] = NORMAL
        
        
        selectedCode = self.grdExps.get(self.selectedExpIndex)[0]
        
        exp = getExperimentByCode(session, selectedCode)
             
        if (exp != None):
            self.cboStatusVar.set(exp.status.experimentstatus)
            self.txtCode.insert(0, exp.code)
            self.txtNumber.insert(0, "%04d" % exp.number)
            
            #remember original state of the selected experiment record
            self.selectedExperiment = exp
        
        self.txtCode["state"] = DISABLED
        
    def selectExpEvent(self, Event):
        
        if (len(self.grdExps.curselection()) > 0):
            self.selectedExpIndex =  self.grdExps.curselection()
        else:
            self.selectedExpIndex =  -1
        
        self.getExpDetails()
    
    def updateExpEvent(self):
        
        if self.selectedExpIndex == -1:
            return
        
        selectedStatus = self.cboStatusVar
        status = session.query(model.ExperimentStatus).filter_by(experimentstatus=selectedStatus.get()).one()
             
        selectedCode = self.grdExps.get(self.selectedExpIndex)[0]
        exp = getExperimentByCode(session, selectedCode)
        
        exp.status = status
        exp.number = self.txtNumber.get()
        
        session.commit()
        session.flush()
    
    
        self.selectedExperiment = exp
        self.editExp = 0
        self.onExpDetailChange(None)
        self.updateExpListbox()
        
    def deleteExpEvent(self):
        
        if self.selectedExpIndex == -1:
            return
        
        code = self.grdExps.get(self.selectedExpIndex)[0]
        
        if (tkMessageBox.askokcancel("Confirm experiment deletion", "Do you really want to remove experiment " + code)):
            try:
                deleteExperimentByCode(session, code)
            except Exception as e:
                tkMessageBox.showerror("Error", e)
        
        self.updateExpListbox()
        
    def changeStatusEvent(self, Event):
         
        if self.selectedExpIndex == -1:
            return
        
        selectedStatus = self.cboStatusVar
        status = session.query(model.ExperimentStatus).filter_by(experimentstatus=selectedStatus.get()).one()
             
        selectedCode = self.grdExps.get(self.selectedExpIndex)[0]
        exp = getExperimentByCode(session, selectedCode)
        exp.status = status
        session.commit()
        
        self.updateExpListbox()

class AddExperimentWindow(GenericWindow):
     
    def __init__(self, parent, rootWidget=None):
        super( AddExperimentWindow, self ).__init__(parent, rootWidget)
      
    
    def show(self):
        
        # create modal dialog
        self.dlg = Toplevel(self.parent.rootWidget, takefocus=True)
        self.dlg.title("Add experiment")
        self.dlg.transient(self.rootWidget)
        self.dlg.state("normal")
        self.dlg.grab_set()
    
        
        self._setupWidgets()
    
    def close(self):
        
        self.rootWidget.grab_set()
        self.parent.updateExpListbox()
        self.dlg.destroy()
        
    def _setupWidgets(self):
              
        Label(self.dlg, text="Code").grid(row=0, sticky=W)
        self.txtExpCode = Entry(self.dlg)
        
        
        btnOK = Button(self.dlg, text="OK", command=self._persistExperiment)
        btnCancel = Button(self.dlg, text="Cancel", command=self.close)
        
        self.txtExpCode.grid(row=0, column=1, sticky=E+W)
        btnOK.grid(row=10, column=0)
        btnCancel.grid(row=10, column=1, sticky=E)
        
        self.txtExpCode.focus_set()
        
    def _persistExperiment(self):
        
        code = upper(self.txtExpCode.get())
        # check that Code has been set
        if (code == ""):
            return
        
        try:
            # add experiment with state "scheduled"
            addExperimentWithState(session, code, 10)
        except Exception as e:
            tkMessageBox.showerror("Error", e)
       
        
        self.close()        
             
if __name__ == "__main__":
    
    root = Tk()
    
    try:
        if (os.getenv("DIFXROOT") == None):
            sys.exit("Error: DIFXROOT environment must be defined.")

        configPath = os.getenv("DIFXROOT") + "/conf/difxdb.ini"


        config = DifxDbConfig(configPath, create=True)

        # try to open the database connection
        connection = Connection()
        connection.type = config.get("Database", "type")
        connection.server = config.get("Database", "server")
        connection.port = config.get("Database", "port")
        connection.user = config.get("Database", "user")
        connection.password = config.get("Database", "password")
        connection.database = config.get("Database", "database")
        connection.echo = False

        dbConn = Schema(connection)
        session = dbConn.session()
        
        if not isSchemaVersion(session, minSchemaMajor, minSchemaMinor):
            major, minor = getCurrentSchemaVersionNumber(session)
            print "Current difxdb database schema is %s.%s but %s.%s is minimum requirement." % (major, minor, minSchemaMajor, minSchemaMinor)
            sys.exit(1)
    
        mainDlg = MainWindow(None, rootWidget=root)

        mainDlg.show()

        root.mainloop()
        
    except Exception as e:
       
        sys.exit(e)
   
   
   

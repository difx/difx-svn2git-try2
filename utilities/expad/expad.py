#! /usr/bin/python

__author__="Helge Rottmann"
__date__ ="$14.11.2011 09:47:23$"

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
        
        self.selectedExpIndex = -1
        self.cboStatusVar = StringVar()
        
        self.expStati = []
        for status in  session.query(model.ExperimentStatus).order_by("statuscode").all():
            self.expStati.append(status.experimentstatus)
        
    def show(self):
        
        
        self._setupWidgets()
        self.updateExpListbox()
       
        
    def _setupWidgets(self):
        
        self.rootWidget.rowconfigure(0, weight=1) 
        self.rootWidget.columnconfigure(0, weight=1)     
        
        # frames
        frmExps = LabelFrame(self.rootWidget, text="Experiments")     
        frmDetail = LabelFrame(self.rootWidget, text="Detail", padx=5)
        frmExps.columnconfigure(0,weight=1)
        frmDetail.columnconfigure(0,weight=1)
        
        #frmExps
        col1 = ListboxColumn("experiment", 25)
        col2 = ListboxColumn("number", 5)  
        self.grdExps = MultiListbox(frmExps, col1, col2)
        self.grdExps.bindEvent("<ButtonRelease-1>", self.selectExpEvent)
        
        btnAddExp = Button(frmExps, text="Add experiment", command=self.addExperimentDlg.show)
             
        #frmDetail
        Label(frmDetail, text="code: ").grid(row=0,column=0, sticky=W)
        Label(frmDetail, text="number: ").grid(row=1,column=0, sticky=W)
        Label(frmDetail, text="status: ").grid(row=10,column=0, sticky=W)
        self.txtCode = Entry(frmDetail, text = "")
        self.txtNumber = Entry(frmDetail, text = "")
        self.cboStatus = OptionMenu (frmDetail, self.cboStatusVar, *self.expStati , command=self.changeStatusEvent)
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
        self.btnDelete.grid(row=20, column=0, columnspan=2, sticky=E+W)
        
        # bind events
        self.cboStatus.bind("<ButtonRelease-1>", self.changeStatusEvent) 
  
    def updateExpListbox(self):
         
        exps = session.query(model.Experiment).order_by(desc(model.Experiment.number)).all()

        self.grdExps.clearData()
        
        for exp in exps: 
            self.grdExps.appendData((exp.code, "%04d" % exp.number))
            
        self.grdExps.update()
        
        self.updateExpDetails()
 
    def updateExpDetails(self):
        
        self.txtCode["state"] = NORMAL
        self.txtNumber["state"] = NORMAL
        self.txtCode.delete(0,END)
        self.txtNumber.delete(0,END)
        
        if self.selectedExpIndex == -1:
            self.btnDelete["state"] = DISABLED
            self.cboStatus["state"] = DISABLED
            self.txtCode["state"] = DISABLED
            self.txtNumber["state"] = DISABLED
            
            return
        
        self.btnDelete["state"] = NORMAL
        self.cboStatus["state"] = NORMAL
        
        
        selectedCode = self.grdExps.get(self.selectedExpIndex)[0]
        
        exp = getExperimentByCode(session, selectedCode)
        
        self.cboStatusVar.set(exp.status.experimentstatus)
        
        if (exp != None):
            self.txtCode.insert(0, exp.code)
            self.txtNumber.insert(0, "%04d" % exp.number)
        
        self.txtCode["state"] = DISABLED
        
    def selectExpEvent(self, Event):
        
        if (len(self.grdExps.curselection()) > 0):
            self.selectedExpIndex =  self.grdExps.curselection()
        else:
            self.selectedExpIndex =  -1
        
        self.updateExpDetails()
      
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
        print selectedStatus
        status = session.query(model.ExperimentStatus).filter_by(experimentstatus=selectedStatus.get()).one()
             
        selectedCode = self.grdExps.get(self.selectedExpIndex)[0]
        exp = getExperimentByCode(session, selectedCode)
        exp.status = status
        session.commit()

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
        
        addExperiment(session, code)
       
        
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
    
        mainDlg = MainWindow(None, rootWidget=root)

        mainDlg.show()

        root.mainloop()
        
    except Exception as e:
       
        sys.exit(e)
   
   
   

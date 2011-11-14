#! /usr/bin/python

__author__="Helge Rottmann"
__date__ ="$14.11.2011 09:47:23$"

import os
import sys
from Tkinter import *

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
        
        
        # frames
        frmExps = LabelFrame(self.rootWidget, text="Experiments")
        frmDetail = LabelFrame(self.rootWidget, text="Detail")
        
        #frmExps
        vscrollbar = Scrollbar(frmExps)
        self.lstExps = Listbox(frmExps, yscrollcommand=vscrollbar.set)
        vscrollbar.config(command=self.lstExps.yview)
 
        #frmDetail
        Label(frmDetail, text="code: ").grid(row=0,column=0, sticky=W)
        Label(frmDetail, text="status: ").grid(row=1,column=0, sticky=W)
        self.txtCode = Entry(frmDetail, text = "")
        self.cboStatus = OptionMenu (frmDetail, self.cboStatusVar, *self.expStati , command=self.changeStatusEvent)
        
        #arrange widgets on root widget
        frmExps.grid(row=0, column=0, sticky=E+W+N+S)
        frmDetail.grid(row=0, column=10, sticky=E+W+N+S)
        
        #arrange widgets on frmExps 
        vscrollbar.grid(row=0, column=1, sticky=N+S)
        self.lstExps.grid(row=0, column=0)
        
        #arrange widgets on frmDetail
        self.txtCode.grid(row=0, column=1, sticky=E+W)
        self.cboStatus.grid(row=1, column=1, sticky=E+W)
        
        # bind events
        self.lstExps.bind("<ButtonRelease-1>", self.selectExpEvent) 
        #self.cboStatus.bind("<ButtonRelease-1>", self.changeStatusEvent) 
  
    def updateExpListbox(self):
         
        exps = session.query(model.Experiment).order_by(model.Experiment.code).all()

        self.lstExps.delete(0, END)
        
        for exp in exps: 
                    
            self.lstExps.insert(END, exp.code)
 
    def updateExpDetails(self):
        
        self.txtCode["state"] = NORMAL
        self.txtCode.delete(0,END)
        
        if self.selectedExpIndex == -1:
            return
        
        selectedCode = self.lstExps.get(self.selectedExpIndex)
        
        exp = getExperimentByCode(session, selectedCode)
        
        self.cboStatusVar.set(exp.status.experimentstatus)
        
        if (exp != None):
            self.txtCode.insert(0, exp.code)
        
        self.txtCode["state"] = DISABLED
        
    def selectExpEvent(self, Event):
        
        if (len(self.lstExps.curselection()) > 0):
            self.selectedExpIndex =  self.lstExps.curselection()[0]
        else:
            self.selectedExpIndex =  -1
            
        self.updateExpDetails()
        
    def changeStatusEvent(self, Event):
         
        if self.selectedExpIndex == -1:
            return
        
        selectedStatus = self.cboStatusVar
        print selectedStatus
        status = session.query(model.ExperimentStatus).filter_by(experimentstatus=selectedStatus.get()).one()
        
        
        selectedCode = self.lstExps.get(self.selectedExpIndex)
        exp = getExperimentByCode(session, selectedCode)
        exp.status = status
        session.commit()
        
        
        
        
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
   
   
   

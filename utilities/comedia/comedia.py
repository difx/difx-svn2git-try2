#!/usr/bin/python
__author__="Helge Rottmann"
__date__ ="$Sep 12, 2011 9:35:56 AM$"


import re
import os
import ConfigParser
import tkMessageBox
import PIL
import barcode

from difxdb.business.experimentaction import experimentExists, getActiveExperimentCodes,getExperimentByCode
from difxdb.business.moduleaction import moduleExists, isCheckOutAllowed, hasDir, getUnscannedModules, getModuleByVSN
from difxdb.business.slotaction import getOccupiedSlots
from difxdb.model.dbConnection import Schema, Connection
from difxdb.model import model
from difxfile.difxdir import *

from barcode.writer import ImageWriter, FONT
from string import strip, upper
from collections import deque

from sqlalchemy import *
from Tkinter import *


class GenericWindow(object):
    def __init__(self, parent=None,rootWidget=None):
        
        self.rootWidget = rootWidget
        self.parent = parent
        self.config = None
        
class MainWindow(GenericWindow):
    
    def __init__(self, parent=None, rootWidget=None):
        
        # call super class constructor
        super( MainWindow, self ).__init__(parent, rootWidget)

        self.rootWidget.title("comedia: Correlator Media Archive")
        
        # sub dialogs
        self.checkinDlg = CheckinWindow(self, rootWidget)  
        self.labelOptionsDlg= LabelOptionsWindow(self,rootWidget)
        self.databaseOptionsDlg= DatabaseOptionsWindow(self,rootWidget)
        self.scanModulesDlg = ScanModulesWindow(self, rootWidget)
        
        self.defaultBgColor = rootWidget["background"]
        self.isConnected = False
        
        self.selectedSlotIndex = -1 
        self.moduleEdit = 0
        self.filterReleaseList = IntVar()
        self.filterDirLess = IntVar()
        
        self.labelSizeX = 320
        self.labelSizeY = 130
        self.moduleFilter = ""
                
    def show(self):
        
        self._setupWidgets()
        self.updateSlotListbox()
        self.refreshStatusEvent()
        
        
    def _setupWidgets(self):
        
     
        # menubar setup
        menubar = Menu(self.parent)
        
        optionmenu = Menu(menubar, tearoff=0)
        optionmenu.add_command(label="Label options", command=self.showLabelOptions)
        optionmenu.add_command(label="Database options", command=self.showDatabaseOptions)

        menubar.add_cascade(label="Options", menu=optionmenu)

       # labelmenu = Menu(menubar, tearoff=0)
       # labelmenu.add_command(label="Print VSN label", command=donothing)
       # labelmenu.add_command(label="Print library label", command=donothing)
       # labelmenu.add_command(label="Print both labels", command=donothing)

        #menubar.add_cascade(label="Label", menu=labelmenu)

        self.rootWidget.config(menu=menubar)
        
        # frames
        self.frmMain = LabelFrame(self.rootWidget, text="")
        self.frmDetail = LabelFrame(self.rootWidget, text="Detail")
        self.frmEditExperiment = Frame(self.frmDetail)
        frmStatus = LabelFrame(self.rootWidget, text="Status")
        #self.frmFilter = LabelFrame(self.rootWidget, text="")
        
        self.btnQuit = Button(self.rootWidget, text="Exit", command=self.rootWidget.destroy)
        
        #widgets on frmMain  
        lblSearch = Label(self.frmMain, text = "search ")
        self.txtSearch = Entry(self.frmMain, text = "") 
        self.chkRelease = Checkbutton(self.frmMain, text = "releasable modules only", variable = self.filterReleaseList, command=self.updateSlotListbox)
        self.chkDirLess = Checkbutton(self.frmMain, text = "modules without .dir only", variable = self.filterDirLess, command=self.updateSlotListbox)
        vscrollbar = Scrollbar(self.frmMain,command=self.scrollbarEvent)
        self.lstMainSlot = Listbox(self.frmMain,yscrollcommand=vscrollbar.set)
        self.lstModule = Listbox(self.frmMain, yscrollcommand=vscrollbar.set)  
        self.btnNewModule = Button (self.frmMain, text="Check-in module", command=self.checkinModule)
          
        
        # widgets on frmStatus
        Label(frmStatus, text="Number of modules without .dir files: ").grid(row=0,column=0, sticky=W)
        Label(frmStatus, text="Number of unscanned modules: ").grid(row=1,column=0, sticky=W)
        self.lblNumDirLess = Label(frmStatus, text = "")
        self.lblNumUnscanned = Label(frmStatus, text = "")
        self.btnRefresh = Button(frmStatus, text="Refresh", command=self.refreshStatusEvent)
        self.btnModuleScan = Button(frmStatus, text="Scan", command=self.scanModuleEvent)
        
        # widgets on frmDetail
        Label(self.frmDetail, text = "location: ").grid(row=0, column=0, sticky=W)
        Label(self.frmDetail, text = "vsn: ").grid(row=1, column=0, sticky=W)
        Label(self.frmDetail, text = "capacity: ").grid(row=2, column=0, sticky=W)
        Label(self.frmDetail, text = "datarate: ").grid(row=3, column=0, sticky=W)
        Label(self.frmDetail, text = "received: ").grid(row=4, column=0, sticky=W)
        Label(self.frmDetail, text = "experiment(s): ").grid(row=5, column=0, sticky=W)     
        self.txtLocationContent = Entry(self.frmDetail, text = "", state=DISABLED) 
        self.lblVSNContent = Entry(self.frmDetail, text = "", state=DISABLED)
        self.lblCapacityContent = Entry(self.frmDetail, text = "", state=DISABLED)
        self.lblDatarateContent = Entry(self.frmDetail, text = "", state=DISABLED)
        self.lblReceivedContent = Entry(self.frmDetail, text = "", state=DISABLED)
        scrollCboExperiments = Scrollbar(self.frmDetail)
        self.cboExperiments =  Listbox(self.frmDetail, height=3, yscrollcommand=scrollCboExperiments.set, selectmode=MULTIPLE, state=DISABLED)
        scrollCboExperiments.config(command=self.cboExperiments.yview)
        self.btnDeleteModule = Button(self.frmDetail, text="Check-out module", command=self.checkOutModule, state=DISABLED)
        self.btnEditModule = Button(self.frmDetail, text="Update module", command=self.updateModule, state=DISABLED)
        self.btnPrintLibraryLabel = Button (self.frmDetail, text="Print library label", command=self.printLibraryLabel,state=DISABLED)
        self.btnPrintVSNLabel = Button (self.frmDetail, text="Print VSN label", command=self.printVSNLabel,state=DISABLED)
        
        # widgets on frmEditExperiment
        scrollCboFreeExperiments = Scrollbar(self.frmEditExperiment)
        self.cboFreeExperiments = Listbox(self.frmEditExperiment, height=3, yscrollcommand=scrollCboFreeExperiments.set, selectmode=MULTIPLE)
        scrollCboFreeExperiments.config(command=self.cboFreeExperiments.yview)
        self.btnAddExperiments = Button(self.frmEditExperiment, text="<<", command=self.addExperimentEvent)
        self.btnRemoveExperiments = Button(self.frmEditExperiment, text=">>", command=self.removeExperimentEvent)
         
        
        # bind events to widgets
        self.txtLocationContent.bind("<KeyRelease>", self.editModuleDetailsEvent)
        self.lblVSNContent.bind("<KeyRelease>", self.editModuleDetailsEvent)
        self.lblCapacityContent.bind("<KeyRelease>", self.editModuleDetailsEvent)
        self.lblDatarateContent.bind("<KeyRelease>", self.editModuleDetailsEvent)
        self.lblReceivedContent.bind("<KeyRelease>", self.editModuleDetailsEvent)
        self.txtSearch.bind("<KeyRelease>", self.searchModuleEvent)
        self.lstMainSlot.bind("<MouseWheel>", self.mouseWheelEvent)
        self.lstModule.bind("<MouseWheel>", self.mouseWheelEvent)
        self.lstMainSlot.bind("<Button-4>", self.mouseWheelEvent)
        self.lstModule.bind("<Button-4>", self.mouseWheelEvent)
        self.lstMainSlot.bind("<Button-5>", self.mouseWheelEvent)
        self.lstModule.bind("<Button-5>", self.mouseWheelEvent)
        self.lstMainSlot.bind("<ButtonRelease-1>", self.selectSlotEvent) 
        self.lstModule.bind("<ButtonRelease-1>", self.selectModuleEvent)
        self.cboExperiments.bind("<ButtonRelease-1>", self.selectExperimentEvent)
        
        # arrange objects on grid       
        #self.frmFilter.grid(row=0,column=0,sticky=W+E)
        self.frmMain.grid(row=1,rowspan=2,column=0, sticky=E+W+N+S)   
        self.frmDetail.grid(row=1, column=3, sticky=E+W+N+S )
        frmStatus.grid(row=2,column=3,sticky=N+S+E+W)
        self.frmEditExperiment.grid(row=5, column=3, sticky=N+W )
        self.btnQuit.grid(row=10,columnspan=5)
        
        # arrange objects on frmMain
        lblSearch.grid(row=0, column=0, sticky=E)
        self.txtSearch.grid(row=0, column=1, sticky=W+E)
        self.chkRelease.grid(row=1, column=1, sticky=W)
        self.chkDirLess.grid(row=2, column=1, sticky=W)
        self.lstMainSlot.grid(row=10, column=0, sticky=N+S+E+W)
        self.lstModule.grid(row=10, column=1, sticky=N+S+E+W)
        vscrollbar.grid(row=10, column=2, sticky=N+S)  
        self.btnNewModule.grid(row=20, columnspan=2, sticky=E+W)        
        
        # arrage objects on frmStatus
        self.lblNumDirLess.grid(row=0, column=1, sticky=W)
        self.lblNumUnscanned.grid(row=1, column=1, sticky=W)
        self.btnModuleScan.grid(row=1,  column=2)
        self.btnRefresh.grid(row=2,  column=0, columnspan=3, sticky=E+W)
        
        #arrange objects on frmDetail
        self.txtLocationContent.grid(row=0, column=1, sticky=W)
        self.lblVSNContent.grid(row=1, column=1, sticky=W)
        self.lblCapacityContent.grid(row=2, column=1, sticky=W)
        self.lblDatarateContent.grid(row=3, column=1, sticky=W)
        self.lblReceivedContent.grid(row=4, column=1, sticky=W)
        self.cboExperiments.grid(row=5, column=1, sticky=W+N+S)
        scrollCboExperiments.grid(row=5,column=2, rowspan=2, sticky=W+N+S)
        self.btnEditModule.grid(row=20, column=0, sticky=E+W)
        self.btnDeleteModule.grid(row=20, column=1, sticky=E+W)
        self.btnPrintLibraryLabel.grid(row=21,column=0, sticky=E+W)
        self.btnPrintVSNLabel.grid(row=21,column=1, sticky=E+W)
        
        
        # arrange objects on frmEditExperiment
        self.cboFreeExperiments.grid(row=0, column=1, rowspan=2, sticky=W+N+S)
        scrollCboFreeExperiments.grid(row=0,column=2, rowspan=2, sticky=W+N+S)
        self.btnAddExperiments.grid(row=0, column=0, sticky=W)
        self.btnRemoveExperiments.grid(row=1, column=0, sticky=W)
        self.frmEditExperiment.grid_remove()
        

    
    def printVSNLabel(self):
        
        slot = model.Slot()
        slot = session.query(model.Slot).filter_by(location=self.lstMainSlot.get(self.selectedSlotIndex)).one()
        
        if (slot > 0):
            
            os.system('rm -f /tmp/comedia_vsn.png')
            vsnString = "%s/%s/%s" % (slot.module.vsn, slot.module.capacity, slot.module.datarate)
            
            options = dict(font_size=26, dpi=200, text_distance=0, quiet_zone=0, module_height=10) 
            
            ean = barcode.get_barcode('code39', vsnString, writer=MyImageWriter())
            ean.save('/tmp/comedia_vsn', options )
            
            #os.system( self.config.get("Label", "printCommand") + ' /tmp/comedia_vsn.png')
    
    def printLibraryLabel(self):
        
        if (self.selectedSlotIndex == -1):
            return
        
        slot = model.Slot()
        slot = session.query(model.Slot).filter_by(location=self.lstMainSlot.get(self.selectedSlotIndex)).one()
        
        
        if (slot > 0):
            
            im = PIL.Image.new("L", (self.labelSizeX,self.labelSizeY),255)

            try:
                font = PIL.ImageFont.truetype("resources/FreeSans.ttf", int(self.config.get("Label", "fontSize")))
            except:
                font = PIL.ImageFont.load_default()
            
            os.system('rm -f /tmp/comedia_tmp.png')
            draw = PIL.ImageDraw.Draw(im)
            draw.text((10,10), self.config.get("Label","headerLine"), font=font, fill=1)
            draw.text((10,40),"%s" % slot.location, font=font, fill=1)
            draw.text((10,70),"%s / %s / %s" % (slot.module.vsn, slot.module.capacity, slot.module.datarate) , font=font, fill=1)

            im.save("/tmp/comedia_tmp.png")
            
            os.system( self.config.get("Label", "printCommand") + ' /tmp/comedia_tmp.png')
        
    def updateModule(self):
        
        if (self.selectedSlotIndex == -1):
            return
        
        if (self.isConnected == False):
            return
            
        slot = model.Slot()
        slot = session.query(model.Slot).filter_by(location=self.lstMainSlot.get(self.selectedSlotIndex)).one()
        
        
        if (slot > 0):
         
            slot.module.vsn = self.lblVSNContent.get()
            slot.module.capacity = self.lblCapacityContent.get()
            slot.module.datarate = self.lblDatarateContent.get()
            
            # remove all experiment assigments of this module
            slot.module.experiments = []
            for expItem in self.cboExperiments.get(0, END):
                exp = getExperimentByCode(session, expItem)
                
                if (exp != None):
                    slot.module.experiments.append(exp)
            


            #session.update(slot)
            session.commit()
        
        self.frmEditExperiment.grid_remove()
        self.moduleEdit = 0
        self._saveModuleDetails()
        self.editModuleDetailsEvent(None)
  

    
    def updateSlotListbox(self):
    
        if (self.isConnected == False):
            return
             
        slots = getOccupiedSlots(session)

        self.lstMainSlot.delete(0, END)
        self.lstModule.delete(0, END)

        for slot in slots: 
            # check for module filter phrase
            if (self.moduleFilter != ""):
                if (self.moduleFilter not in slot.module.vsn):
                    #self.lstMainSlot.insert(END, slot.location)
                    #self.lstModule.insert(END, slot.module.vsn)
                    continue
                    
            #check if "released" checkbox is activated
            if (self.filterReleaseList.get()):
                if (not isCheckOutAllowed(session, slot.module.vsn)):
                    continue
            
            # ckeck if "dirLess" checkbox is activated
            if (self.filterDirLess.get()):
                if (hasDir(slot.module.vsn)):
                    continue
                    
            self.lstMainSlot.insert(END, slot.location)
            self.lstModule.insert(END, slot.module.vsn)
   
    
    def _saveModuleDetails(self):
        
        self.lastLocationContent = self.txtLocationContent.get()
        self.lastVSNContent = self.lblVSNContent.get()
        self.lastCapacityContent = self.lblCapacityContent.get()
        self.lastDatarateContent = self.lblDatarateContent.get()
        self.lastReceivedContent = self.lblReceivedContent.get()
        
        self.lastExperiments = self.cboExperiments.get(0,END)
      
         
    def _updateExperimentListboxes(self):
        pass
    
    def updateSlotDetails(self):
         
        
        self.btnPrintVSNLabel["state"] = DISABLED
        self.btnPrintLibraryLabel["state"] = DISABLED
        self.btnDeleteModule["state"] = DISABLED
        self.txtLocationContent["state"] = NORMAL
        self.lblVSNContent["state"] = NORMAL
        self.lblCapacityContent["state"] = NORMAL
        self.lblDatarateContent["state"] = NORMAL
        self.lblReceivedContent["state"] = NORMAL
        self.cboExperiments["state"] = NORMAL
        
        self.txtLocationContent.delete(0,END)
        self.lblVSNContent.delete(0,END)
        self.lblCapacityContent.delete(0,END)
        self.lblDatarateContent.delete(0,END)
        self.lblReceivedContent.delete(0,END)
        self.cboExperiments.delete(0, END)
        
        if self.selectedSlotIndex == -1:
            self._saveModuleDetails()
            return
        
        if (self.isConnected == False):
            return
        
        self.btnPrintVSNLabel["state"] = NORMAL
        self.btnPrintLibraryLabel["state"] = NORMAL
        self.btnDeleteModule["state"] = NORMAL
        
        slot = model.Slot()    
        slot = session.query(model.Slot).filter_by(location=self.lstMainSlot.get(self.selectedSlotIndex)).one()
    
        
        if (slot != None):
            assignedCodes = []
            
            self.txtLocationContent.insert(0, slot.location)
            self.lblVSNContent.insert(0, slot.module.vsn)
            self.lblCapacityContent.insert(0, slot.module.capacity)
            self.lblDatarateContent.insert(0, slot.module.datarate)
            self.lblReceivedContent.insert(0, slot.module.received)
            
            # update experiment listbox
            for experiment in slot.module.experiments:
                assignedCodes.append(experiment.code)
                self.cboExperiments.insert(END, experiment.code)
                
            # update listbox containing unassigned experiments
            freeExps = getActiveExperimentCodes(session)
            for code in freeExps:
                if code in assignedCodes:
                    continue
                self.cboFreeExperiments.insert(END, code)
                
            self._saveModuleDetails()
            
        self.txtLocationContent["state"] = DISABLED
        self.lblReceivedContent["state"] = DISABLED
        self.cboExperiments["state"] = NORMAL
            
    def checkOutModule(self):
    
        if (self.selectedSlotIndex == -1):
            return
        
        slot = model.Slot()    
        slot = session.query(model.Slot).filter_by(location=self.lstMainSlot.get(self.selectedSlotIndex)).one()

        if (slot == None):
            return
        
        # delete module
        module = model.Module()
        module = session.query(model.Module).filter_by(id = slot.module.id).one()
        
        
        if (module != None):
            if (isCheckOutAllowed(session,module.vsn) == False):
                tkMessageBox.showerror("Error", "Module cannot be checked-out.\nIt contains experiments that have not been released yet.")
                return
            
            if (tkMessageBox.askokcancel("Confirm module check-out", "Do you really want to remove module " + slot.module.vsn + " from the library? ")):

                session.delete(module) 
                session.commit()

                self.selectedSlotIndex = -1
                self.updateSlotListbox()
                self.updateSlotDetails()
                
                # delete .dir file
                 
                dirFile = buildDirFilename(settings["dirPath"], module.vsn)
                if os.path.isfile(dirFile):
                    os.remove(dirFile)
                else:
                    print "file does not exists"

        return
    
    def selectExperimentEvent(self, Event):
        
        if (self.selectedSlotIndex == -1):
            return
        
        self.frmEditExperiment.grid()
        
    def removeExperimentEvent(self):
        
        if (len(self.cboExperiments.curselection()) == 0):
            return
        selection = list(self.cboExperiments.curselection())
        selection.reverse()
        for exp in selection:
            code = self.cboExperiments.get(exp)
            self.cboExperiments.delete(exp)
            self.cboFreeExperiments.insert(END, code)
        
        self.editModuleDetailsEvent(None)
        
    def addExperimentEvent(self):
        
        if (len(self.cboFreeExperiments.curselection()) == 0):
            return
        
        selection = list(self.cboFreeExperiments.curselection())
        selection.reverse()
        
        for exp in selection:
            code = self.cboFreeExperiments.get(exp)
            self.cboFreeExperiments.delete(exp)
            self.cboExperiments.insert(END, code)   
        
        self.editModuleDetailsEvent(None)
            
    def selectModuleEvent(self,Event):
        
        # check for unsaved module edits
        if (self.moduleEdit > 0):
            if (tkMessageBox.askyesno("Cancel unsaved changes", "There are unsaved changes in the module details\nAre you sure you want to abandon these?") == False):
                self.lstModule.selection_clear(self.lstModule.curselection()[0])
                self.lstModule.selection_set(self.selectedSlotIndex)
                self.lstMainSlot.selection_clear(self.lstMainSlot.curselection()[0])
                self.lstMainSlot.selection_set(self.selectedSlotIndex)
                return
            else:
                self._saveModuleDetails()
                self.editModuleDetailsEvent(None)
            
        if (len(self.lstModule.curselection()) > 0):
            self.selectedSlotIndex =  self.lstModule.curselection()[0]
            self.lstMainSlot.selection_set(self.selectedSlotIndex)
        else:
            self.selectedSlotIndex =  -1
            
        self.updateSlotDetails()
      
    def scanModuleEvent(self):
       
        self.scanModulesDlg.scanModules()
        self.refreshStatusEvent()
    
    #print difxdir.getExperiments()
            
            
    def refreshStatusEvent(self):
        
        dirLessCount = 0
        unvalidatedCount = 0
        
        slots = getOccupiedSlots(session)
             
        for slot in slots:
            
            # find modules without .dir file
            if (not hasDir(slot.module.vsn)):
                dirLessCount += 1
        
            # find unvalidated modules
            if (slot.module.numScans == None):
                unvalidatedCount += 1
        
        
        self.lblNumDirLess["text"] = dirLessCount
        self.lblNumUnscanned["text"] = unvalidatedCount
                
        
        
    def selectSlotEvent(self, Event):
    
        # check for unsaved module edits
        if (self.moduleEdit > 0):
            if (tkMessageBox.askyesno("Cancel unsaved changes", "There are unsaved changes in the module details\nAre you sure you want to abandon these?") == False):
                self.lstMainSlot.selection_clear(self.lstMainSlot.curselection()[0])
                self.lstMainSlot.selection_set(self.selectedSlotIndex)
                self.frmEditExperiment.grid_remove()
                return
            else:
                self._saveModuleDetails()
                self.editModuleDetailsEvent(None)
                
        if (len(self.lstMainSlot.curselection()) > 0):
            self.selectedSlotIndex =  self.lstMainSlot.curselection()[0]
        else:
            self.selectedSlotIndex =  -1
            
        self.updateSlotDetails()
    
    def scrollbarEvent(self, *args):
        
        self.lstMainSlot.yview(*args)
        self.lstModule.yview(*args)
        
    def mouseWheelEvent(self, event):
        
        self.lstMainSlot.yview("scroll", event.delta,"units")
        self.lstModule.yview("scroll",event.delta,"units")
        
        # this prevents default bindings from firing, which
        # would end up scrolling the widget twice
        return "break"

    
    def searchModuleEvent(self, Event):
         
        self.moduleFilter = upper(strip(self.txtSearch.get()))   
        self.updateSlotListbox()
         
        
    def editModuleDetailsEvent(self, Event):
        
        self.moduleEdit = 0
        
        color = self.defaultBgColor
        editColor = "red"
        
        if (self.lastLocationContent != self.txtLocationContent.get()):
            self.txtLocationContent["background"] = editColor
            self.moduleEdit += 1
        else:
            self.txtLocationContent["background"] = color
           
            
        if (self.lastVSNContent != self.lblVSNContent.get()):
            self.lblVSNContent["background"] = editColor
            self.moduleEdit += 1
            
        else:
            self.lblVSNContent["background"] = color
            
            
        if (self.lastCapacityContent != self.lblCapacityContent.get()):
            self.lblCapacityContent["background"] = editColor
            self.moduleEdit += 1

        else:
            self.lblCapacityContent["background"] = color
            
            
        if (self.lastDatarateContent != self.lblDatarateContent.get()):
            self.lblDatarateContent["background"] = editColor
            self.moduleEdit += 1
        else:
            self.lblDatarateContent["background"] = color
          
            
        if (self.lastReceivedContent != self.lblReceivedContent.get()):
            self.lblReceivedContent["background"] = editColor
            self.moduleEdit += 1
        else:
            self.lblReceivedContent["background"] = color
        
        if (sorted(self.cboExperiments.get(0, END)) != sorted(self.lastExperiments)):
            self.cboExperiments["background"] = editColor
            self.moduleEdit +=1
        else:
            self.cboExperiments["background"] = color
      
        if self.moduleEdit > 0:
            self.btnEditModule["state"] = NORMAL
        else:
            self.btnEditModule["state"] = DISABLED
        
        
    def checkinModule(self):
        
        self.checkinDlg.show()
        
    def showLabelOptions(self):
             
        self.labelOptionsDlg.config = self.config
        self.labelOptionsDlg.show()
        
    def showDatabaseOptions(self):
             
        self.databaseOptionsDlg.config = self.config
        self.databaseOptionsDlg.show()
        
class CheckinWindow(GenericWindow):
     
    def __init__(self, parent=None, rootWidget=None):
        
        # call super class constructor
        super( CheckinWindow, self ).__init__(parent, rootWidget)
        
        self.addExperimentDlg = AddExperimentWindow(self, rootWidget)
        
            
    def show(self):
        
        # create modal dialog
        self.dlg = Toplevel(self.rootWidget, takefocus=True)
        self.dlg.title("Check-in module")
        self.dlg.transient(self.rootWidget)
        self.dlg.state("normal")
        self.dlg.focus_set()
        self.dlg.grab_set()
        
        self._setupWidgets()
        self.updateExperimentListbox()
     
    def updateExperimentListbox(self):
         
        self.lstExp.delete(0,END)
        
        # obtain listbox items from database
        experiments = getActiveExperimentCodes(session)
        for code in experiments:
            self.lstExp.insert(END, code)
        
    def _setupWidgets(self):
         

        # create dialog elements
        yScroll = Scrollbar ( self.dlg, orient=VERTICAL )
        yScroll2 = Scrollbar ( self.dlg, orient=VERTICAL )

        Label(self.dlg, text="VSN").grid(row=0)
        Label(self.dlg, text="Slot").grid(row=1)
        Label(self.dlg, text="Experiment(s)").grid(row=3)

        self.txtVSN = Entry(self.dlg)

        self.lstSlot = Listbox(self.dlg, yscrollcommand=yScroll.set, height=5, exportselection = False )
        self.lstExp = Listbox(self.dlg, yscrollcommand=yScroll2.set, height=5 , selectmode=MULTIPLE, exportselection = False)

        yScroll.config(command=self.lstSlot.yview)
        yScroll2.config(command=self.lstExp.yview)

        # populate slot list
        ciSlotItems = getEmptySlots()
        for instance in ciSlotItems:
            self.lstSlot.insert(END, instance.location)

        #frame = LabelFrame(self.dlg)
        btnOK = Button(self.dlg, text="OK", command=self._persistSlot)
        btnCancel = Button(self.dlg, text="Cancel", command=self.dlg.destroy)
        btnAddExp = Button(self.dlg, text="+", command=self._addExperiment)

        # arrange elements on grid
        self.txtVSN.grid(row=0, column=1)
        self.lstSlot.grid(row=1, column=1)
        self.lstExp.grid(row=3, column=1)
        

        #frame.grid(row=10, column=0, columnspan=4, sticky=E+W)
        btnOK.grid(row=10, column=1, sticky=W,pady=7)
        btnCancel.grid(row=10, column=4, sticky=E)
        btnAddExp.grid(row=3, column=3, sticky=W)
        yScroll.grid ( row=1, column=2, sticky=W+N+S )
        yScroll2.grid ( row=3, column=2, sticky=W+N+S )
        
        self.txtVSN.focus_set()
        
        
    def _splitVSNLabelScan(self):
        
        m = re.match('([a-zA-Z]+[\+-]\d+)/(\d+)/(\d+)', strip(self.txtVSN.get()))

        if (m != None):
            vsn = upper(m.group(1))

            
            if (len(vsn) != 8):
                raise Exception("Illegal VSN")

            capacity = m.group(2)
            datarate = m.group(3)

            return(vsn, capacity, datarate)
        else:
            raise Exception("Illegal VSN label")
    
    def _addExperiment(self):
        self.addExperimentDlg.show()
    
    def _persistSlot(self):
     
        error = ""
        
        # check that VSN has been set
        if (self.txtVSN.get() == ""):
            error += "Empty VSN\n"

        # check that start slot has been set
        if (len(self.lstSlot.curselection()) == 0):
            error += "Empty slot\n"
                
            
        if (error != ""):
            tkMessageBox.showerror("Error", error)
            return
        
        try:
            vsn, capacity, datarate = self._splitVSNLabelScan()

        except:
            tkMessageBox.showerror("Error", "Illegal VSN label content. Must be VSN/capacity/datarate.")
            return
        
        print vsn
        
        if (moduleExists(session, vsn)):
            tkMessageBox.showerror("Error","Module\n%s\nalready checked-in" % vsn)
            return

        # retrieve currently selected item from slot select box
        selectedSlot = model.Slot()    
        selectedSlot = session.query(model.Slot).filter_by(location=self.lstSlot.get(self.lstSlot.curselection()[0])).one()
        
        
        # create new Module object
        if (selectedSlot != None):

            newModule = model.Module()

            newModule.vsn = vsn
            newModule.capacity = capacity
            newModule.datarate = datarate
            newModule.slot.append(selectedSlot)

            session.add(newModule)

            # append selected experiments
            for expItem in self.lstExp.curselection():

                exp = model.Experiment()
                exp = session.query(model.Experiment).filter_by(code=self.lstExp.get(expItem)).one()

                newModule.experiments.append(exp)

            session.commit()

            self.parent.updateSlotListbox()
            
            self.dlg.destroy()
         

        return
    
class DatabaseOptionsWindow(GenericWindow):
     
    def __init__(self, parent, rootWidget=None):
        
        # call super class constructor
        super( DatabaseOptionsWindow, self ).__init__(parent, rootWidget)
             
        
    def show(self):
        
        # create modal dialog
        self.dlg = Toplevel(self.rootWidget, takefocus=True)
        self.dlg.title ("Database Options")
        self.dlg.transient(self.rootWidget)
        self.dlg.state("normal")
        self.dlg.grab_set()
        
        self._setupWidgets()
    
    
    def _setupWidgets(self):
              
        Label(self.dlg, text="Type").grid(row=0, sticky=W)
        Label(self.dlg, text="Server").grid(row=1, sticky=W)
        Label(self.dlg, text="Port").grid(row=2, sticky=W)
        Label(self.dlg, text="Database").grid(row=3, sticky=W)
        Label(self.dlg, text="Username").grid(row=4, sticky=W)
        Label(self.dlg, text="Password").grid(row=5, sticky=W)
        
        optionList = ("mysql", "postgresql", "oracle")
        self.cboDBTypeVar = StringVar()
        self.cboDBTypeVar.set(self.config.get("Database", "type"))
        self.cboDBType = OptionMenu ( self.dlg, self.cboDBTypeVar, *optionList )

        
        self.txtServer = Entry(self.dlg)
        self.txtPort = Entry(self.dlg)
        self.txtDatabase = Entry(self.dlg)
        self.txtUsername= Entry(self.dlg)
        self.txtPassword = Entry(self.dlg)
       
        Button(self.dlg, text="OK", command=self.saveConfig).grid(row=10, column=0, sticky=E+W)
        Button(self.dlg, text="Cancel", command=self.dlg.destroy).grid(row=10, column=1, sticky=E+W) 
        Button(self.dlg, text="Test Connection", command=self._checkDatabaseConnection).grid(row=9, column=0, sticky=E+W) 
        
        self.cboDBType.grid(row=0, column=1,sticky=E+W)
        self.txtServer.grid(row=1, column=1,sticky=E+W)
        self.txtPort.grid(row=2, column=1, sticky=E+W)
        self.txtDatabase.grid(row=3, column=1, sticky=E+W)
        self.txtUsername.grid(row=4, column=1, sticky=E+W)
        self.txtPassword.grid(row=5, column=1, sticky=E+W)
    
        self.txtServer.insert(0, self.config.get("Database", "server"))
        self.txtPort.insert(0, self.config.get("Database", "port"))
        self.txtDatabase.insert(0, self.config.get("Database", "database"))
        self.txtUsername.insert(0, self.config.get("Database", "user"))
        self.txtPassword.insert(0, self.config.get("Database", "password"))
    
        
    def _checkDatabaseConnection(self):
                
        connection = Connection()
        connection.type = self.cboDBTypeVar.get()
        connection.server = self.txtServer.get()
        connection.port = self.txtPort.get()
        connection.user = self.txtUsername.get()
        connection.password = self.txtPassword.get()
        connection.database = self.txtDatabase.get()

        try:
            Schema(connection)

            tkMessageBox.showinfo("Check database connection", "Connection to database successful")
        except Exception as e:
            tkMessageBox.showerror("Check database connection", "Connection to database failed %s" % e)
    
    def saveConfig(self):
        
        self.config.set("Database", "type", self.cboDBTypeVar.get())
        self.config.set("Database", "server", self.txtServer.get())
        self.config.set("Database", "port", self.txtPort.get())
        self.config.set("Database", "database", self.txtDatabase.get())
        self.config.set("Database", "user", self.txtUsername.get())
        self.config.set("Database", "password", self.txtPassword.get())
        
        
        with open('comedia.ini', 'wb') as configfile:
            config.write(configfile)
            
        self.dlg.destroy()
 
class ScanModulesWindow(GenericWindow):
 
    def __init__(self, parent, rootWidget=None):
        
        # call super class constructor
        super( ScanModulesWindow, self ).__init__(parent, rootWidget)
        
        self.checkList = deque()
        
    def show(self):
        
        # create modal dialog
        self.dlg = Toplevel(self.rootWidget, takefocus=True)
        self.dlg.title("Scan Modules")
        self.dlg.transient(self.rootWidget)
        self.dlg.state("normal")
        self.dlg.grab_set()

        
        self._setupWidgets()
    
    
    def _setupWidgets(self):
        
        rowCount = 1
        actionVar = deque()
        
        canvas = Canvas(self.dlg, width=1024, height=800)
        yBar = Scrollbar(self.dlg)
        xBar = Scrollbar(self.dlg)
        xBar.config(command=canvas.xview, orient=HORIZONTAL)
        yBar.config(command=canvas.yview)                   
        canvas.config(xscrollcommand=xBar.set, yscrollcommand=yBar.set)
        
        # make the canvas expandable
        root.grid_rowconfigure(0, weight=1)
        root.grid_columnconfigure(0, weight=1)
 
        frmModules = Frame(canvas)
        frmModules.rowconfigure(1, weight=1)
        frmModules.columnconfigure(1, weight=1)
        
        Label(frmModules, text="module", relief="flat").grid(row=0,column=0, sticky=E+W)
        Label(frmModules, text="assigned exp.", relief="flat").grid(row=0,column=1, sticky=E+W)
        Label(frmModules, text="scanned exp.",relief="flat").grid(row=0,column=2, sticky=E+W)
        
        for module in self.checkList:
            
            Label(frmModules, text=module.vsn, relief="sunken", justify="left",padx=10).grid(row=rowCount,column=0, sticky=E+W)
            Label(frmModules, text=list(module.assignedExps), relief="sunken", justify="left", padx=10).grid(row=rowCount,column=1, sticky=E+W)
            Label(frmModules, text=list(module.scannedExps),relief="sunken", justify="left", padx=10).grid(row=rowCount,column=2, sticky=E+W)
            Radiobutton(frmModules, text="fix", variable=module.action, value=0, state=NORMAL).grid(row=rowCount, column=3)
            Radiobutton(frmModules, text="don't fix, remind me later", variable=module.action, value=1).grid(row=rowCount, column=4)
            Radiobutton(frmModules, text="don't fix, don't remind me again", variable=module.action, value=2).grid(row=rowCount, column=5)
            
            rowCount += 1
        
        btnOK = Button(self.dlg, text="OK", command=self.updateModuleEvent)
        btnCancel = Button(self.dlg, text="Cancel", command=self.dlg.destroy)
        
        canvas.grid(row=0,column=0,sticky=N+S+E+W)
        xBar.grid(row=1,column=0, sticky=E+W)
        yBar.grid(row=0,column=1, sticky=N+S)
        #frmModules.grid(row=10, column=0, sticky=N+E+S+W)
        btnOK.grid(row=10, column=0, sticky=E)
        btnCancel.grid(row=10, column=1, sticky=E+W)
        
        canvas.create_window(0, 0, anchor=NW, window=frmModules)
        frmModules.update_idletasks()

        canvas.config(scrollregion=canvas.bbox("all"))
        
    def scanModules(self):
        
        self.checkList.clear()
        modules = getUnscannedModules(session)
        
        for module in modules:
            
            
            assignedExps = deque()
            
            # check if .dir file exists
            if (not hasDir(module.vsn)):
                continue
            
            try:
                difxdir = DifxDir(settings["dirPath"], module.vsn)
            except Exception as e:
                tkMessageBox.showerror("Error", e)
                continue
                
            scannedExps = difxdir.getExperiments()
            
            
            # compare associated experiments
            for exp in module.experiments:
                assignedExps.append(exp.code)
         
            if (sorted(scannedExps) != sorted(assignedExps)):
                print scannedExps
                checkModule = self.CheckModuleItem()
                checkModule.vsn = module.vsn
                checkModule.assignedExps = assignedExps
                checkModule.scannedExps = scannedExps
                checkModule.numScans = difxdir.getScanCount()
                checkModule.stationCode = difxdir.getStationCode()
                
                self.checkList.append(checkModule)
                
            else:
                module.numScans = difxdir.getScanCount()
                module.stationCode = difxdir.getStationCode()
           
        session.commit()
            
        if (len(self.checkList) > 0):
            self.show()
            
    
     
    def updateModuleEvent(self):
        
        for checkModule in self.checkList:
            
            module = getModuleByVSN(session,checkModule.vsn)
            
            if (module == None):
                continue
            
            # action was 'fix' 
            if (checkModule.action.get() == 0):
                # remove all experiment assignments of this module
                module.experiments = []
                
                # loop over all scanned experiment codes
                for expCode in checkModule.scannedExps:
                    
                    if (not experimentExists(session, expCode)):
                       
                        # add new experiment
                        exp = model.Experiment()
                        exp.code = expCode
                        session.add(exp)
                        session.commit()
                        
                    exp = getExperimentByCode(session, expCode)
                    module.experiments.append(exp)
                    module.numScans = checkModule.numScans
                        
                session.commit()
                
                continue
            # action was 'remind again'
            elif (checkModule.action.get() == 1):
                continue
            #action was 'don't remind again'
            elif (checkModule.action.get() == 2):
                module.numScans = checkModule.numScans
                session.commit()
                
        session.commit()
        self.parent.refreshStatusEvent()
        self.dlg.destroy()
        
    class CheckModuleItem(object):
            
        def __init__(self):

            self.vsn = ""
            self.assignedExps = deque()
            self.scannedExps = deque()
            self.action = IntVar()
            self.numScans = 0
            
            self.action.set(0)
        
    
class LabelOptionsWindow(GenericWindow):
     
    def __init__(self, parent, rootWidget=None):
        
        # call super class constructor
        super( LabelOptionsWindow, self ).__init__(parent, rootWidget)
        
        
    def show(self):
        
        # create modal dialog
        self.dlg = Toplevel(self.rootWidget, takefocus=True)
        self.dlg.title("Label Options")
        self.dlg.transient(self.rootWidget)
        self.dlg.state("normal")
        
        self.dlg.grab_set()
        
        self._setupWidgets()
    
    
    def _setupWidgets(self):
              
        Label(self.dlg, text="Label header").grid(row=0, sticky=W)
        Label(self.dlg, text="Label font size").grid(row=1, sticky=W)
        Label(self.dlg, text="Label print command").grid(row=2, sticky=W)
        
        
        self.txtLabelHeader = Entry(self.dlg)
        self.txtFontSize = Entry(self.dlg)
        self.txtPrintCommand = Entry(self.dlg)
       
        Button(self.dlg, text="OK", command=self.saveConfig).grid(row=10, column=0, sticky=E+W)
        Button(self.dlg, text="Cancel", command=self.dlg.destroy).grid(row=10, column=1, sticky=E+W) 
        
       
        self.txtLabelHeader.grid(row=0, column=1,sticky=E+W)
        self.txtFontSize.grid(row=1, column=1,sticky=E+W)
        self.txtPrintCommand.grid(row=2, column=1, sticky=E+W)
    
        self.txtLabelHeader.insert(0, self.config.get("Label", "headerLine"))
        self.txtFontSize.insert(0, self.config.get("Label", "fontSize"))
        self.txtPrintCommand.insert(0, self.config.get("Label", "printCommand"))
        
    
    def saveConfig(self):
        self.config.set("Label", "headerLine", self.txtLabelHeader.get())
        self.config.set("Label", "fontSize", self.txtFontSize.get())
        self.config.set("Label", "printCommand", self.txtPrintCommand.get())
        
        with open('comedia.ini', 'wb') as configfile:
            config.write(configfile)

        self.dlg.destroy()
        
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
        
        self.parent.dlg.grab_set()
        self.parent.updateExperimentListbox()
        self.parent.txtVSN.focus_set()
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
        
        if (experimentExists(session, code)):
            tkMessageBox.showerror("Error", "Experiment with code %s exists already." % code)
            return
        
        experiment = model.Experiment()
        experiment.code = code
        
        session.add(experiment)
        session.commit()
        
        self.close()
        
class MyImageWriter(ImageWriter):
    
    def _mm2px(self, mm, dpi=300):
        return (mm * dpi) / 25.4

    def calculate_size(self, modules_per_line, number_of_lines, dpi=300):
        
        width = 2 * self.quiet_zone + modules_per_line * self.module_width
        height = 1.0 + self.module_height * number_of_lines
        if self.text:
            height += (self.font_size + self.text_distance) / 3

        print int(self._mm2px(width, dpi)), int(self._mm2px(height, dpi))
        return int(self._mm2px(width, dpi)), int(self._mm2px(height, dpi))

    def _paint_text(self, xpos, ypos):
        # align font to the left side of the bar code
        xpos = self.quiet_zone
        pos = (self._mm2px(xpos, self.dpi), self._mm2px(ypos, self.dpi))
        font = PIL.ImageFont.truetype(FONT, self.font_size)
        self._draw.text(pos, self.text, font=font, fill=self.foreground)
        
    

def getEmptySlots():   
    
    result =  session.query(model.Slot).order_by(model.Slot.location).filter_by(isActive = 1).order_by(model.Slot.location).filter(model.Slot.moduleID == None)
    
    return(result)

def createConfig():
    
    config.add_section('Label')
    config.set('Label', 'headerLine', 'Correlator Media Library')
    config.set('Label', 'fontSize', '24')
    config.set('Label', 'printCommand', 'lpr -P')
 
    config.add_section('Database')
    config.set('Database', 'server', 'enter database server')
    config.set('Database', 'port', 'enter database server port')
    config.set('Database', 'user', 'enter database user')
    config.set('Database', 'password', 'enter database password')
    config.set('Database', 'database', 'difxdb')
    config.set('Database', 'type', 'mysql')
    

    # Writing our configuration file to 'example.cfg'
    with open(configName, 'wb') as configfile:
        config.write(configfile)
 


if __name__ == "__main__":
    
    dbConn = None
    session = None
    settings = {}
    
    configName = 'comedia.ini'
    
    root = Tk()
    
    mainDlg = MainWindow(None, rootWidget=root)
    
    # read the configuration file
    config = ConfigParser.RawConfigParser()
    
    if os.path.isfile(configName):
        config.read(configName)
        
    else:
        createConfig()
        print "Warning: no initial configuration file ( %s ) found." % configName
        
    if (os.getenv("MARK5_DIR_PATH") == None):
        sys.exit("Error: environment variable MARK5_DIR_PATH must be defined.")
    else:
        settings["dirPath"] = os.getenv("MARK5_DIR_PATH")
        
    
    # try to open the database connection
    connection = Connection()
    connection.type = config.get("Database", "type")
    connection.server = config.get("Database", "server")
    connection.port = config.get("Database", "port")
    connection.user = config.get("Database", "user")
    connection.password = config.get("Database", "password")
    connection.database = config.get("Database", "database")
    
    connection.echo = False
    
    try:
        dbConn = Schema(connection)
        session = dbConn.session()

        mainDlg.isConnected = True
        
    except Exception as e:
        print "Error: ",  e, " Please check your database settings under Options->Database Options"
        mainDlg.isConnected = False
        

    mainDlg.config = config
    
    mainDlg.show()
    
    root.mainloop()


    
    
    
  

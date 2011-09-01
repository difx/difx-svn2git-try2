/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import javax.swing.JCheckBoxMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 *
 * @author jspitzak
 */
public class Mark5NodesHeader extends ClusterNodesHeader {
    
    public Mark5NodesHeader( String name ) {
        super( name );
    }
    
    @Override
    public void createAdditionalItems() {
        super.createAdditionalItems();
        _stateChanged = new ColumnTextArea( "State Changed" );
        this.add( _stateChanged );
        _bankAVSN = new ColumnTextArea( "Bank A" );
        this.add( _bankAVSN );
        _bankBVSN = new ColumnTextArea( "Bank B" );
        this.add( _bankBVSN );
        _statusWord = new ColumnTextArea( "Status Word" );
        this.add( _statusWord );
        _activeBank = new ColumnTextArea( "Active Bank" );
        this.add( _activeBank );
        _scanNumber = new ColumnTextArea( "Scan #" );
        this.add( _scanNumber );
        _scanName = new ColumnTextArea( "Scan Name" );
        this.add( _scanName );
        _position = new ColumnTextArea( "Position" );
        this.add( _position );
        _playRate = new ColumnTextArea( "Play Rate" );
        this.add( _playRate );
        _dataMJD = new ColumnTextArea( "Data MJD" );
        this.add( _dataMJD );
        _currentJob = new ColumnTextArea( "Current Job" );
        this.add( _currentJob );
        _showStateChanged = new JCheckBoxMenuItem( "State Changed" );
        _showStateChanged.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showStateChanged );
        _showBankAVSN = new JCheckBoxMenuItem( "Bank A" );
        _showBankAVSN.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showBankAVSN );
        _showBankBVSN = new JCheckBoxMenuItem( "Bank B" );
        _showBankBVSN.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showBankBVSN );
        _showStatusWord = new JCheckBoxMenuItem( "Status Word" );
        _showStatusWord.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showStatusWord );
        _showActiveBank = new JCheckBoxMenuItem( "Active Bank" );
        _showActiveBank.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showActiveBank );
        _showScanNumber = new JCheckBoxMenuItem( "Scan Number" );
        _showScanNumber.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showScanNumber );
        _showScanName = new JCheckBoxMenuItem( "Scan Name" );
        _showScanName.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showScanName );
        _showPosition = new JCheckBoxMenuItem( "Position" );
        _showPosition.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showPosition );
        _showPlayRate = new JCheckBoxMenuItem( "Play Rate" );
        _showPlayRate.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showPlayRate );
        _showDataMJD = new JCheckBoxMenuItem( "Data MJD" );
        _showDataMJD.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showDataMJD );
        _showCurrentJob = new JCheckBoxMenuItem( "Current Job" );
        _showCurrentJob.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showCurrentJob );
    }
    
    @Override
    public void positionItems() {
        super.positionItems();
        if ( _showStateChanged.getState() )
            setTextArea( _stateChanged, 70 );
        if ( _showBankAVSN.getState() )
            setTextArea( _bankAVSN, 70 );
        if ( _showBankBVSN.getState() )
            setTextArea( _bankBVSN, 70 );
        if ( _showStatusWord.getState() )
            setTextArea( _statusWord, 70 );
        if ( _showActiveBank.getState() )
            setTextArea( _activeBank, 70 );
        if ( _showScanNumber.getState() )
            setTextArea( _scanNumber, 70 );
        if ( _showScanName.getState() )
            setTextArea( _scanName, 70 );
        if ( _showPosition.getState() )
            setTextArea( _position, 70 );
        if ( _showPlayRate.getState() )
            setTextArea( _playRate, 70 );
        if ( _showDataMJD.getState() )
            setTextArea( _dataMJD, 70 );
        if ( _showCurrentJob.getState() )
            setTextArea( _currentJob, 200 );
    }
    
    public void initializeDisplaySettings() {
        super.initializeDisplaySettings();
        _showStateChanged.setState( false );
        _showBankAVSN.setState( false );
        _showBankBVSN.setState( false );
        _showStatusWord.setState( false );
        _showActiveBank.setState( true );
        _showScanNumber.setState( true );
        _showScanName.setState( false );
        _showPosition.setState( false );
        _showPlayRate.setState( false );
        _showDataMJD.setState( false );
        _showCurrentJob.setState( true );
    }
    
    @Override
    public void updateDisplayedData() {
        super.updateDisplayedData();
        for ( Iterator<BrowserNode> iter = _children.iterator(); iter.hasNext(); ) {
            Mark5Node thisNode = (Mark5Node)(iter.next());
            thisNode.showStateChanged( _showStateChanged.getState() );
            thisNode.showBankAVSN( _showBankAVSN.getState() );
            thisNode.showBankBVSN( _showBankBVSN.getState() );
            thisNode.showStatusWord( _showStatusWord.getState() );
            thisNode.showActiveBank( _showActiveBank.getState() );
            thisNode.showScanNumber( _showScanNumber.getState() );
            thisNode.showScanName( _showScanName.getState() );
            thisNode.showPosition( _showPosition.getState() );
            thisNode.showPlayRate( _showPlayRate.getState() );
            thisNode.showDataMJD( _showDataMJD.getState() );
            thisNode.showCurrentJob( _showCurrentJob.getState() );
            thisNode.updateUI();
        }
        _stateChanged.setVisible( _showStateChanged.getState() );
        _bankAVSN.setVisible( _showBankAVSN.getState() );
        _bankBVSN.setVisible( _showBankBVSN.getState() );
        _statusWord.setVisible( _showStatusWord.getState() );
        _activeBank.setVisible( _showActiveBank.getState() );
        _scanNumber.setVisible( _showScanNumber.getState() );
        _scanName.setVisible( _showScanName.getState() );
        _position.setVisible( _showPosition.getState() );
        _playRate.setVisible( _showPlayRate.getState() );
        _dataMJD.setVisible( _showDataMJD.getState() );
        _currentJob.setVisible( _showCurrentJob.getState() );
    }
    
    protected JCheckBoxMenuItem _showStateChanged;
    protected JCheckBoxMenuItem _showBankAVSN;
    protected JCheckBoxMenuItem _showBankBVSN;
    protected JCheckBoxMenuItem _showStatusWord;
    protected JCheckBoxMenuItem _showActiveBank;
    protected JCheckBoxMenuItem _showScanNumber;
    protected JCheckBoxMenuItem _showScanName;
    protected JCheckBoxMenuItem _showPosition;
    protected JCheckBoxMenuItem _showPlayRate;
    protected JCheckBoxMenuItem _showDataMJD;
    protected JCheckBoxMenuItem _showCurrentJob;

    protected ColumnTextArea _stateChanged;
    protected ColumnTextArea _bankAVSN;
    protected ColumnTextArea _bankBVSN;
    protected ColumnTextArea _statusWord;
    protected ColumnTextArea _activeBank;
    protected ColumnTextArea _scanNumber;
    protected ColumnTextArea _scanName;
    protected ColumnTextArea _position;
    protected ColumnTextArea _playRate;
    protected ColumnTextArea _dataMJD;
    protected ColumnTextArea _currentJob;
}

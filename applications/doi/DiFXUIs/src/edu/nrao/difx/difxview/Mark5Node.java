/*
 * This is a browser node used to display Mark5 data.  It inherits the "ClusterNode"
 * class, as it shares many of the data display traits with that class.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.ActivityMonitorLight;
import edu.nrao.difx.difxdatamodel.Mark5Unit;

import javax.swing.JSeparator;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Color;

/**
 *
 * @author jspitzak
 */
public class Mark5Node extends ClusterNode {
    
    public Mark5Node( String name, SystemSettings settings ) {
        super( name, settings );
    }
    
    @Override
    public void createAdditionalItems() {
        super.createAdditionalItems();
        _stateChanged = new ActivityMonitorLight();
        this.add( _stateChanged );
        _bankAVSN = new ColumnTextArea();
        _bankAVSN.justify( ColumnTextArea.RIGHT );
        this.add( _bankAVSN );
        _bankBVSN = new ColumnTextArea();
        _bankBVSN.justify( ColumnTextArea.RIGHT );
        this.add( _bankBVSN );
        _statusWord = new ColumnTextArea();
        _statusWord.justify( ColumnTextArea.RIGHT );
        this.add( _statusWord );
        _activeBank = new ColumnTextArea();
        _activeBank.justify( ColumnTextArea.RIGHT );
        this.add( _activeBank );
        _scanNumber = new ColumnTextArea();
        _scanNumber.justify( ColumnTextArea.RIGHT );
        this.add( _scanNumber );
        _scanName = new ColumnTextArea();
        _scanName.justify( ColumnTextArea.RIGHT );
        this.add( _scanName );
        _position = new ColumnTextArea();
        _position.justify( ColumnTextArea.RIGHT );
        this.add( _position );
        _playRate = new ColumnTextArea();
        _playRate.justify( ColumnTextArea.RIGHT );
        this.add( _playRate );
        _dataMJD = new ColumnTextArea();
        _dataMJD.justify( ColumnTextArea.RIGHT );
        this.add( _dataMJD );
        _currentJob = new ColumnTextArea();
        _currentJob.justify( ColumnTextArea.RIGHT );
        this.add( _currentJob );
        _popup.add( new JSeparator() );
        JMenuItem startItem = new JMenuItem( "Start" );
        startItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                sendDiFXCommandMessage( "StartMark5A" );
            }
        });
        _popup.add( startItem );
        JMenuItem stopItem = new JMenuItem( "Stop" );
        stopItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                sendDiFXCommandMessage( "StopMark5A" );
            }
        });
        _popup.add( stopItem );
        JMenuItem clearItem = new JMenuItem( "Clear" );
        clearItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                sendDiFXCommandMessage( "Clear" );
            }
        });
        _popup.add( clearItem );
        JMenuItem copyItem = new JMenuItem( "Copy" );
        copyItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                sendDiFXCommandMessage( "Copy" );
            }
        });
        _popup.add( copyItem );
        JMenuItem getVSNItem = new JMenuItem( "Get VSN" );
        getVSNItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                sendDiFXCommandMessage( "GetVSN" );
            }
        });
        _popup.add( getVSNItem );
        JMenuItem getLoadItem = new JMenuItem( "Get Load" );
        getLoadItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                sendDiFXCommandMessage( "GetLoad" );
            }
        });
        _popup.add( getLoadItem );
        JMenuItem getDirItem = new JMenuItem( "Get Directory" );
        getDirItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                sendDiFXCommandMessage( "GetDir" );
            }
        });
        _popup.add( getDirItem );
    }
    
    @Override
    public void positionItems() {
        super.positionItems();
        if ( _showStateChanged ) {
            _stateChanged.setBounds( _xOff + ( _widthStateChanged - 10 ) / 2, 6, 10, 10 );
            _xOff += _widthStateChanged;
            _colorColumn = false;
        }
        if ( _showBankAVSN ) {
            _bankAVSN.setBounds( _xOff, 1, _widthBankAVSN, _ySize - 2);
            if ( _activeBank.getText().equalsIgnoreCase( "A" ) ) {
                _bankAVSN.setForeground( Color.BLACK );
                _bankAVSN.setBackground( Color.GREEN );
            }
            else {
                _bankAVSN.setForeground( Color.DARK_GRAY );
                _bankAVSN.setBackground( Color.LIGHT_GRAY );
            }
            _xOff += _widthBankAVSN;
            _colorColumn = false;
        }
        if ( _showBankBVSN ) {
            _bankBVSN.setBounds( _xOff, 1, _widthBankBVSN, _ySize - 2);
            if ( _activeBank.getText().equalsIgnoreCase( "B" ) ) {
                _bankBVSN.setForeground( Color.BLACK );
                _bankBVSN.setBackground( Color.GREEN );
            }
            else {
                _bankBVSN.setForeground( Color.DARK_GRAY );
                _bankBVSN.setBackground( Color.LIGHT_GRAY );
            }
            _xOff += _widthBankBVSN;
            _colorColumn = false;
        }
        if ( _showStatusWord )
            setTextArea( _statusWord, _widthStatusWord );
        if ( _showActiveBank )
            setTextArea( _activeBank, _widthActiveBank );
        if ( _showScanNumber )
            setTextArea( _scanNumber, _widthScanNumber );

        if ( _showScanName )
            setTextArea( _scanName, _widthScanName );
        if ( _showPosition )
            setTextArea( _position, _widthPosition );
        if ( _showPlayRate )
            setTextArea( _playRate, _widthPlayRate );
        if ( _showDataMJD )
            setTextArea( _dataMJD, _widthDataMJD );
        if ( _showCurrentJob )
            setTextArea( _currentJob, _widthCurrentJob );
    }
    
    
    public void showStateChanged( boolean newVal ) {
        _showStateChanged = newVal;
        _stateChanged.setVisible( newVal );            
    }
    public void showBankAVSN( boolean newVal ) {
        _showBankAVSN = newVal;
        _bankAVSN.setVisible( newVal );            
    }
    public void showBankBVSN( boolean newVal ) {
        _showBankBVSN = newVal;
        _bankBVSN.setVisible( newVal );            
    }
    public void showStatusWord( boolean newVal ) {
        _showStatusWord = newVal;
        _statusWord.setVisible( newVal );            
    }
    public void showActiveBank( boolean newVal ) {
        _showActiveBank = newVal;
        _activeBank.setVisible( newVal );            
    }
    public void showScanNumber( boolean newVal ) {
        _showScanNumber = newVal;
        _scanNumber.setVisible( newVal );            
    }
    public void showScanName( boolean newVal ) {
        _showScanName = newVal;
        _scanName.setVisible( newVal );            
    }
    public void showPosition( boolean newVal ) {
        _showPosition = newVal;
        _position.setVisible( newVal );            
    }
    public void showPlayRate( boolean newVal ) {
        _showPlayRate = newVal;
        _playRate.setVisible( newVal );            
    }
    public void showDataMJD( boolean newVal ) {
        _showDataMJD = newVal;
        _dataMJD.setVisible( newVal );            
    }
    public void showCurrentJob( boolean newVal ) {
        _showCurrentJob = newVal;
        _currentJob.setVisible( newVal );            
    }
    
    public void setData( Mark5Unit newData ) {
        super.setData( newData );
        _stateChanged.on( newData.isStateChanged() );
        _bankAVSN.setText( newData.getBankAVSN() );
        //  Add this module to our list of modules (if its not there already).
        if ( !_settings.dataSourceInList( newData.getBankAVSN(), "VSN" ) ) {
            if ( newData.getBankAVSN().length() > 0 && !newData.getBankAVSN().equalsIgnoreCase( "NONE" ) )
                _settings.addDataSource( newData.getBankAVSN(), "VSN", "hardware" );
        }
        _bankBVSN.setText( newData.getBankBVSN() );
        //  Add this module to our list of modules (if its not there already).
        if ( !_settings.dataSourceInList( newData.getBankBVSN(), "VSN" ) ) {
            if ( newData.getBankBVSN().length() > 0 && !newData.getBankBVSN().equalsIgnoreCase( "NONE" ) )
                _settings.addDataSource( newData.getBankBVSN(), "VSN", "hardware" );
        }
        _statusWord.setText( newData.getStatusWord() );
        _activeBank.setText( newData.getActiveBank() );
        _scanNumber.setText( String.format( "%10d", newData.getScanNumber() ) );
        _scanName.setText( newData.getScanName() );
        _position.setText( String.format( "%10d", newData.getPosition() ) );
        _playRate.setText( String.format( "%10.3f", newData.getPlayRate() ) );
        _dataMJD.setText( newData.getDataMJD().toString() );
        _currentJob.setText( newData.getCurrentJob() );
    }
    
    public void widthStateChanged( int newVal ) { _widthStateChanged = newVal; }
    public void widthBankAVSN( int newVal ) { _widthBankAVSN = newVal; }
    public void widthBankBVSN( int newVal ) { _widthBankBVSN = newVal; }
    public void widthStatusWord( int newVal ) { _widthStatusWord = newVal; }
    public void widthActiveBank( int newVal ) { _widthActiveBank = newVal; }
    public void widthScanNumber( int newVal ) { _widthScanNumber = newVal; }
    public void widthScanName( int newVal ) { _widthScanName = newVal; }
    public void widthPosition( int newVal ) { _widthPosition = newVal; }
    public void widthPlayRate( int newVal ) { _widthPlayRate = newVal; }
    public void widthDataMJD( int newVal ) { _widthDataMJD = newVal; }
    public void widthCurrentJob( int newVal ) { _widthCurrentJob = newVal; }
    
    public String bankAVSN() { return _bankAVSN.getText(); }
    public String bankBVSN() { return _bankBVSN.getText(); }
    
    protected ActivityMonitorLight _stateChanged;
    protected boolean _showStateChanged;
    protected ColumnTextArea _bankAVSN;
    protected boolean _showBankAVSN;
    protected ColumnTextArea _bankBVSN;
    protected boolean _showBankBVSN;
    protected ColumnTextArea _statusWord;
    protected boolean _showStatusWord;
    protected ColumnTextArea _activeBank;
    protected boolean _showActiveBank;
    protected ColumnTextArea _scanNumber;
    protected boolean _showScanNumber;
    protected ColumnTextArea _scanName;
    protected boolean _showScanName;
    protected ColumnTextArea _position;
    protected boolean _showPosition;
    protected ColumnTextArea _playRate;
    protected boolean _showPlayRate;
    protected ColumnTextArea _dataMJD;
    protected boolean _showDataMJD;
    protected ColumnTextArea _currentJob;
    protected boolean _showCurrentJob;

    protected int _widthStateChanged;
    protected int _widthBankAVSN;
    protected int _widthBankBVSN;
    protected int _widthStatusWord;
    protected int _widthActiveBank;
    protected int _widthScanNumber;
    protected int _widthScanName;
    protected int _widthPosition;
    protected int _widthPlayRate;
    protected int _widthDataMJD;
    protected int _widthCurrentJob;
    
}

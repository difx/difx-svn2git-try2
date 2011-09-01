/*
 * This is a browser node used to display Mark5 data.  It inherits the "ClusterNode"
 * class, as it shares many of the data display traits with that class.
 */
package edu.nrao.difx.difxview;

import edu.nrao.difx.difxdatamodel.Mark5Unit;

/**
 *
 * @author jspitzak
 */
public class Mark5Node extends ClusterNode {
    
    public Mark5Node( String name ) {
        super( name );
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
    }
    
    @Override
    public void positionItems() {
        super.positionItems();
        if ( _showStateChanged ) {
            _stateChanged.setBounds( _xOff + 30, 6, 10, 10 );
            _xOff += 70;
            _colorColumn = false;
        }
        if ( _showBankAVSN )
            setTextArea( _bankAVSN, 70 );
        if ( _showBankBVSN )
            setTextArea( _bankBVSN, 70 );
        if ( _showStatusWord )
            setTextArea( _statusWord, 70 );
        if ( _showActiveBank )
            setTextArea( _activeBank, 70 );
        if ( _showScanNumber )
            setTextArea( _scanNumber, 70 );

        if ( _showScanName )
            setTextArea( _scanName, 70 );
        if ( _showPosition )
            setTextArea( _position, 70 );
        if ( _showPlayRate )
            setTextArea( _playRate, 70 );
        if ( _showDataMJD )
            setTextArea( _dataMJD, 70 );
        if ( _showCurrentJob )
            setTextArea( _currentJob, 200 );
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
        _bankBVSN.setText( newData.getBankBVSN() );
        _statusWord.setText( newData.getStatusWord() );
        _activeBank.setText( newData.getActiveBank() );
        _scanNumber.setText( String.format( "%10d", newData.getScanNumber() ) );
        _scanName.setText( newData.getScanName() );
        _position.setText( String.format( "%10d", newData.getPosition() ) );
        _playRate.setText( String.format( "%10.3f", newData.getPlayRate() ) );
        _dataMJD.setText( newData.getDataMJD().toString() );
        _currentJob.setText( newData.getCurrentJob() );
    }
    
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

}

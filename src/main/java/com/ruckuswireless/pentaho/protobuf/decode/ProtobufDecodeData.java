package com.ruckuswireless.pentaho.protobuf.decode;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * Protocol Buffers step data being in process
 * 
 * @author Michael Spector
 */
public class ProtobufDecodeData extends BaseStepData implements StepDataInterface {

	ProtobufDecoder decoder;
	RowMetaInterface outputRowMeta;
	boolean canceled;
	int processed;
}

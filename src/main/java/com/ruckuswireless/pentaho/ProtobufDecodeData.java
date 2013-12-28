package com.ruckuswireless.pentaho;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

public class ProtobufDecodeData extends BaseStepData implements StepDataInterface {

	RowMetaInterface outputRowMeta;
	boolean canceled;
	int processed;
}

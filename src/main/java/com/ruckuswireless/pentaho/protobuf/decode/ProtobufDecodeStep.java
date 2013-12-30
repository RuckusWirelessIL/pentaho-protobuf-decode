package com.ruckuswireless.pentaho.protobuf.decode;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import com.ruckuswireless.pentaho.protobuf.decode.ProtobufDecoder.ProtobufDecoderException;

/**
 * Main processing unit for the Protocol Buffers Decode step
 * 
 * @author Michael Spector
 */
public class ProtobufDecodeStep extends BaseStep implements StepInterface {

	public ProtobufDecodeStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
			Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		super.init(smi, sdi);

		ProtobufDecodeMeta meta = (ProtobufDecodeMeta) smi;
		ProtobufDecodeData data = (ProtobufDecodeData) sdi;
		try {
			data.decoder = new ProtobufDecoder(meta.getClasspath(), meta.getRootClass());
		} catch (ProtobufDecoderException e) {
			logError(Messages.getString("ProtobufDecodeStep.Init.Error", getStepname()), e);
			return false;
		}
		return true;
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		Object[] r = getRow();
		if (r == null) {
			setOutputDone();
			return false;
		}

		// ProtobufDecodeMeta meta = (ProtobufDecodeMeta) smi;
		ProtobufDecodeData data = (ProtobufDecodeData) sdi;

		if (first) {
			first = false;
			data.outputRowMeta = getInputRowMeta().clone();
			RowMetaInterface rowMeta = new RowMeta();
			// XXX: add new fields
			data.outputRowMeta.mergeRowMeta(rowMeta);
		}

		try {
			// XXX: this is just an example
			Object[] someData = null;
			r = RowDataUtil.addRowData(r, getInputRowMeta().size(), someData);
			putRow(data.outputRowMeta, r);

			if (isRowLevel()) {
				logRowlevel(Messages.getString("ProtobufDecodeStep.Log.OutputRow", Long.toString(getLinesWritten()),
						data.outputRowMeta.getString(r)));
			}
		} catch (KettleException e) {
			if (!getStepMeta().isDoingErrorHandling()) {
				logError(Messages.getString("ProtobufDecodeStep.ErrorInStepRunning", e.getMessage()));
				setErrors(1);
				stopAll();
				setOutputDone();
				return false;
			}
			putError(getInputRowMeta(), r, 1, e.toString(), null, getStepname());
		}
		return true;
	}

	public void stopRunning(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {

		ProtobufDecodeData data = (ProtobufDecodeData) sdi;
		data.canceled = true;

		super.stopRunning(smi, sdi);
	}
}

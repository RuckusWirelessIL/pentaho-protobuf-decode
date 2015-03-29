package com.ruckuswireless.pentaho.protobuf.decode;

import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
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

	public ProtobufDecodeStep(StepMeta stepMeta,
			StepDataInterface stepDataInterface, int copyNr,
			TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		super.init(smi, sdi);

		ProtobufDecodeMeta meta = (ProtobufDecodeMeta) smi;
		ProtobufDecodeData data = (ProtobufDecodeData) sdi;
		try {
			data.decoder = new ProtobufDecoder(
					environmentSubstitute(meta.getClasspath()),
					meta.getRootClass(), meta.getFields());
		} catch (ProtobufDecoderException e) {
			logError(Messages.getString("ProtobufDecodeStep.Dispose.Error",
					getStepname()), e);
			return false;
		}
		return true;
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {

		ProtobufDecodeData data = (ProtobufDecodeData) sdi;
		if (data.decoder != null) {
			try {
				data.decoder.dispose();
			} catch (ProtobufDecoderException e) {
				logError(Messages.getString("ProtobufDecodeStep.Init.Error",
						getStepname()), e);
			}
			data.decoder = null;
		}
		super.dispose(smi, sdi);
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi)
			throws KettleException {
		Object[] r = getRow();
		if (r == null) {
			setOutputDone();
			return false;
		}

		ProtobufDecodeMeta meta = (ProtobufDecodeMeta) smi;
		ProtobufDecodeData data = (ProtobufDecodeData) sdi;

		if (first) {
			first = false;
			data.outputRowMeta = getInputRowMeta().clone();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this);

			String inputField = environmentSubstitute(meta.getInputField());

			int numErrors = 0;
			if (Const.isEmpty(inputField)) {
				logError(Messages
						.getString("ProtobufDecodeStep.Log.FieldNameIsNull")); //$NON-NLS-1$
				numErrors++;
			}
			data.inputFieldNr = getInputRowMeta().indexOfValue(inputField);
			if (data.inputFieldNr < 0) {
				logError(Messages.getString(
						"ProtobufDecodeStep.Log.CouldntFindField", inputField)); //$NON-NLS-1$
				numErrors++;
			}
			if (!getInputRowMeta().getValueMeta(data.inputFieldNr).isBinary()) {
				logError(Messages.getString(
						"ProtobufDecodeStep.Log.FieldNotValid", inputField)); //$NON-NLS-1$
				numErrors++;
			}
			if (numErrors > 0) {
				setErrors(numErrors);
				stopAll();
				return false;
			}
			data.inputFieldMeta = getInputRowMeta().getValueMeta(
					data.inputFieldNr);
		}

		try {
			byte[] message = data.inputFieldMeta
					.getBinary(r[data.inputFieldNr]);
			try {
				List<Object[]> decodedData = data.decoder.decode(message);
				for (Object[] d : decodedData) {
					r = RowDataUtil.addRowData(r, getInputRowMeta().size(), d);
					putRow(data.outputRowMeta, r);
					if (isRowLevel()) {
						logRowlevel(Messages.getString(
								"ProtobufDecodeStep.Log.OutputRow",
								Long.toString(getLinesWritten()),
								data.outputRowMeta.getString(r)));
					}
				}
			} catch (ProtobufDecoderException e) {
				throw new KettleException(e);
			}
		} catch (KettleException e) {
			if (!getStepMeta().isDoingErrorHandling()) {
				logError(Messages
						.getString("ProtobufDecodeStep.ErrorInStepRunning",
								e.getMessage()));
				setErrors(1);
				stopAll();
				setOutputDone();
				return false;
			}
			putError(getInputRowMeta(), r, 1, e.toString(), null, getStepname());
		}
		return true;
	}

	public void stopRunning(StepMetaInterface smi, StepDataInterface sdi)
			throws KettleException {

		ProtobufDecodeData data = (ProtobufDecodeData) sdi;
		data.canceled = true;

		super.stopRunning(smi, sdi);
	}
}

package com.ruckuswireless.pentaho.protobuf.decode;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

import com.ruckuswireless.pentaho.protobuf.decode.ProtobufDecoder.ProtobufDecoderException;

/**
 * Protocol Buffers transformation step definitions
 * 
 * @author Michael Spector
 */
public class ProtobufDecodeMeta extends BaseStepMeta implements StepMetaInterface {

	private FieldDefinition[] fields;
	private File jarFile;
	private String rootClass;

	/**
	 * @return fields definitions
	 */
	public FieldDefinition[] getFields() {
		return fields;
	}

	/**
	 * @param fields
	 *            Fields definitions
	 */
	public void setFields(FieldDefinition[] fields) {
		this.fields = fields;
	}

	/**
	 * @return Path of the Jar file containing compiled Protocol Buffer Java
	 *         classes
	 */
	public File getJarFile() {
		return jarFile;
	}

	/**
	 * @param jarFile
	 *            Path of the Jar file containing compiled Protocol Buffer Java
	 *            classes
	 */
	public void setJarFile(File jarFile) {
		this.jarFile = jarFile;
	}

	/**
	 * @return Root class in object hierarchy
	 */
	public String getRootClass() {
		return rootClass;
	}

	/**
	 * @param rootClass
	 *            Root class in object hierarchy
	 */
	public void setRootClass(String rootClass) {
		this.rootClass = rootClass;
	}

	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
			RowMetaInterface prev, String input[], String output[], RowMetaInterface info) {
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
			Trans trans) {
		return new ProtobufDecodeStep(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData() {
		return new ProtobufDecodeData();
	}

	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters)
			throws KettleXMLException {

		try {
		} catch (Exception e) {
			throw new KettleXMLException(Messages.getString("ProtobufDecodeMeta.Exception.loadXml"), e);
		}
	}

	public String getXML() throws KettleException {
		StringBuilder retval = new StringBuilder();
		return retval.toString();
	}

	public void readRep(Repository rep, ObjectId stepId, List<DatabaseMeta> databases, Map<String, Counter> counters)
			throws KettleException {
		try {
		} catch (Exception e) {
			throw new KettleException("ProtobufDecodeMeta.Exception.loadRep", e);
		}
	}

	public void saveRep(Repository rep, ObjectId transformationId, ObjectId stepId) throws KettleException {
		try {
		} catch (Exception e) {
			throw new KettleException("ProtobufDecodeMeta.Exception.saveRep", e);
		}
	}

	public void setDefault() {
	}

	public void getFields(RowMetaInterface rowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep,
			VariableSpace space) throws KettleStepException {

		rowMeta.clear();

		try {
			ProtobufDecoder protobufDecoder = new ProtobufDecoder(jarFile, rootClass);
			Map<String, Class<?>> detectedFields = protobufDecoder.guessFields();

			for (Entry<String, Class<?>> e : detectedFields.entrySet()) {

				String fieldPath = e.getKey();
				int i = fieldPath.lastIndexOf('.');
				String fieldName = i == -1 ? fieldPath : fieldPath.substring(i + 1);

				Class<?> type = e.getValue();
				int kettleType;
				if (type == double.class || type == float.class) {
					kettleType = ValueMetaInterface.TYPE_NUMBER;
				} else if (type == String.class) {
					kettleType = ValueMetaInterface.TYPE_STRING;
				} else if (type == long.class || type == int.class) {
					kettleType = ValueMetaInterface.TYPE_INTEGER;
				} else if (type == Date.class) {
					kettleType = ValueMetaInterface.TYPE_DATE;
				} else if (type == boolean.class) {
					kettleType = ValueMetaInterface.TYPE_BOOLEAN;
				} else if (type == byte[].class) {
					kettleType = ValueMetaInterface.TYPE_BINARY;
				} else {
					kettleType = ValueMetaInterface.TYPE_NONE;
				}

				ValueMetaInterface valueMeta = new ValueMeta(fieldName, kettleType);
				valueMeta.setOrigin(origin);
				rowMeta.addValueMeta(valueMeta);
			}
		} catch (ProtobufDecoderException e) {
			throw new KettleStepException("Error detecting fields", e);
		}
	}
}

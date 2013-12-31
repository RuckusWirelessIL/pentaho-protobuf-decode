package com.ruckuswireless.pentaho.protobuf.decode;

import java.util.List;
import java.util.Map;

import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
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

/**
 * Protocol Buffers transformation step definitions
 * 
 * @author Michael Spector
 */
public class ProtobufDecodeMeta extends BaseStepMeta implements StepMetaInterface {

	private String inputField;
	private String[] classpath;
	private String rootClass;
	private FieldDefinition[] fields;

	/**
	 * @return field name from incoming stream, which contains encoded Protocol
	 *         Buffers message
	 */
	public String getInputField() {
		return inputField;
	}

	/**
	 * @param inputField
	 *            Field name from incoming stream, which contains encoded
	 *            Protocol Buffers message
	 */
	public void setInputField(String inputField) {
		this.inputField = inputField;
	}

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
	 * @return Classpath containing compiled Protocol Buffer Java classes
	 */
	public String[] getClasspath() {
		return classpath;
	}

	/**
	 * @param classpath
	 *            Classpath containing compiled Protocol Buffer Java classes
	 */
	public void setClasspath(String[] classpath) {
		this.classpath = classpath;
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
			inputField = XMLHandler.getTagValue(stepnode, "INPUT_FIELD");

			Node classpathNode = XMLHandler.getSubNode(stepnode, "CLASSPATH");
			if (classpathNode != null) {
				int nrfields = XMLHandler.countNodes(classpathNode, "PATH");
				classpath = new String[nrfields];
				for (int i = 0; i < nrfields; ++i) {
					Node pathNode = XMLHandler.getSubNodeByNr(classpathNode, "PATH", i);
					classpath[i] = XMLHandler.getNodeValue(pathNode);
				}
			}

			rootClass = XMLHandler.getTagValue(stepnode, "ROOT_CLASS");

			Node fieldsNode = XMLHandler.getSubNode(stepnode, "FIELDS");
			if (fieldsNode != null) {
				int nrfields = XMLHandler.countNodes(fieldsNode, "FIELD");
				fields = new FieldDefinition[nrfields];
				for (int i = 0; i < nrfields; i++) {
					fields[i] = new FieldDefinition();
					Node fnode = XMLHandler.getSubNodeByNr(fieldsNode, "FIELD", i);
					fields[i].name = XMLHandler.getTagValue(fnode, "NAME");
					fields[i].path = XMLHandler.getTagValue(fnode, "PATH");
					fields[i].type = ValueMeta.getType(XMLHandler.getTagValue(fnode, "TYPE"));
				}
			}
		} catch (Exception e) {
			throw new KettleXMLException(Messages.getString("ProtobufDecodeMeta.Exception.loadXml"), e);
		}
	}

	public String getXML() throws KettleException {
		StringBuilder retval = new StringBuilder();

		if (inputField != null) {
			retval.append("    ").append(XMLHandler.addTagValue("INPUT_FIELD", inputField));
		}
		if (classpath != null) {
			retval.append("    ").append(XMLHandler.openTag("CLASSPATH")).append(Const.CR);
			for (String path : classpath) {
				retval.append("      " + XMLHandler.addTagValue("PATH", path));
			}
			retval.append("    ").append(XMLHandler.closeTag("CLASSPATH")).append(Const.CR);
		}
		if (rootClass != null) {
			retval.append("    ").append(XMLHandler.addTagValue("ROOT_CLASS", rootClass));
		}
		if (fields != null) {
			retval.append("    ").append(XMLHandler.openTag("FIELDS")).append(Const.CR);
			for (FieldDefinition field : fields) {
				retval.append("      ").append(XMLHandler.openTag("FIELD")).append(Const.CR);
				retval.append("        " + XMLHandler.addTagValue("NAME", field.name));
				retval.append("        " + XMLHandler.addTagValue("PATH", field.path));
				retval.append("        " + XMLHandler.addTagValue("TYPE", ValueMeta.getTypeDesc(field.type)));
				retval.append("      ").append(XMLHandler.closeTag("FIELD")).append(Const.CR);
			}
			retval.append("    ").append(XMLHandler.closeTag("FIELDS")).append(Const.CR);
		}
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

		for (FieldDefinition field : fields) {
			ValueMetaInterface valueMeta = new ValueMeta(field.name, field.type);
			valueMeta.setOrigin(origin);
			rowMeta.addValueMeta(valueMeta);
		}
	}
}

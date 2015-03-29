package com.ruckuswireless.pentaho.protobuf.decode;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.trans.step.TableItemInsertListener;

import com.ruckuswireless.pentaho.protobuf.decode.ProtobufDecoder.ProtobufDecoderException;
import com.ruckuswireless.pentaho.utils.KettleTypesConverter;

/**
 * UI for the Protocol Buffers step
 * 
 * @author Michael Spector
 */
public class ProtobufDecodeDialog extends BaseStepDialog implements
		StepDialogInterface {

	private ProtobufDecodeMeta meta;
	private Button wDetectFields;
	private Listener lsDetectFields;
	private CCombo wInputField;
	private Button wbbClasspath;
	private TextVar wClasspath;
	private TextVar wRootClass;
	private TableView wFields;

	public ProtobufDecodeDialog(Shell parent, Object in, TransMeta tr,
			String sname) {
		super(parent, (BaseStepMeta) in, tr, sname);
		meta = (ProtobufDecodeMeta) in;
	}

	public ProtobufDecodeDialog(Shell parent, BaseStepMeta baseStepMeta,
			TransMeta transMeta, String stepname) {
		super(parent, baseStepMeta, transMeta, stepname);
		meta = (ProtobufDecodeMeta) baseStepMeta;
	}

	public ProtobufDecodeDialog(Shell parent, int nr, BaseStepMeta in,
			TransMeta tr) {
		super(parent, nr, in, tr);
		meta = (ProtobufDecodeMeta) in;
	}

	public String open() {
		Shell parent = getParent();
		Display display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN
				| SWT.MAX);
		props.setLook(shell);
		setShellImage(shell, meta);

		ModifyListener lsMod = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				meta.setChanged();
			}
		};
		changed = meta.hasChanged();

		FormLayout formLayout = new FormLayout();
		formLayout.marginWidth = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(Messages.getString("ProtobufDecodeDialog.Shell.Title"));

		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;

		// Step name
		wlStepname = new Label(shell, SWT.RIGHT);
		wlStepname.setText(Messages
				.getString("ProtobufDecodeDialog.StepName.Label"));
		props.setLook(wlStepname);
		fdlStepname = new FormData();
		fdlStepname.left = new FormAttachment(0, 0);
		fdlStepname.right = new FormAttachment(middle, -margin);
		fdlStepname.top = new FormAttachment(0, margin);
		wlStepname.setLayoutData(fdlStepname);
		wStepname = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wStepname);
		wStepname.addModifyListener(lsMod);
		fdStepname = new FormData();
		fdStepname.left = new FormAttachment(middle, 0);
		fdStepname.top = new FormAttachment(0, margin);
		fdStepname.right = new FormAttachment(100, 0);
		wStepname.setLayoutData(fdStepname);
		Control lastControl = wStepname;

		// Input field
		RowMetaInterface previousFields;
		try {
			previousFields = transMeta.getPrevStepFields(stepMeta);
		} catch (KettleStepException e) {
			new ErrorDialog(
					shell,
					BaseMessages.getString("System.Dialog.Error.Title"),
					Messages.getString("ProtobufDecodeDialog.ErrorDialog.UnableToGetInputFields.Message"),
					e);
			previousFields = new RowMeta();
		}
		Label wlInputField = new Label(shell, SWT.RIGHT);
		wlInputField.setText(Messages
				.getString("ProtobufDecodeDialog.InputField.Label"));
		props.setLook(wlInputField);
		FormData fdlInputField = new FormData();
		fdlInputField.top = new FormAttachment(lastControl, margin);
		fdlInputField.left = new FormAttachment(0, 0);
		fdlInputField.right = new FormAttachment(middle, -margin);
		wlInputField.setLayoutData(fdlInputField);
		wInputField = new CCombo(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		wInputField.setItems(previousFields.getFieldNames());
		props.setLook(wInputField);
		wInputField.addModifyListener(lsMod);
		FormData fdFilename = new FormData();
		fdFilename.top = new FormAttachment(lastControl, margin);
		fdFilename.left = new FormAttachment(middle, 0);
		fdFilename.right = new FormAttachment(100, 0);
		wInputField.setLayoutData(fdFilename);
		lastControl = wInputField;

		// Jar file...
		//
		// The filename browse button
		//
		wbbClasspath = new Button(shell, SWT.PUSH | SWT.CENTER);
		props.setLook(wbbClasspath);
		wbbClasspath.setText(Messages
				.getString("ProtobufDecodeDialog.Classpath.Browse.Label"));
		wbbClasspath.setToolTipText(Messages
				.getString("ProtobufDecodeDialog.Classpath.Browse.Label"));
		FormData fdbFilename = new FormData();
		fdbFilename.top = new FormAttachment(lastControl, margin);
		fdbFilename.right = new FormAttachment(100, 0);
		wbbClasspath.setLayoutData(fdbFilename);
		// The field itself...
		//
		Label wlClasspath = new Label(shell, SWT.RIGHT);
		wlClasspath.setText(Messages
				.getString("ProtobufDecodeDialog.Classpath.Label"));
		props.setLook(wlClasspath);
		FormData fdlFilename = new FormData();
		fdlFilename.top = new FormAttachment(lastControl, margin);
		fdlFilename.left = new FormAttachment(0, 0);
		fdlFilename.right = new FormAttachment(middle, -margin);
		wlClasspath.setLayoutData(fdlFilename);
		wClasspath = new TextVar(transMeta, shell, SWT.SINGLE | SWT.LEFT
				| SWT.BORDER);
		props.setLook(wClasspath);
		wClasspath.addModifyListener(lsMod);
		FormData fdClasspath = new FormData();
		fdClasspath.top = new FormAttachment(lastControl, margin);
		fdClasspath.left = new FormAttachment(middle, 0);
		fdClasspath.right = new FormAttachment(wbbClasspath, -margin);
		wClasspath.setLayoutData(fdClasspath);
		lastControl = wClasspath;

		// Topic name
		Label wlRootClass = new Label(shell, SWT.RIGHT);
		wlRootClass.setText(Messages
				.getString("ProtobufDecodeDialog.RootClass.Label"));
		props.setLook(wlRootClass);
		FormData fdlRootClass = new FormData();
		fdlRootClass.top = new FormAttachment(lastControl, margin);
		fdlRootClass.left = new FormAttachment(0, 0);
		fdlRootClass.right = new FormAttachment(middle, -margin);
		wlRootClass.setLayoutData(fdlRootClass);
		wRootClass = new TextVar(transMeta, shell, SWT.SINGLE | SWT.LEFT
				| SWT.BORDER);
		props.setLook(wRootClass);
		wRootClass.addModifyListener(lsMod);
		FormData fdTopicName = new FormData();
		fdTopicName.top = new FormAttachment(lastControl, margin);
		fdTopicName.left = new FormAttachment(middle, 0);
		fdTopicName.right = new FormAttachment(100, 0);
		wRootClass.setLayoutData(fdTopicName);
		lastControl = wRootClass;

		// Buttons
		wOK = new Button(shell, SWT.PUSH);
		wOK.setText(BaseMessages.getString("System.Button.OK")); //$NON-NLS-1$
		wDetectFields = new Button(shell, SWT.PUSH);
		wDetectFields.setText(Messages
				.getString("ProtobufDecodeDialog.DetectFields.Button")); //$NON-NLS-1$
		wCancel = new Button(shell, SWT.PUSH);
		wCancel.setText(BaseMessages.getString("System.Button.Cancel")); //$NON-NLS-1$

		// Fields
		Label wlFields = new Label(shell, SWT.NONE);
		wlFields.setText(Messages
				.getString("ProtobufDecodeDialog.Fields.Label")); //$NON-NLS-1$
		props.setLook(wlFields);
		FormData fdlFields = new FormData();
		fdlFields.left = new FormAttachment(0, 0);
		fdlFields.top = new FormAttachment(lastControl, margin * 2);
		wlFields.setLayoutData(fdlFields);

		ColumnInfo[] ciFields = new ColumnInfo[3];
		ciFields[0] = new ColumnInfo(
				Messages.getString("ProtobufDecodeDialog.ColumnInfo.Field"), ColumnInfo.COLUMN_TYPE_TEXT, false); //$NON-NLS-1$
		ciFields[1] = new ColumnInfo(
				Messages.getString("ProtobufDecodeDialog.ColumnInfo.Path"), ColumnInfo.COLUMN_TYPE_TEXT, false); //$NON-NLS-1$
		ciFields[2] = new ColumnInfo(
				Messages.getString("ProtobufDecodeDialog.ColumnInfo.Type"), ColumnInfo.COLUMN_TYPE_CCOMBO, ValueMeta.getTypes()); //$NON-NLS-1$

		wFields = new TableView(transMeta, shell, SWT.BORDER
				| SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL,
				ciFields, meta.getFields() == null ? 1
						: meta.getFields().length, lsMod, props);

		FormData fdFields = new FormData();
		fdFields.left = new FormAttachment(0, 0);
		fdFields.top = new FormAttachment(wlFields, margin);
		fdFields.right = new FormAttachment(100, 0);
		fdFields.bottom = new FormAttachment(wOK, -margin);
		wFields.setLayoutData(fdFields);

		setButtonPositions(new Button[] { wOK, wDetectFields, wCancel },
				margin, null);

		// Add listeners
		lsCancel = new Listener() {
			public void handleEvent(Event e) {
				cancel();
			}
		};
		lsDetectFields = new Listener() {
			public void handleEvent(Event e) {
				detectFields();
			}
		};
		lsOK = new Listener() {
			public void handleEvent(Event e) {
				ok();
			}
		};
		wCancel.addListener(SWT.Selection, lsCancel);
		wDetectFields.addListener(SWT.Selection, lsDetectFields);
		wOK.addListener(SWT.Selection, lsOK);

		lsDef = new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				ok();
			}
		};
		wStepname.addSelectionListener(lsDef);
		wInputField.addSelectionListener(lsDef);
		wClasspath.addSelectionListener(lsDef);
		wRootClass.addSelectionListener(lsDef);

		// Listen to the browse button next to the file name
		wbbClasspath.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(shell, SWT.OPEN);
				dialog.setFilterExtensions(new String[] { "*.jar", "*" });
				if (wClasspath.getText() != null) {
					String fname = transMeta.environmentSubstitute(wClasspath
							.getText());
					dialog.setFileName(fname);
				}

				dialog.setFilterNames(new String[] {
						Messages.getString("ProtobufDecodeDialog.FileType.Classpaths"),
						BaseMessages.getString("System.FileType.AllFiles") });

				if (dialog.open() != null) {
					String str = dialog.getFilterPath()
							+ System.getProperty("file.separator")
							+ dialog.getFileName();
					wClasspath.setText(str);
				}
			}
		});

		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(ShellEvent e) {
				cancel();
			}
		});

		// Set the shell size, based upon previous time...
		setSize(shell, 650, 500, true);

		getData(meta, true);
		meta.setChanged(changed);

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return stepname;
	}

	/**
	 * Copy information from the meta-data input to the dialog fields.
	 */
	private void getData(ProtobufDecodeMeta consumerMeta, boolean copyStepname) {
		if (copyStepname) {
			wStepname.setText(stepname);
		}

		wInputField.setText(Const.NVL(consumerMeta.getInputField(), ""));
		StringBuilder cpBuf = new StringBuilder();
		String[] classpath = consumerMeta.getClasspath();
		if (classpath != null) {
			for (int i = 0; i < classpath.length; ++i) {
				if (i > 0) {
					cpBuf.append(File.pathSeparatorChar);
				}
				cpBuf.append(classpath[i]);
			}
		}
		wClasspath.setText(cpBuf.toString());
		wRootClass.setText(Const.NVL(consumerMeta.getRootClass(), ""));

		FieldDefinition[] fields = meta.getFields();
		if (fields != null) {
			for (FieldDefinition field : fields) {
				TableItem item = new TableItem(wFields.table, SWT.NONE);
				int colnr = 1;
				item.setText(colnr++, Const.NVL(field.name, ""));
				item.setText(colnr++, Const.NVL(field.path, ""));
				item.setText(colnr++, ValueMeta.getTypeDesc(field.type));
			}
		}
		wFields.removeEmptyRows();
		wFields.setRowNums();
		wFields.optWidth(true);

		wStepname.selectAll();
	}

	private void cancel() {
		stepname = null;
		meta.setChanged(changed);
		dispose();
	}

	/**
	 * Copy information from the dialog fields to the meta-data input
	 */
	private void setData(ProtobufDecodeMeta consumerMeta) {
		consumerMeta.setInputField(transMeta.environmentSubstitute(wInputField
				.getText()));
		consumerMeta.setClasspath(transMeta.environmentSubstitute(
				wClasspath.getText().trim()).split(File.pathSeparator));
		consumerMeta.setRootClass(wRootClass.getText());

		int nrNonEmptyFields = wFields.nrNonEmpty();
		FieldDefinition[] fields = new FieldDefinition[nrNonEmptyFields];
		for (int i = 0; i < nrNonEmptyFields; i++) {
			TableItem item = wFields.getNonEmpty(i);
			fields[i] = new FieldDefinition();
			int colnr = 1;
			fields[i].name = item.getText(colnr++);
			fields[i].path = item.getText(colnr++);
			fields[i].type = ValueMeta.getType(item.getText(colnr++));
		}
		consumerMeta.setFields(fields);
		wFields.removeEmptyRows();
		wFields.setRowNums();
		wFields.optWidth(true);

		consumerMeta.setChanged();
	}

	private void ok() {
		if (Const.isEmpty(wStepname.getText())) {
			return;
		}
		setData(meta);
		stepname = wStepname.getText();
		dispose();
	}

	private void detectFields() {
		try {
			ProtobufDecoder protobufDecoder = new ProtobufDecoder(transMeta
					.environmentSubstitute(wClasspath.getText().trim()).split(
							File.pathSeparator), wRootClass.getText(), null);
			Map<String, Class<?>> fields = protobufDecoder.guessFields();
			RowMeta rowMeta = new RowMeta();
			for (Entry<String, Class<?>> e : fields.entrySet()) {
				String fieldPath = e.getKey();
				int i = fieldPath.lastIndexOf('.');
				String fieldName = i != -1 ? fieldPath.substring(i + 1)
						: fieldPath;
				rowMeta.addValueMeta(new FieldMeta(fieldName, fieldPath,
						KettleTypesConverter.javaToKettleType(e.getValue())));
			}
			BaseStepDialog.getFieldsFromPrevious(rowMeta, wFields, 1,
					new int[] { 1 }, new int[] { 3 }, -1, -1,
					new TableItemInsertListener() {
						public boolean tableItemInserted(TableItem tableItem,
								ValueMetaInterface v) {
							tableItem.setText(2, ((FieldMeta) v).path);
							return true;
						}
					});
		} catch (ProtobufDecoderException e) {
			new ErrorDialog(
					shell,
					BaseMessages.getString("System.Dialog.Error.Title"),
					Messages.getString("ProtobufDecodeDialog.ErrorDialog.ErrorDetectingFields"),
					e);
		}
	}

	static class FieldMeta extends ValueMeta {
		String path;

		public FieldMeta(String name, String path, int type) {
			super(name, type);
			this.path = path;
		}
	}
}

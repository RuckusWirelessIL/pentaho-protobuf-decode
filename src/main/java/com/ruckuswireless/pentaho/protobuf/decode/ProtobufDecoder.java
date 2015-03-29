package com.ruckuswireless.pentaho.protobuf.decode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.ruckuswireless.pentaho.utils.KettleTypesConverter;

/**
 * Protocol Buffers messages decoder
 * 
 * @author Michael Spector
 */
public class ProtobufDecoder {

	private final Object EMPTY = new Object();
	private URLClassLoader classLoader;
	private Class<?> rootClass;
	private Method rootParseFromMethod;
	private LinkedHashMap<String, Integer> paths;
	private FieldDefinition[] fields;

	public ProtobufDecoder(String[] classpath, String rootClass,
			FieldDefinition[] fields) throws ProtobufDecoderException {

		try {
			URL[] url = new URL[classpath.length];
			for (int i = 0; i < classpath.length; ++i) {
				String file = classpath[i];
				if (file.startsWith("file://")) {
					file = file.substring(7);
				}
				url[i] = new File(file).toURI().toURL();
			}
			this.classLoader = new URLClassLoader(url, getClass()
					.getClassLoader());
		} catch (MalformedURLException e) {
			throw new ProtobufDecoderException(e);
		}

		try {
			this.rootClass = classLoader.loadClass(rootClass);
		} catch (ClassNotFoundException e) {
			throw new ProtobufDecoderException("Can't find root class", e);
		}

		try {
			this.rootParseFromMethod = this.rootClass.getDeclaredMethod(
					"parseFrom", new Class<?>[] { byte[].class });
		} catch (SecurityException e) {
			throw new ProtobufDecoderException(e);
		} catch (NoSuchMethodException e) {
			throw new ProtobufDecoderException(
					"Can't setup Protocol Buffers decoder", e);
		}

		if (fields != null) {
			this.paths = new LinkedHashMap<String, Integer>();
			for (int i = 0; i < fields.length; ++i) {
				this.paths.put(fields[i].path, Integer.valueOf(i));
			}
			this.fields = fields;
		}
	}

	/**
	 * Disposes the decoder
	 * 
	 * @throws ProtobufDecoderException
	 */
	public void dispose() throws ProtobufDecoderException {
		if (classLoader != null) {
			try {
				classLoader.close();
			} catch (IOException e) {
				throw new ProtobufDecoderException(e);
			}
			classLoader = null;
		}
	}

	/**
	 * Decodes message, and returns denormalized rows for the object fields
	 * exactly in the order they appear in <code>fields</code> parameter.
	 * 
	 * @param message
	 *            Encoded Protocol Buffers message
	 * @return rows list
	 * @throws ProtobufDecoderException
	 */
	public List<Object[]> decode(byte[] message)
			throws ProtobufDecoderException {
		Message decodedMessage;
		try {
			decodedMessage = (Message) rootParseFromMethod
					.invoke(null, message);
		} catch (IllegalArgumentException e) {
			throw new ProtobufDecoderException(e);
		} catch (IllegalAccessException e) {
			throw new ProtobufDecoderException(e);
		} catch (InvocationTargetException e) {
			throw new ProtobufDecoderException("Can't call to "
					+ rootParseFromMethod, e.getCause());
		}

		ValueNode root = buildValuesTree(decodedMessage, "");
		LinkedList<Object[]> result = new LinkedList<Object[]>();
		if (root != null) {
			produceRows(root, new LinkedList<Object[]>(), result,
					new HashSet<Integer>());
		}
		return result;
	}

	protected static class ValueNode {
		Integer fieldIdx;
		Object value;
		List<ValueNode> children;
	}

	protected ValueNode buildValuesTree(Object root, String currentPath)
			throws ProtobufDecoderException {

		if (root instanceof Message) {
			Message message = (Message) root;
			List<FieldDescriptor> fields = message.getDescriptorForType()
					.getFields();
			List<ValueNode> ch = new ArrayList<ValueNode>(fields.size());
			for (FieldDescriptor field : fields) {
				ValueNode n = buildValuesTree(
						message.getField(field),
						currentPath.length() > 0 ? currentPath + "."
								+ field.getName() : field.getName());
				if (n != null) {
					ch.add(n);
				}
			}
			if (ch.size() > 0) {
				ValueNode node = new ValueNode();
				node.children = ch;
				return node;
			}
			return null;
		}

		if (root instanceof List<?>) {
			List<?> list = (List<?>) root;
			List<ValueNode> ch = new ArrayList<ValueNode>(list.size());
			for (Object v : list) {
				ValueNode n = buildValuesTree(v, currentPath);
				if (n != null) {
					ch.add(n);
				}
			}
			if (ch.size() > 0) {
				ValueNode node = new ValueNode();
				node.children = ch;
				return node;
			}
			return null;
		}

		// primitive
		Integer fieldIdx = paths.get(currentPath);
		if (fieldIdx != null) {
			ValueNode node = new ValueNode();
			node.fieldIdx = fieldIdx;
			node.value = KettleTypesConverter.kettleCast(root);
			return node;
		}
		return null;
	}

	protected void produceRows(ValueNode root, LinkedList<Object[]> rowsPool,
			LinkedList<Object[]> result, Set<Integer> processedFields) {

		if (root.fieldIdx != null) { // Field value (leaf)
			int fieldIdx = root.fieldIdx.intValue();
			if (rowsPool.size() == 0) {
				// Create new row, and push it to the rows pool
				Object[] row = new Object[fields.length];
				for (int i = 0; i < row.length; ++i) {
					row[i] = EMPTY;
				}
				row[fieldIdx] = root.value;
				rowsPool.add(row);

			} else {
				List<Object[]> newRows = new LinkedList<Object[]>();
				for (Object[] row : rowsPool) {
					if (row[fieldIdx] == EMPTY) {
						row[fieldIdx] = root.value;
					} else {
						// Clone the row:
						Object[] newRow = new Object[row.length];
						System.arraycopy(row, 0, newRow, 0, row.length);
						newRow[fieldIdx] = root.value;
						newRows.add(newRow);
					}
				}
				rowsPool.addAll(newRows);
			}
			processedFields.add(root.fieldIdx);
		} else {
			for (ValueNode child : root.children) {
				produceRows(child, rowsPool, result, processedFields);
			}
			if (processedFields.size() == fields.length) {
				// Move all created rows from the pool to the result
				result.addAll(rowsPool);
				rowsPool.clear();
				processedFields.clear();
			}
		}
	}

	/**
	 * @return detected fields of the object tree
	 */
	public Map<String, Class<?>> guessFields() {
		Map<String, Class<?>> fieldsMap = new HashMap<String, Class<?>>();
		guessFields(fieldsMap, rootClass, "");
		return fieldsMap;
	}

	private void guessFields(Map<String, Class<?>> fieldsMap,
			Class<?> rootClass, String prefix) {
		List<String> fields = new LinkedList<String>();
		for (Method m : rootClass.getDeclaredMethods()) {
			String methodName = m.getName();
			String field = null;
			if (methodName.startsWith("has")) {
				field = methodName.substring(3);
			} else if (methodName.startsWith("get")
					&& methodName.endsWith("List") && methodName.length() > 7) {
				field = methodName.substring(3, methodName.length() - 4);
			}
			if (field != null && !field.endsWith("OrBuilder")) {
				fields.add(field);
			}
		}

		String rootPackage = rootClass.getPackage().getName();
		for (String val : fields) {
			try {
				Method getMethod;
				try {
					getMethod = rootClass.getDeclaredMethod("get" + val,
							new Class<?>[] { int.class });
				} catch (NoSuchMethodException e) {
					getMethod = rootClass.getDeclaredMethod("get" + val,
							new Class<?>[0]);
				}
				String name = val.length() > 1 ? Character.toLowerCase(val
						.charAt(0)) + val.substring(1) : val;
				String path = prefix.length() > 0 ? prefix + "." + name : name;

				Class<?> returnType = getMethod.getReturnType();
				if (returnType.getName().startsWith(rootPackage)) {
					guessFields(fieldsMap, returnType, path);
				} else {
					fieldsMap.put(path, returnType);
				}
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			}
		}
	}

	public static class ProtobufDecoderException extends Exception {
		private static final long serialVersionUID = 1L;

		public ProtobufDecoderException(String message, Throwable cause) {
			super(message, cause);
		}

		public ProtobufDecoderException(String message) {
			super(message);
		}

		public ProtobufDecoderException(Throwable cause) {
			super(cause);
		}
	}

	public static interface RowProduceListener {
		void onNewRow(Object[] row);
	}
}

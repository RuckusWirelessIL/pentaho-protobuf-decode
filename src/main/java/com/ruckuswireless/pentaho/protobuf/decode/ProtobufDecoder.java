package com.ruckuswireless.pentaho.protobuf.decode;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

/**
 * Protocol Buffers messages decoder
 * 
 * @author Michael Spector
 */
public class ProtobufDecoder {

	private Class<?> rootClass;
	private Method rootParseFromMethod;

	public ProtobufDecoder(String[] classpath, String rootClass) throws ProtobufDecoderException {
		URLClassLoader classLoader;
		try {
			URL[] url = new URL[classpath.length];
			for (int i = 0; i < classpath.length; ++i) {
				url[i] = new File(classpath[i]).toURI().toURL();
			}
			classLoader = new URLClassLoader(url, getClass().getClassLoader());
		} catch (MalformedURLException e) {
			throw new ProtobufDecoderException(e);
		}

		try {
			this.rootClass = classLoader.loadClass(rootClass);
		} catch (ClassNotFoundException e) {
			throw new ProtobufDecoderException("Can't find root class", e);
		}

		try {
			this.rootParseFromMethod = this.rootClass.getDeclaredMethod("parseFrom", new Class<?>[] { byte[].class });
		} catch (SecurityException e) {
			throw new ProtobufDecoderException(e);
		} catch (NoSuchMethodException e) {
			throw new ProtobufDecoderException("Can't setup Protocol Buffers decoder", e);
		}
	}

	/**
	 * Parses binary message
	 * 
	 * @param message
	 *            Encoded message
	 * @return decoded Protocol Buffers message
	 * @throws ProtobufDecoderException
	 */
	protected Message decode(byte[] message) throws ProtobufDecoderException {
		try {
			return (Message) rootParseFromMethod.invoke(null, message);
		} catch (IllegalArgumentException e) {
			throw new ProtobufDecoderException(e);
		} catch (IllegalAccessException e) {
			throw new ProtobufDecoderException(e);
		} catch (InvocationTargetException e) {
			throw new ProtobufDecoderException("Can't call to " + rootParseFromMethod, e.getCause());
		}
	}

	/**
	 * Decodes message, and returns denormalized rows for the object fields
	 * exactly in the order they appear in <code>fields</code> parameter.
	 * 
	 * @param message
	 *            Encoded Protocol Buffers message
	 * @param fields
	 *            Definitions of fields to return
	 * @return values
	 * @throws ProtobufDecoderException
	 */
	public Object[] decode(byte[] message, FieldDefinition[] fields) throws ProtobufDecoderException {
		LinkedList<Object[]> result = new LinkedList<Object[]>();
		buildRows(decode(message), fields, result);
		return result.toArray();
	}

	protected void buildRows(Message message, FieldDefinition[] fields, LinkedList<Object[]> result)
			throws ProtobufDecoderException {

		for (int i = 0; i < fields.length; ++i) {
			FieldDefinition field = fields[i];

			List<Object> values = new ArrayList<Object>();
			getFieldValues(message, field.path, values);

			for (Object val : values) {
				Object[] lastRow = result.getLast();
				for (int j = 0; j < values.size() - 1; ++j) {
					Object[] clone = new Object[lastRow.length];
					System.arraycopy(lastRow, 0, clone, 0, lastRow.length);
					result.add(clone);
				}
				for (Object[] res : result) {
					res[i] = val;
				}
			}
		}
	}

	protected void getFieldValues(Object root, String fieldPath, List<Object> values) throws ProtobufDecoderException {
		int i = fieldPath.indexOf('.');
		if (i == -1) {
			i = fieldPath.length();
		}
		String fieldName = fieldPath.substring(0, i);
		fieldPath = fieldPath.substring(i);

		if (root instanceof Message) {
			if (fieldName.length() == 0) {
				throw new ProtobufDecoderException("Field path doesn't lead to a primitive value!");
			}
			Message message = (Message) root;
			FieldDescriptor fieldDesc = message.getDescriptorForType().findFieldByName(fieldName);
			getFieldValues(message.getField(fieldDesc), fieldPath, values);

		} else if (root instanceof List<?>) {
			List<?> valuesList = (List<?>) root;
			for (Object v : valuesList) {
				getFieldValues(v, fieldPath, values);
			}
		} else { // primitive
			if (fieldPath.length() > 0) {
				throw new ProtobufDecoderException("Field path leads through primitive value!");
			}
			values.add(root);
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

	private void guessFields(Map<String, Class<?>> fieldsMap, Class<?> rootClass, String prefix) {
		List<String> fields = new LinkedList<String>();
		for (Method m : rootClass.getDeclaredMethods()) {
			String methodName = m.getName();
			String field = null;
			if (methodName.startsWith("has")) {
				field = methodName.substring(3);
			} else if (methodName.startsWith("get") && methodName.endsWith("List") && methodName.length() > 7) {
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
					getMethod = rootClass.getDeclaredMethod("get" + val, new Class<?>[] { int.class });
				} catch (NoSuchMethodException e) {
					getMethod = rootClass.getDeclaredMethod("get" + val, new Class<?>[0]);
				}
				String name = val.length() > 1 ? Character.toLowerCase(val.charAt(0)) + val.substring(1) : val;
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
}

package com.ruckuswireless.pentaho.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.pentaho.di.core.row.ValueMetaInterface;

/**
 * Simple types converter
 * 
 * @author Michael Spector
 */
public class KettleTypesConverter {

	public static Object kettleCast(Object value) {
		if (value == null) {
			return value;
		}
		Class<?> c = value.getClass();
		if (c == Integer.class || c == int.class) {
			return ((Integer) value).longValue();
		}
		if (c == Float.class || c == float.class) {
			return ((Float) value).doubleValue();
		}
		if (c == BigInteger.class) {
			return new BigDecimal((char[]) value);
		}
		return value;
	}

	public static int javaToKettleType(Class<?> c) {
		if (c == String.class) {
			return ValueMetaInterface.TYPE_STRING;
		}
		if (c == Long.class || c == long.class || c == Integer.class || c == int.class) {
			return ValueMetaInterface.TYPE_INTEGER;
		}
		if (c == Double.class || c == double.class || c == Float.class || c == float.class) {
			return ValueMetaInterface.TYPE_NUMBER;
		}
		if (c == BigDecimal.class || c == BigInteger.class) {
			return ValueMetaInterface.TYPE_BIGNUMBER;
		}
		if (Date.class.isAssignableFrom(c)) {
			return ValueMetaInterface.TYPE_DATE;
		}
		if (c == Boolean.class || c == boolean.class) {
			return ValueMetaInterface.TYPE_BOOLEAN;
		}
		if (c == byte[].class) {
			return ValueMetaInterface.TYPE_BINARY;
		}
		return ValueMetaInterface.TYPE_NONE;
	}

	public static Class<?> kettleToJavaType(int t) {
		switch (t) {
		case ValueMetaInterface.TYPE_STRING:
			return String.class;
		case ValueMetaInterface.TYPE_INTEGER:
			return long.class;
		case ValueMetaInterface.TYPE_NUMBER:
			return double.class;
		case ValueMetaInterface.TYPE_BIGNUMBER:
			return BigDecimal.class;
		case ValueMetaInterface.TYPE_DATE:
			return Date.class;
		case ValueMetaInterface.TYPE_BOOLEAN:
			return boolean.class;
		case ValueMetaInterface.TYPE_BINARY:
			return byte[].class;
		default:
			return null;
		}
	}
}

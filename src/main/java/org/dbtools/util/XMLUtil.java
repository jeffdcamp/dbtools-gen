/*
 * XMLUtil.java
 *
 * Created on March 29, 2004
 *
 * Copyright 2006 Jeff Campbell. All rights reserved. Unauthorized reproduction 
 * is a violation of applicable law. This material contains certain 
 * confidential or proprietary information and trade secrets of Jeff Campbell.
 */

package org.dbtools.util;

import org.dom4j.Attribute;
import org.dom4j.Element;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

/**
 * @author Jeff
 */
public class XMLUtil {

    /**
     * Creates a new instance of XMLUtil
     */
    private XMLUtil() {
    }

    public static String getAttribute(Element element, String attributeName, boolean required) {
        return getAttribute(element, attributeName, required, null);
    }

    public static String getAttribute(Element element, String attributeName, boolean required, String defaultValue) {
        if (element == null) {
            throw new IllegalArgumentException("element cannot be null");
        }

        Attribute attr = element.attribute(attributeName);
        if (attr != null) {
            return attr.getValue();
        } else {
            if (required) {
                throw new IllegalArgumentException("Element Attribute [" + attributeName + "] is a required field!");
            }
            return defaultValue;
        }

    }

    public static boolean getAttributeBoolean(Element element, String attributeName, boolean required, boolean defaultValue) {
        String value = getAttribute(element, attributeName, required, null);

        if (value != null) {
            return Boolean.parseBoolean(value);
        } else {
            return defaultValue;
        }
    }

    public static int getAttributeInt(Element element, String attributeName, boolean required, int defaultValue) {
        String value = getAttribute(element, attributeName, required, null);

        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException nfe) {
                return 0;
            }
        } else {
            return defaultValue;
        }
    }

    public static long getAttributeLong(Element element, String attributeName, boolean required, long defaultValue) {
        String value = getAttribute(element, attributeName, required, null);

        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException nfe) {
                return 0;
            }
        } else {
            return defaultValue;
        }
    }

    public static float getAttributeFloat(Element element, String attributeName, boolean required, float defaultValue) {
        String value = getAttribute(element, attributeName, required, null);

        if (value != null) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException nfe) {
                return 0;
            }
        } else {
            return defaultValue;
        }
    }

    public static double getAttributeDouble(Element element, String attributeName, boolean required, double defaultValue) {
        String value = getAttribute(element, attributeName, required, null);

        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException nfe) {
                return 0;
            }
        } else {
            return defaultValue;
        }
    }

    public static Date getAttributeDate(Element element, String attributeName, boolean required, DateFormat dateformat, Date defaultValue) {
        String value = getAttribute(element, attributeName, required, null);

        if (value != null) {
            try {
                return dateformat.parse(value);
            } catch (ParseException e) {
                return null;
            }
        } else {
            return defaultValue;
        }
    }

//    public static Fraction getAttributeFraction(Element element, String attributeName, boolean required, Fraction defaultValue) {
//        String value = getAttribute(element, attributeName, required, null);
//
//        if (value != null) {
//            try {
//                return Fraction.parseFraction(value);
//            } catch (NumberFormatException nfe) {
//                return null;
//            }
//        } else {
//            return defaultValue;
//        }
//    }
//
//    public static Money getAttributeMoney(Element element, String attributeName, boolean required, Money defaultValue) {
//        String value = getAttribute(element, attributeName, required, null);
//
//        if (value != null) {
//            try {
//                if (defaultValue == null) {
//                    return Money.parseMoney(value,Money.DEFAULT_FORMAT);
//                } else {
//                    return Money.parseMoney(value, defaultValue.getDecimals(), defaultValue.getFormat());
//                }
//            } catch (java.text.ParseException pe) {
//                return null;
//            }
//        } else {
//            return defaultValue;
//        }
//    }

    public static Element getElementChild(Element fromElement, String elementName, String elementAttributeName, String elementAttributeValue) {
        Element foundElement = null;

        if (fromElement == null) {
            throw new IllegalArgumentException("fromElement cannot be null");
        }

        Iterator childElements = fromElement.elementIterator(elementName);
        while (childElements.hasNext()) {
            Element childElement = (Element) childElements.next();
            String attrName = getAttribute(childElement, elementAttributeName, false);
            if (attrName.equals(elementAttributeValue)) {
                foundElement = childElement;
            }
        }

        return foundElement;
    }

    public static String multiLineStringToXmlString(String multiLineString) {
        return multiLineStringToXmlString(multiLineString, "^");
    }

    public static String multiLineStringToXmlString(String multiLineText, String delimiter) {
        return multiLineText.replace("\n", delimiter);
    }

    public static String xmlStringToMultiLineString(String xmlString) {
        return xmlStringToMultiLineString(xmlString, "^");
    }

    public static String xmlStringToMultiLineString(String xmlString, String delimiter) {
        return xmlString.replace(delimiter, "\n");
    }
}

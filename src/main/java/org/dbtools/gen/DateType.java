package org.dbtools.gen;

import org.dbtools.schema.schemafile.SchemaField;

public enum DateType {
    JAVA_DATE(false),
    JODA(true),
    JSR_310(true);

    private boolean mutable;

    DateType(boolean mutable) {
        this.mutable = mutable;
    }

    public boolean isMutable() {
        return mutable;
    }

    public String getValuesValue(SchemaField field, String fieldName) {
        switch (this) {
            default:
            case JAVA_DATE:
                return "getTime()";
            case JODA:
                return "DBToolsDateFormatter.dateTimeToLong(" + fieldName + ")";
            case JSR_310:
                switch (field.getJdbcDataType()) {
                    default:
                    case TIMESTAMP:
                        return "DBToolsDateFormatter.localDateTimeToLong(" + fieldName + ")";
                    case DATETIME:
                        return "DBToolsDateFormatter.localDateTimeToDBString(" + fieldName + ")";
                    case DATE:
                        return "DBToolsDateFormatter.localDateToDBString(" + fieldName + ")";
                    case TIME:
                        return "DBToolsDateFormatter.localTimeToDBString(" + fieldName + ")";
                }
        }
    }

    public boolean isAlternative() {
        return this != JAVA_DATE;
    }

    public String getJavaClassDataType(SchemaField field) {
        String dateDataType;
        if (this == JODA) {
            dateDataType = "org.joda.time.DateTime";
        } else if (this == JSR_310) {
            switch (field.getJdbcDataType()) {
                default:
                case DATETIME:
                case TIMESTAMP:
                    dateDataType = "org.threeten.bp.LocalDateTime";
                    break;
                case DATE:
                    dateDataType = "org.threeten.bp.LocalDate";
                    break;
                case TIME:
                    dateDataType = "org.threeten.bp.LocalTime";
                    break;
            }
        } else {
            dateDataType = "Date";
        }

        return dateDataType;
    }

    public String getJavaClassDataTypeDefaultValue(SchemaField field) {
        String dateDataType;
        if (this == JODA) {
            dateDataType = "org.joda.time.DateTime.now()";
        } else if (this == JSR_310) {
            switch (field.getJdbcDataType()) {
                default:
                case DATETIME:
                case TIMESTAMP:
                    dateDataType = "org.threeten.bp.LocalDateTime.now()";
                    break;
                case DATE:
                    dateDataType = "org.threeten.bp.LocalDate.now()";
                    break;
                case TIME:
                    dateDataType = "org.threeten.bp.LocalTime.now()";
                    break;
            }
        } else {
            dateDataType = "Date()";
        }

        return dateDataType;
    }

    public String getValuesDbStringToObjectMethod(SchemaField field, String paramValue) {
        switch (field.getJdbcDataType()) {
            case DATETIME:
                if (this == JSR_310) {
                    return "DBToolsDateFormatter.dbStringToLocalDateTime(values.getAsString(" + paramValue + "))";
                } else {
                    return "TIME IS CURRENTLY ONLY SUPPORTED BY JSR_310";
                }
            case DATE:
                switch (this) {
                    default:
                    case JAVA_DATE:
                        return "DBToolsDateFormatter.dbStringToDate(values.getAsString(" + paramValue + "))";
                    case JODA:
                        return "DBToolsDateFormatter.dbStringToDateTime(values.getAsString(" + paramValue + "))";
                    case JSR_310:
                        return "DBToolsDateFormatter.dbStringToLocalDate(values.getAsString(" + paramValue + "))";
                }

            case TIME:
                if (this == JSR_310) {
                    return "DBToolsDateFormatter.dbStringToLocalTime(values.getAsString(" + paramValue + "))";
                } else {
                    return "TIME IS CURRENTLY ONLY SUPPORTED BY JSR_310";
                }

            default:
            case TIMESTAMP:
                switch (this) {
                    default:
                    case JAVA_DATE:
                        return "new java.util.Date(values.getAsLong(" + paramValue + "))";
                    case JODA:
                        return "DBToolsDateFormatter.longToDateTime(values.getAsLong(" + paramValue + "))";
                    case JSR_310:
                        return "DBToolsDateFormatter.longToLocalDateTime(values.getAsLong(" + paramValue + "))";
                }
        }
    }

    public String getCursorDbStringToObjectMethod(SchemaField field, String paramValue, boolean kotlin) {
        switch (field.getJdbcDataType()) {
            case DATETIME:
                if (this == JSR_310) {
                    if (kotlin && field.isNotNull()) {
                        return "DBToolsDateFormatter.dbStringToLocalDateTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                    } else {
                        return "DBToolsDateFormatter.dbStringToLocalDateTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                    }
                } else {
                    return "TIME IS CURRENTLY ONLY SUPPORTED BY JSR_310";
                }
            case DATE:
                switch (this) {
                    default:
                    case JAVA_DATE:
                        if (kotlin && field.isNotNull()) {
                            return "DBToolsDateFormatter.dbStringToDate(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                        } else {
                            return "DBToolsDateFormatter.dbStringToDate(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                        }

                    case JODA:
                        if (kotlin && field.isNotNull()) {
                            return "DBToolsDateFormatter.dbStringToDateTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                        } else {
                            return "DBToolsDateFormatter.dbStringToDateTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                        }

                    case JSR_310:
                        if (kotlin && field.isNotNull()) {
                            return "DBToolsDateFormatter.dbStringToLocalDate(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                        } else {
                            return "DBToolsDateFormatter.dbStringToLocalDate(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                        }

                }
            case TIME:
                if (this == JSR_310) {
                    if (kotlin && field.isNotNull()) {
                        return "DBToolsDateFormatter.dbStringToLocalTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                    } else {
                        return "DBToolsDateFormatter.dbStringToLocalTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                    }
                } else {
                    return "TIME IS CURRENTLY ONLY SUPPORTED BY JSR_310";
                }

            default:
            case TIMESTAMP:
                switch (this) {
                    default:
                    case JAVA_DATE:
                        if (kotlin) {
                            if (field.isNotNull()) {
                                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + "))) java.util.Date(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + ")))!! else null!!";
                            } else {
                                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + "))) java.util.Date(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) else null";
                            }
                        } else {
                            return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? new java.util.Date(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) : null";
                        }
                    case JODA:
                        if (kotlin) {
                            if (field.isNotNull()) {
                                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + "))) DBToolsDateFormatter.longToDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + ")))!! else null!!";
                            } else {
                                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + "))) DBToolsDateFormatter.longToDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) else null";
                            }
                        } else {
                            return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? DBToolsDateFormatter.longToDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) : null";
                        }
                    case JSR_310:
                        if (kotlin) {
                            if (field.isNotNull()) {
                                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + "))) DBToolsDateFormatter.longToLocalDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + ")))!! else null!!";
                            } else {
                                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + "))) DBToolsDateFormatter.longToLocalDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) else null";
                            }
                        } else {
                            return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? DBToolsDateFormatter.longToLocalDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) : null";
                        }
                }
        }
    }
}
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
                switch (field.getJdbcDataType()) {
                    default:
                    case TIMESTAMP:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dateToLong(" + fieldName + ")";
                    case DATETIME:
                    case DATE:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dateToDBString(" + fieldName + ")";
                    case TIME:
                        return "TIME IS CURRENTLY ONLY SUPPORTED BY JSR_310";
                }
            case JODA:
                switch (field.getJdbcDataType()) {
                    default:
                    case TIMESTAMP:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dateTimeToLong(" + fieldName + ")";
                    case DATETIME:
                    case DATE:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dateTimeToDBString(" + fieldName + ")";
                    case TIME:
                        return "TIME IS CURRENTLY ONLY SUPPORTED BY JSR_310";
                }
            case JSR_310:
                switch (field.getJdbcDataType()) {
                    default:
                    case TIMESTAMP:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.localDateTimeToLong(" + fieldName + ")";
                    case DATETIME:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.localDateTimeToDBString(" + fieldName + ")";
                    case DATE:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.localDateToDBString(" + fieldName + ")";
                    case TIME:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.localTimeToDBString(" + fieldName + ")";
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
                switch (this) {
                    default:
                    case JAVA_DATE:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDate(values.getAsString(" + paramValue + "))";
                    case JODA:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDateTime(values.getAsString(" + paramValue + "))";
                    case JSR_310:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToLocalDateTime(values.getAsString(" + paramValue + "))";
                }
            case DATE:
                switch (this) {
                    default:
                    case JAVA_DATE:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDate(values.getAsString(" + paramValue + "))";
                    case JODA:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDateTime(values.getAsString(" + paramValue + "))";
                    case JSR_310:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToLocalDate(values.getAsString(" + paramValue + "))";
                }
            case TIME:
                if (this == JSR_310) {
                    return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToLocalTime(values.getAsString(" + paramValue + "))";
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
                        return "org.dbtools.android.domain.DBToolsDateFormatter.longToDateTime(values.getAsLong(" + paramValue + "))";
                    case JSR_310:
                        return "org.dbtools.android.domain.DBToolsDateFormatter.longToLocalDateTime(values.getAsLong(" + paramValue + "))";
                }
        }
    }

    public String getCursorDbStringToObjectMethod(SchemaField field, String paramValue, boolean kotlin) {
        switch (field.getJdbcDataType()) {
            case DATETIME:
                switch (this) {
                    default:
                    case JAVA_DATE:
                        if (kotlin && field.isNotNull()) {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDate(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                        } else {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDate(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                        }

                    case JODA:
                        if (kotlin && field.isNotNull()) {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDateTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                        } else {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDateTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                        }

                    case JSR_310:
                        if (kotlin && field.isNotNull()) {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToLocalDateTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                        } else {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToLocalDateTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                        }
                }
            case DATE:
                switch (this) {
                    default:
                    case JAVA_DATE:
                        if (kotlin && field.isNotNull()) {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDate(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                        } else {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDate(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                        }

                    case JODA:
                        if (kotlin && field.isNotNull()) {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDateTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                        } else {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToDateTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                        }

                    case JSR_310:
                        if (kotlin && field.isNotNull()) {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToLocalDate(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                        } else {
                            return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToLocalDate(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
                        }
                }
            case TIME:
                if (this == JSR_310) {
                    if (kotlin && field.isNotNull()) {
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToLocalTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))!!";
                    } else {
                        return "org.dbtools.android.domain.DBToolsDateFormatter.dbStringToLocalTime(cursor.getString(cursor.getColumnIndexOrThrow(" + paramValue + ")))";
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
                                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + "))) org.dbtools.android.domain.DBToolsDateFormatter.longToDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + ")))!! else null!!";
                            } else {
                                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + "))) org.dbtools.android.domain.DBToolsDateFormatter.longToDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) else null";
                            }
                        } else {
                            return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? org.dbtools.android.domain.DBToolsDateFormatter.longToDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) : null";
                        }
                    case JSR_310:
                        if (kotlin) {
                            if (field.isNotNull()) {
                                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + "))) org.dbtools.android.domain.DBToolsDateFormatter.longToLocalDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + ")))!! else null!!";
                            } else {
                                return "if (!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + "))) org.dbtools.android.domain.DBToolsDateFormatter.longToLocalDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) else null";
                            }
                        } else {
                            return "!cursor.isNull(cursor.getColumnIndexOrThrow(" + paramValue + ")) ? org.dbtools.android.domain.DBToolsDateFormatter.longToLocalDateTime(cursor.getLong(cursor.getColumnIndexOrThrow(" + paramValue + "))) : null";
                        }
                }
        }
    }

    public String getCopy(String fieldName) {
        switch (this) {
            default:
            case JAVA_DATE:
                return fieldName + " != null ? new java.util.Date(" + fieldName + ".getTime()) : null ";
            case JODA:
            case JSR_310:
                // immutable
                return fieldName;
        }
    }
}
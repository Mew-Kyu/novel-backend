package com.graduate.novel.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;

public class PostgreSQLVectorDialect extends PostgreSQLDialect {

    @Override
    public int getDefaultDecimalPrecision() {
        return 19;
    }
}


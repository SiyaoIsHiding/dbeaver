/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.postgresql.model.impls;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreServerCockroachDB
 */
public class PostgreServerCockroachDB extends PostgreServerExtensionBase {

    public PostgreServerCockroachDB(PostgreDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String getServerTypeName() {
        return "CockroachDB";
    }

    @Override
    public boolean supportsOids() {
        return true;
    }

    @Override
    public boolean supportsIndexes() {
        return true;
    }

    @Override
    public boolean supportsMaterializedViews() {
        return false;
    }

    @Override
    public boolean supportsPartitions() {
        return false;
    }

    @Override
    public boolean supportsInheritance() {
        return false;
    }

    @Override
    public boolean supportsTriggers() {
        return true;
    }

    @Override
    public boolean supportsRules() {
        return false;
    }

    @Override
    public boolean supportsExtensions() {
        return false;
    }

    @Override
    public boolean supportsEncodings() {
        return false;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    @Override
    public boolean supportsSequences() {
        return false;
    }

    @Override
    public boolean supportsRoles() {
        return true;
    }

    @Override
    public boolean supportsSessionActivity() {
        return false;
    }

    @Override
    public boolean supportsLocks() {
        return false;
    }

    @Override
    public boolean supportsForeignServers() {
        return false;
    }

    @Override
    public boolean isSupportsLimits() {
        return false;
    }

    @Override
    public boolean supportsRelationSizeCalc() {
        return false;
    }

    @Override
    public boolean supportFunctionDefRead() {
        return false;
    }

    @Override
    public List<PostgrePermission> readObjectPermissions(DBRProgressMonitor monitor, PostgreTableBase table, boolean includeNestedObjects) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Load CockroachDB table grants")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW GRANTS ON " + table.getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    List<PostgrePermission> permissions = new ArrayList<>();
                    Map<String, List<PostgrePrivilege>> privilegeMap = new HashMap<>();
                    while (resultSet.next()) {
                        String databaseName = JDBCUtils.safeGetString(resultSet, "database_name");
                        String schemaName = JDBCUtils.safeGetString(resultSet, "schema_name");
                        String tableName = JDBCUtils.safeGetString(resultSet, "table_name");
                        String grantee = JDBCUtils.safeGetString(resultSet, "grantee");
                        String privilege = JDBCUtils.safeGetString(resultSet, "privilege_type");
                        List<PostgrePrivilege> privList = privilegeMap.computeIfAbsent(grantee, k -> new ArrayList<>());
                        PostgrePrivilegeType privType = CommonUtils.valueOf(PostgrePrivilegeType.class, privilege, PostgrePrivilegeType.UNKNOWN);
                        privList.add(new PostgrePrivilege("", grantee, databaseName, schemaName, tableName, privType, true, true));
                    }
                    for (Map.Entry<String, List<PostgrePrivilege>> entry : privilegeMap.entrySet()) {
                        PostgrePermission permission = new PostgreObjectPermission(table, entry.getKey(), entry.getValue());
                        permissions.add(permission);
                    }
                    return permissions;
                }
            }
        } catch (Exception e) {
            throw new DBException(e, table.getDataSource());
        }
    }

    @Override
    public String readTableDDL(DBRProgressMonitor monitor, PostgreTableBase table) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Load CockroachDB table DDL")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW CREATE TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (resultSet.next()) {
                        String line = JDBCUtils.safeGetString(resultSet, "CreateTable");
                        if (line == null) {
                            line = JDBCUtils.safeGetString(resultSet, 2);
                        }
                        if (line == null) {
                            continue;
                        }
                        sql.append(line);
                    }
                    return sql.toString();
                }
            }
        } catch (Exception e) {
            throw new DBException(e, table.getDataSource());
        }
    }
}


package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reconciles only the pristine schema created by V1. The already-adopted
 * entity-era schema is fingerprinted and accepted without application DDL.
 */
public class V2__adopt_entity_schema extends BaseJavaMigration {
    private static final String V1_CHECKSUM = "1270414328";
    private static final String ENTITY_FINGERPRINT =
            "1a1e6743b99178ae0e7f069aec2ebc6ca0415df55329d26b3823be5d6cd2e45e";
    private static final List<String> TABLES = List.of(
            "audit_logs", "departments", "employees", "leave_requests", "users");

    @Override
    public Integer getChecksum() {
        // Java migrations do not have Flyway's SQL-file checksum. Keep this
        // stable for V2; any post-application change must be a new V3.
        return 1367554321;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        requireApplicationTables(connection);
        HistoryRow row = readVersionOneHistory(connection);

        String fingerprint = entityFingerprint(connection);
        if ("BASELINE".equals(row.type)) {
            if (!ENTITY_FINGERPRINT.equals(fingerprint)) {
                throw new IllegalStateException("V2 refused: baseline-at-1 schema fingerprint is unknown or ambiguous.");
            }
            validateEntityEraValues(connection);
            return;
        }

        if (!"SQL".equals(row.type) || !V1_CHECKSUM.equals(row.checksum)) {
            throw new IllegalStateException("V2 refused: version 1 history is not the approved baseline or unchanged V1 migration.");
        }

        validateV1Shape(connection);
        requireEmptyApplicationTables(connection);
        applyFreshSchemaChanges(connection);
        validateFreshResult(connection);
    }

    private static void requireApplicationTables(Connection c) throws Exception {
        Set<String> found = new TreeSet<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' AND table_name IN (?,?,?,?,?)")) {
            for (int i = 0; i < TABLES.size(); i++) ps.setString(i + 1, TABLES.get(i));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) found.add(rs.getString(1)); }
        }
        if (!found.equals(new TreeSet<>(TABLES))) {
            throw new IllegalStateException("V2 refused: expected exactly the five EMS application tables.");
        }
    }

    private static HistoryRow readVersionOneHistory(Connection c) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT type, checksum FROM flyway_schema_history_v2 WHERE version='1' AND success=1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("V2 refused: version 1 is absent from flyway_schema_history_v2.");
                HistoryRow row = new HistoryRow(rs.getString(1), rs.getString(2));
                if (rs.next()) throw new IllegalStateException("V2 refused: version 1 history is ambiguous.");
                return row;
            }
        }
    }

    private static String entityFingerprint(Connection c) throws Exception {
        String sql = "SET SESSION group_concat_max_len=1000000";
        try (Statement s = c.createStatement()) { s.execute(sql); }
        sql = "SELECT SHA2(GROUP_CONCAT(line ORDER BY line SEPARATOR '\\n'),256) FROM (" +
                "SELECT CONCAT('T|',TABLE_NAME,'|',ENGINE,'|',TABLE_COLLATION) line FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('audit_logs','departments','employees','leave_requests','users') " +
                "UNION ALL SELECT CONCAT('C|',TABLE_NAME,'|',COLUMN_NAME,'|',COLUMN_TYPE,'|',IS_NULLABLE,'|',IFNULL(CAST(COLUMN_DEFAULT AS CHAR),'<NULL>'),'|',EXTRA) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('audit_logs','departments','employees','leave_requests','users') " +
                "UNION ALL SELECT CONCAT('I|',TABLE_NAME,'|',INDEX_NAME,'|',NON_UNIQUE,'|',SEQ_IN_INDEX,'|',COLUMN_NAME,'|',INDEX_TYPE) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('audit_logs','departments','employees','leave_requests','users') " +
                "UNION ALL SELECT CONCAT('F|',k.TABLE_NAME,'|',k.CONSTRAINT_NAME,'|',k.COLUMN_NAME,'|',k.REFERENCED_TABLE_NAME,'|',k.REFERENCED_COLUMN_NAME,'|',r.UPDATE_RULE,'|',r.DELETE_RULE) FROM information_schema.KEY_COLUMN_USAGE k JOIN information_schema.REFERENTIAL_CONSTRAINTS r ON r.CONSTRAINT_SCHEMA=k.CONSTRAINT_SCHEMA AND r.TABLE_NAME=k.TABLE_NAME AND r.CONSTRAINT_NAME=k.CONSTRAINT_NAME WHERE k.CONSTRAINT_SCHEMA=DATABASE() AND k.REFERENCED_TABLE_NAME IS NOT NULL AND k.TABLE_NAME IN ('audit_logs','departments','employees','leave_requests','users') " +
                "UNION ALL SELECT CONCAT('K|',tc.TABLE_NAME,'|',tc.CONSTRAINT_NAME,'|',tc.CONSTRAINT_TYPE,'|',IFNULL(cc.CHECK_CLAUSE,'')) FROM information_schema.TABLE_CONSTRAINTS tc LEFT JOIN information_schema.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME WHERE tc.CONSTRAINT_SCHEMA=DATABASE() AND tc.TABLE_NAME IN ('audit_logs','departments','employees','leave_requests','users') AND tc.CONSTRAINT_TYPE='CHECK') schema_lines";
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (!rs.next()) throw new IllegalStateException("V2 refused: could not calculate schema fingerprint.");
            return rs.getString(1);
        }
    }

    private static void validateV1Shape(Connection c) throws Exception {
        if (scalar(c, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('audit_logs','departments','employees','leave_requests','users') AND (engine<>'InnoDB' OR table_collation<>'utf8mb4_unicode_ci')") != 0)
            throw new IllegalStateException("V2 refused: V1 table engine/collation fingerprint is unknown.");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("departments", "id,name,description");
        expected.put("employees", "id,first_name,last_name,email,phone,date_of_birth,gender,department_id,department,designation,salary,joining_date,address,status,termination_date,termination_reason,created_at,updated_at");
        expected.put("users", "id,username,password,role,employee_id,created_at");
        expected.put("leave_requests", "id,employee_id,type,start_date,end_date,reason,status,applied_at");
        expected.put("audit_logs", "id,action,entity_name,entity_id,performed_by,timestamp,details");
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            List<String> actual = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? ORDER BY ordinal_position")) {
                ps.setString(1, entry.getKey());
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) actual.add(rs.getString(1)); }
            }
            if (!String.join(",", actual).equals(entry.getValue())) {
                throw new IllegalStateException("V2 refused: V1 column fingerprint is unknown or partially migrated for " + entry.getKey() + ".");
            }
        }
        requireColumn(c, "departments", "description", "varchar(255)", "YES");
        requireColumn(c, "users", "created_at", "datetime", "YES");
        requireColumn(c, "leave_requests", "type", "varchar(30)", "NO");
        requireColumn(c, "leave_requests", "reason", "varchar(500)", "YES");
        requireColumn(c, "audit_logs", "performed_by", "varchar(50)", "YES");
        requireColumn(c, "audit_logs", "entity_name", "varchar(50)", "NO");
        requireColumn(c, "employees", "salary", "decimal(12,2)", "NO");
        requireColumn(c, "users", "role", "varchar(20)", "NO");
        requireV1ColumnTypes(c);
        requireForeignKeys(c, Set.of("fk_employee_department:employees.department_id>departments.id:NO ACTION:SET NULL",
                "fk_user_employee:users.employee_id>employees.id:NO ACTION:SET NULL",
                "fk_leave_employee:leave_requests.employee_id>employees.id:NO ACTION:CASCADE"));
        requireIndexes(c, Set.of(
                "departments.PRIMARY:0=id", "departments.uk_departments_name:0=name",
                "employees.PRIMARY:0=id", "employees.uk_employee_email:0=email", "employees.idx_employee_dept:1=department",
                "employees.idx_employee_dept_id:1=department_id", "employees.idx_employee_status:1=status", "employees.idx_employee_name:1=first_name,last_name",
                "users.PRIMARY:0=id", "users.uk_users_username:0=username", "users.idx_users_username:1=username",
                // InnoDB creates this exact supporting index for fk_user_employee.
                // Exact-set comparison still rejects any unrecognized extra index.
                "users.fk_user_employee:1=employee_id",
                "leave_requests.PRIMARY:0=id", "leave_requests.idx_leave_employee:1=employee_id", "leave_requests.idx_leave_status:1=status",
                "audit_logs.PRIMARY:0=id", "audit_logs.idx_audit_timestamp:1=timestamp", "audit_logs.idx_audit_entity:1=entity_name,entity_id"));
        Set<String> checks = new TreeSet<>();
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                "SELECT tc.constraint_name,cc.check_clause FROM information_schema.table_constraints tc JOIN information_schema.check_constraints cc ON cc.constraint_schema=tc.constraint_schema AND cc.constraint_name=tc.constraint_name WHERE tc.constraint_schema=DATABASE() AND tc.constraint_type='CHECK' AND tc.table_name IN ('departments','employees','users','leave_requests','audit_logs')")) {
            while (rs.next()) checks.add(rs.getString(1) + ":" + rs.getString(2).replace("`", "").replace(" ", "").toLowerCase());
        }
        if (checks.size() != 1 || !checks.iterator().next().startsWith("chk_salary:") || !checks.iterator().next().endsWith("salary>0)")) {
            throw new IllegalStateException("V2 refused: V1 check-constraint fingerprint is unknown.");
        }
        requireColumnDefault(c, "employees", "status", "Active");
        requireColumnDefault(c, "employees", "created_at", "CURRENT_TIMESTAMP");
        requireColumnDefault(c, "employees", "updated_at", "CURRENT_TIMESTAMP");
        requireColumnDefault(c, "leave_requests", "status", "PENDING");
        requireColumnDefault(c, "leave_requests", "applied_at", "CURRENT_TIMESTAMP");
        requireColumnDefault(c, "users", "created_at", "CURRENT_TIMESTAMP");
        requireColumnDefault(c, "audit_logs", "timestamp", "CURRENT_TIMESTAMP");
        requireDefaultCount(c, "departments", 0);
        requireDefaultCount(c, "employees", 3);
        requireDefaultCount(c, "users", 1);
        requireDefaultCount(c, "leave_requests", 2);
        requireDefaultCount(c, "audit_logs", 1);
        requireAutoIncrement(c, "departments");
        requireAutoIncrement(c, "employees");
        requireAutoIncrement(c, "users");
        requireAutoIncrement(c, "leave_requests");
        requireAutoIncrement(c, "audit_logs");
        if (scalar(c, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='employees' AND column_name='updated_at' AND extra LIKE '%on update CURRENT_TIMESTAMP%'") != 1)
            throw new IllegalStateException("V2 refused: V1 employees.updated_at update behavior is unexpected.");
    }

    private static void requireColumn(Connection c, String table, String column, String type, String nullable) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT column_type,is_nullable FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? AND column_name=?")) {
            ps.setString(1, table); ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !type.equalsIgnoreCase(rs.getString(1)) || !nullable.equals(rs.getString(2)) || rs.next())
                    throw new IllegalStateException("V2 refused: unexpected definition for " + table + "." + column + ".");
            }
        }
    }

    private static void requireColumnDefault(Connection c, String table, String column, String value) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT column_default FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? AND column_name=?")) {
            ps.setString(1, table); ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !value.equals(rs.getString(1))) throw new IllegalStateException("V2 refused: unexpected default for " + table + "." + column + ".");
            }
        }
    }

    private static void requireDefaultCount(Connection c, String table, long expected) throws Exception {
        long actual = scalar(c, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='" + table + "' AND column_default IS NOT NULL");
        if (actual != expected) throw new IllegalStateException("V2 refused: unexpected column-default fingerprint for " + table + ".");
    }

    private static void requireAutoIncrement(Connection c, String table) throws Exception {
        if (scalar(c, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='" + table + "' AND column_name='id' AND extra LIKE '%auto_increment%'") != 1)
            throw new IllegalStateException("V2 refused: unexpected generated-key definition for " + table + ".");
    }

    private static void requireIndexes(Connection c, Set<String> expected) throws Exception {
        Set<String> actual = new TreeSet<>();
        String sql = "SELECT table_name,index_name,non_unique,GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name IN ('audit_logs','departments','employees','leave_requests','users') GROUP BY table_name,index_name,non_unique";
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) actual.add(rs.getString(1) + "." + rs.getString(2) + ":" + rs.getString(3) + "=" + rs.getString(4));
        }
        if (!actual.equals(new TreeSet<>(expected))) throw new IllegalStateException("V2 refused: V1 index fingerprint is unknown or ambiguous.");
    }

    private static void requireForeignKeys(Connection c, Set<String> expected) throws Exception {
        Set<String> actual = new TreeSet<>();
        String sql = "SELECT k.constraint_name,k.table_name,k.column_name,k.referenced_table_name,k.referenced_column_name,r.update_rule,r.delete_rule FROM information_schema.key_column_usage k JOIN information_schema.referential_constraints r ON r.constraint_schema=k.constraint_schema AND r.table_name=k.table_name AND r.constraint_name=k.constraint_name WHERE k.constraint_schema=DATABASE() AND k.referenced_table_name IS NOT NULL";
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) actual.add(rs.getString(1) + ":" + rs.getString(2) + "." + rs.getString(3) + ">" + rs.getString(4) + "." + rs.getString(5) + ":" + rs.getString(6) + ":" + rs.getString(7));
        }
        if (!actual.equals(new TreeSet<>(expected))) throw new IllegalStateException("V2 refused: V1 foreign-key fingerprint is unknown or ambiguous.");
    }

    private static void requireV1ColumnTypes(Connection c) throws Exception {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("departments", "id|bigint|NO\nname|varchar(100)|NO\ndescription|varchar(255)|YES");
        types.put("employees", "id|bigint|NO\nfirst_name|varchar(50)|NO\nlast_name|varchar(50)|NO\nemail|varchar(100)|NO\nphone|varchar(20)|YES\ndate_of_birth|date|YES\ngender|varchar(20)|YES\ndepartment_id|bigint|YES\ndepartment|varchar(50)|YES\ndesignation|varchar(100)|YES\nsalary|decimal(12,2)|NO\njoining_date|date|NO\naddress|varchar(500)|YES\nstatus|varchar(20)|NO\ntermination_date|date|YES\ntermination_reason|varchar(500)|YES\ncreated_at|datetime|YES\nupdated_at|datetime|YES");
        types.put("users", "id|bigint|NO\nusername|varchar(50)|NO\npassword|varchar(255)|NO\nrole|varchar(20)|NO\nemployee_id|bigint|YES\ncreated_at|datetime|YES");
        types.put("leave_requests", "id|bigint|NO\nemployee_id|bigint|NO\ntype|varchar(30)|NO\nstart_date|date|NO\nend_date|date|NO\nreason|varchar(500)|YES\nstatus|varchar(20)|NO\napplied_at|datetime|YES");
        types.put("audit_logs", "id|bigint|NO\naction|varchar(50)|NO\nentity_name|varchar(50)|NO\nentity_id|bigint|YES\nperformed_by|varchar(50)|YES\ntimestamp|datetime|YES\ndetails|varchar(1000)|YES");
        for (Map.Entry<String, String> entry : types.entrySet()) {
            Set<String> expected = new TreeSet<>(Arrays.asList(entry.getValue().split("\\n")));
            Set<String> actual = new TreeSet<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT column_name,column_type,is_nullable FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=?")) {
                ps.setString(1, entry.getKey());
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) actual.add(rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getString(3)); }
            }
            if (!actual.equals(expected)) throw new IllegalStateException("V2 refused: V1 column/type/nullability fingerprint is unknown for " + entry.getKey() + ".");
        }
    }

    private static void requireEmptyApplicationTables(Connection c) throws Exception {
        for (String table : TABLES) {
            if (scalar(c, "SELECT COUNT(*) FROM `" + table + "`") != 0)
                throw new IllegalStateException("V2 refused: strict empty-fresh policy requires zero rows in " + table + ".");
        }
    }

    private static void validateEntityEraValues(Connection c) throws Exception {
        if (scalar(c, "SELECT COUNT(*) FROM employees WHERE UPPER(TRIM(status)) NOT IN ('ACTIVE','INACTIVE','TERMINATED')") != 0)
            throw new IllegalStateException("V2 refused: employees.status contains an unsupported value.");
    }

    private static void applyFreshSchemaChanges(Connection c) throws Exception {
        String[] ddl = {
                "ALTER TABLE departments MODIFY description VARCHAR(500) NULL, ADD created_at DATETIME(6) NULL, ADD updated_at DATETIME(6) NULL",
                "ALTER TABLE employees MODIFY created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6), MODIFY updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)",
                "ALTER TABLE users MODIFY created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6), ADD email VARCHAR(100) NOT NULL, ADD enabled BIT(1) NOT NULL, ADD updated_at DATETIME(6) NULL, ADD CONSTRAINT uk_user_email UNIQUE (email)",
                "ALTER TABLE leave_requests CHANGE COLUMN type leave_type VARCHAR(20) NOT NULL, MODIFY reason VARCHAR(500) NOT NULL, MODIFY applied_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6), ADD review_comments VARCHAR(500) NULL, ADD reviewed_by VARCHAR(100) NULL, ADD reviewed_at DATETIME(6) NULL, ADD created_at DATETIME(6) NULL, ADD updated_at DATETIME(6) NULL",
                "ALTER TABLE audit_logs CHANGE COLUMN performed_by actor VARCHAR(100) NOT NULL, CHANGE COLUMN entity_name entity_type VARCHAR(50) NOT NULL, MODIFY timestamp DATETIME(6) NOT NULL",
                "CREATE INDEX idx_audit_actor ON audit_logs(actor)"
        };
        try (Statement s = c.createStatement()) {
            for (String statement : ddl) s.execute(statement);
        }
    }

    private static void validateFreshResult(Connection c) throws Exception {
        requireColumn(c, "users", "email", "varchar(100)", "NO");
        requireColumn(c, "users", "enabled", "bit(1)", "NO");
        requireColumn(c, "leave_requests", "leave_type", "varchar(20)", "NO");
        requireColumn(c, "audit_logs", "actor", "varchar(100)", "NO");
        requireColumn(c, "audit_logs", "entity_type", "varchar(50)", "NO");
        requireColumn(c, "departments", "description", "varchar(500)", "YES");
        for (String table : TABLES) {
            if (scalar(c, "SELECT COUNT(*) FROM `" + table + "`") != 0)
                throw new IllegalStateException("V2 postcondition failed: empty fresh tables changed unexpectedly.");
        }
        if (scalar(c, "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='audit_logs' AND index_name='idx_audit_actor'") != 1)
            throw new IllegalStateException("V2 postcondition failed: audit actor index is missing.");
    }

    private static long scalar(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (!rs.next()) throw new IllegalStateException("V2 metadata query returned no result.");
            return rs.getLong(1);
        }
    }

    private record HistoryRow(String type, String checksum) { }
}

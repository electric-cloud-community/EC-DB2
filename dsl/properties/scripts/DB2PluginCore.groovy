import groovy.sql.Sql
import groovy.json.JsonOutput;

public class DB2PluginCore {
    def connectionLine;
    def userName;
    def password;
    def db2InstanceAddress;
    def db2InstancePort;
    def databaseName;
    def driverLocation;
    def driverClass;
    def driverPath;
    def licensePath;

    public init() {
        if (!this.userName) {
            throw new PluginException("userName parameter is mandatory");
        }
        if (!this.password) {
            throw new PluginException("password parameter is mandatory");
        }
        if (!this.db2InstanceAddress) {
            throw new PluginException("db2InstanceAddress parameter is mandatory");
        }
        if (!this.db2InstancePort) {
            throw new PluginException("db2InstancePort parameter is mandatory");
        }
        if (!this.databaseName) {
            throw new PluginException("databaseName parameter is mandatory");
        }
        if (!this.driverClass) {
            this.driverClass = "com.ibm.db2.jcc.DB2Driver";
        }
        this.connectionLine = getConnectionLine(this.db2InstanceAddress, this.db2InstancePort, this.databaseName);
        println "Connection line: " + this.connectionLine;
        return true;
    }
    public static getConnectionLine(address, port, databaseName) {
        def connectionLine = "jdbc:db2://$address:$port/$databaseName"
        return connectionLine
    }

    public getSqlConnection() {
        def conn = Sql.newInstance(
            this.connectionLine, this.userName, this.password, driverClass
        )
        return conn
    }

    public parseSQL(sql) {
        Scanner s = new Scanner(sql);
        s.useDelimiter("(;(\r)?\n)|(--\n)");
        def statements = [];
        while (s.hasNext())
        {
            String line = s.next();
            if (line.startsWith("/*!") && line.endsWith("*/")) {
                int i = line.indexOf(' ');
                line = line.substring(i + 1, line.length() - " */".length());
            }
            if (line.trim().length() > 0) {
                def row = line.trim();
                row = row.replaceFirst(/;$/, "")
                statements.add(row)
            }
        }
        return statements
    }

    public executeSQLBatch(def batch) {
        def conn = this.getSqlConnection();

        def statements = this.parseSQL(batch);
        def results = [];
        def counter = 0;
        statements.each {
            statement ->
                counter += 1
                def result
                if (statement =~ /^(?i)SELECT/) {
                    result = executeSQLSelectWithConn(conn, statement, 'json');
                    println("Select result: " + result);
                }
                else {
                    result = this.executeSQL(conn, statement);
                    println("Result: " + result);
                }
                def row = [:];
                row.query = statement;
                row.result = result;
                row.order = counter;
                results.add(row);
        }
        return results;
    }
    public executeSQLUniversal(def conn, def query, def returnType) {
        def retval;
        conn.execute query, { isResultSet, result ->
            if (isResultSet) {
                if (returnType == 'json') {
                    retval = JsonOutput.toJson(result);
                }
                else {
                    retval = result
                }
            }
            else {
                if (returnType == 'json') {
                    retval = JsonOutput.toJson([affectedRows:result]);
                }
                else {
                    retval = result
                }
            }
        }
        return retval
    }
    public executeSQLUniversal(def query, def returnType) {
        def conn = this.getSqlConnection();
        return this.executeSQLUniversal(conn, query, returnType);
    }
    public executeSQL(def query) {
        def conn = this.getSqlConnection();
        return this.executeSQL(conn, query);
    }
    public executeSQL(def conn, def query) {
        def result = conn.execute(query);
        return result;
    }
    public executeSQLSelectWithConn(def conn, def query, def returnType) {
        def result = conn.rows(query);
        if (returnType == 'map') {
            return result;
        }
        else if (returnType == 'json') {
            def json = JsonOutput.toJson(result)
            return json;
        }
        else {
            throw new PluginException("Unknown return type");
        }
    }
    public executeSQLSelect(query, returnType) {
        return executeSQLSelect(query, returnType, false);
    }
    public executeSQLSelect(query, returnType, dump) {
        def conn = this.getSqlConnection();
        def result = conn.rows(query);
        if (dump) {
            result.each {
                m ->
                    println "==="
                    m.each {
                        key, value -> println "$key : $value"
                    }
                    println "==="
            }
        }
        if (returnType == 'map') {
            return result;
        }
        else if (returnType == 'json') {
            def json = JsonOutput.toJson(result)
            return json;
        }
        else {
            throw new PluginException("Unknown return type");
        }
    }
}

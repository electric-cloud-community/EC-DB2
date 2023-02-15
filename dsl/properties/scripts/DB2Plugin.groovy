class DB2Plugin {
    static def CONFIG_PROPERTY_SHEET = 'ec_plugin_cfgs'
    static def CONFIG_FIELD_NAME = 'config'
    static def pluginCore;
    static def build(def configName = null) {
        def efClient = new EFClient()

        if (!configName) {
            configName = efClient.getParameter(CONFIG_FIELD_NAME)
        }
        def pluginProjectName = '$[/myProject/projectName]'
        def config = efClient.getConfigValues(CONFIG_PROPERTY_SHEET, configName, pluginProjectName)
        pluginCore = new DB2PluginCore(
            userName: config.credential.userName,
            password: config.credential.password,
            db2InstanceAddress: config.sql_server_url,
            db2InstancePort: config.sql_server_port,
            databaseName: config.database_name,
            driverClass: "com.ibm.db2.jcc.DB2Driver",
            licensePath: config.license_path
        );
        if (config.driver_path) {
            pluginCore.driverPath = config.driver_path;
        }
        pluginCore.init();
    }
    static def executeSQL(parameters)  {
        def query = getSQLQueryFromParameters(parameters);
        println "Executing query:"
        println query

        def result;
        try {
            result = pluginCore.executeSQLUniversal(query, "json")
        }
        catch (Exception e) {
            println "Error occured: " + e.getMessage();
            System.exit(1)
        }

        println "Result:"
        if (result.getClass() == Integer) {
            result = result.toString();
            println result
        }
        else {
            println(JsonOutput.prettyPrint(result))
        }
        def efClient = new EFClient()
        efClient.setProperty(parameters.propertyPath + '/' + 'result', result);
    }
    static def executeSQLSelect(parameters) {
        def query = getSQLQueryFromParameters(parameters);
        println "Executing query:"
        println query
        def json = pluginCore.executeSQLSelect(query, "json", true);
        println("Result:");
        println(JsonOutput.prettyPrint(json))
        def efClient = new EFClient()
        efClient.setProperty(parameters.propertyPath + '/' + 'result', json);
    }
    static def executeSQLBatch(parameters) {
        def query = getSQLQueryFromParameters(parameters);
        println "Executing query:"
        println query
        def result = pluginCore.executeSQLBatch(query);
        def json = JsonOutput.toJson(result)
        println("Result:");
        println(JsonOutput.prettyPrint(json))
        def efClient = new EFClient()
        efClient.setProperty(parameters.propertyPath + '/' + 'result', json);
    }
    static def getSQLQueryFromParameters(parameters) {
        if (parameters.sql && parameters.sqlFile) {
            throw new PluginException("Both the SQL statement and the path to the SQL file cannot be specified at the same time.");
        }
        if (!parameters.sql && !parameters.sqlFile) {
            throw new PluginException("Either the SQL statement or the path to the SQL file must be specified.");
        }
        def query = "";
        if (parameters.sql) {
            query = parameters.sql
        }
        else {
            def file = new File(parameters.sqlFile);
            if (!file.exists()) {
                throw new PluginException("File " + parameters.sqlFile + " does not exist");
            }
            if (file.isDirectory()) {
                throw new PluginException("File " + parameters.sqlFile + " is a directory");
            }
            query = file.text;
        }
    }
}

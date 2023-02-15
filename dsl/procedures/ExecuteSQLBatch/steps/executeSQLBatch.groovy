$[/myProject/scripts/preamble]

Map parameters = [:]
parameters.sql = '''
$[sql]
'''.trim()

parameters.sqlFile = '''
$[sql_file_path]
'''.trim()

parameters.propertyPath = '''
$[result_properties]
'''.trim();

DB2Plugin.build()

def pCore = DB2Plugin.pluginCore;

if (pCore.licensePath) {
    println "Loading license file: " + pCore.licensePath
    ecl.loadClass(pCore.licensePath);
}

if (pCore.driverPath && pCore.driverClass) {
    ecl.loadClass(pCore.driverPath, pCore.driverClass);
}

DB2Plugin.executeSQLBatch(parameters)

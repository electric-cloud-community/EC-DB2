$[/myProject/scripts/preamble]

Map parameters = [:]

def sql = '''
$[sql]
'''

def sqlFile = '''
$[sql_file_path]
'''

def propertyPath = '''
$[result_properties]
'''

parameters.propertyPath = propertyPath.trim()
parameters.sqlFile = sqlFile.trim()
parameters.sql = sql.trim()

DB2Plugin.build()

def pCore = DB2Plugin.pluginCore;

if (pCore.licensePath) {
    println "Loading license file: " + pCore.licensePath
    ecl.loadClass(pCore.licensePath);
}
if (pCore.driverPath && pCore.driverClass) {
    ecl.loadClass(pCore.driverPath, pCore.driverClass);
}

DB2Plugin.executeSQL(parameters)

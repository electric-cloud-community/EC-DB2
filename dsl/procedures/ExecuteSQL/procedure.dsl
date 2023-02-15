import java.io.File

def procName = 'ExecuteSQL'
def procDesc = 'Executes SQL query against DB2 instance.'
procedure procName, description: procDesc, {

	step 'ExecuteSQL',
    	  command: new File(pluginDir, "dsl/procedures/$procName/steps/executeSQL.groovy").text,
    	  // shell: 'ec-groovy -classpath "$[db2_driver_jar_dir]" {0}'
          shell: 'ec-groovy'
}
  

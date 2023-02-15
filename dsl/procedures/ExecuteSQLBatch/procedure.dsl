import java.io.File

def procName = 'ExecuteSQLBatch'
def procDesc = 'Executes a batch of the SQL queries.'
procedure procName, description: procDesc, {

	step 'ExecuteSQLBatch',
    	  command: new File(pluginDir, "dsl/procedures/$procName/steps/executeSQLBatch.groovy").text,
          shell: 'ec-groovy'
}
  

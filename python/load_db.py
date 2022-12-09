import sqlite3
import xlrd
from sqlite3 import Error
from os.path import isfile


database = r'data\out\upscale.db'
dataSource = r'data_builder.xls'


SHEET_UNITS = 'units'
SHEET_INGREDIENTS = 'ingredients'
SHEET_STEP_TYPES = 'step_types'
SHEET_STEPS = 'steps'
SHEET_RECIPES = 'recipes'
SHEET_RECIPE_STEPS = 'recipe_steps'

#All foreign keys must be added to this list. 
FOREIGN_KEYS = ['unit_id', 'step_type_id', 'recipe_id', 'step_id', 'ingredient_id']


def create_connection(db_file):
    """ create a database connection to the SQLite database
        specified by db_file
    :param db_file: database file
    :return: Connection object or None
    """
    conn = None
    try:
        conn = sqlite3.connect(db_file)
        return conn
    except Error as e:
        print(type(e).__name__)
        print(e)
    return conn

def execute_sql_list(conn, sqlStrings):
    for sqlString in sqlStrings: 
        execute_sql(conn, sqlString)

def execute_sql(conn, sql):
    """ create a table from the create_table_sql statement
    :param conn: Connection object
    :param create_table_sql: a CREATE TABLE statement
    :return:
    """
    try:
        c = conn.cursor()
        c.execute(sql)
    except Error as e:
        print(f'FAILURE - executed {sql}')
        print(e)
        return False
        
    print(f'SUCCESS - executed {sql}')
    return True

def getIDs(conn, key, table):
    try:
        c = conn.cursor()
        c.execute(f'SELECT {key}, id FROM {table}')
        IDs = c.fetchall()
    except Error as e:
        print(e)

    print (f'Fetched {len(IDs)} {table} objects')
    return dict(IDs)

def getInsertString(valueDict, table):
    headers = valueDict.keys()
    vals = [str(value) for value in valueDict.values()]
    
    sqlStart = 'INSERT INTO '
    sqlMid1 ='('
    sqlMid2 = ') VALUES('
    sqlEnd =  ');'
    
    sqlString = sqlStart + table + sqlMid1 + ', '.join(headers) + sqlMid2 + ', '.join(vals) + sqlEnd
    return sqlString


def splitRowHeaders(xlDoc, sheetName):
    
    sheet = xlDoc.sheet_by_name(sheetName)
    
    headerRow = sheet.row(0)
    headers = []
    for cell in headerRow:
        headers.append(cell.value)
                
    rows = []
    for i in range(1, sheet.nrows):
        rows.append(sheet.row(i))
        
    return (headers, rows)   
    
def defaultParse(headers, row):
    
    valueDict = {}
    
    for i in range(len(row)):
        cell = row[i]
        header = headers[i]
        
        #if empty, do not add this to output
        if cell.ctype is xlrd.XL_CELL_EMPTY:
            continue 
        
        elif cell.ctype is xlrd.XL_CELL_TEXT:
            val = "\"" + cell.value + "\""
        
        else:
            val = str(cell.value)
        
        valueDict[header] = val
        
    return valueDict
    
def commit(conn):
    if conn is not None:
        # print(sqlString)
        conn.commit()
    else:
        print("Error, conn is null")

def insertForeignKeys(foreignKeyHeader, foreignKeyDict, valueDict):
    #if there is a unitID, swap it for foreign key
    if foreignKeyHeader in valueDict:
        #we have to remove quotes from this 
        idVal = valueDict[foreignKeyHeader][1:-1]
        
        if idVal in foreignKeyDict:
            valueDict[foreignKeyHeader] = str(foreignKeyDict[idVal])
        else:
            print(f'Error: invalid foreign key: {idVal} for {valueDict["name"]}')
    return valueDict
    
           
    
def main():
    # create a database connection
    conn = create_connection(database)
    
    if not isfile(dataSource):
        print (f'File {dataSource} does not exist')
        return
    
    xlDoc = xlrd.open_workbook(dataSource)
             
    # PARSE RECIPES
    sheetName = SHEET_RECIPES
    (headers, rows) = splitRowHeaders(xlDoc, sheetName)
    for row in rows:
        valueDict = defaultParse(headers, row)
        execute_sql(conn, getInsertString(valueDict, sheetName))
    commit(conn)
    recipeDict = getIDs(conn, 'name', SHEET_RECIPES)
    print(recipeDict)
    
    
    # PARSE RECIPES_STEPS
    sheetName = SHEET_RECIPE_STEPS
    (headers, rows) = splitRowHeaders(xlDoc, sheetName)
    recipePos = 0.0
    lastRecipe = None
    
    for row in rows:
        valueDict = defaultParse(headers, row)
        valueDict['step_order'] = recipePos
        
        currentRecipe = valueDict['recipe_id']
        
        value_dict = insertForeignKeys('recipe_id', recipeDict, valueDict)
        
        execute_sql(conn, getInsertString(valueDict, sheetName))
        
        
        if lastRecipe and currentRecipe != lastRecipe:
            lastRecipe = currentRecipe
            recipePos = 0.0
        else:
            recipePos += 1
               
    commit(conn)
    conn.close()


    return 
    
    



if __name__ == '__main__':
    main()
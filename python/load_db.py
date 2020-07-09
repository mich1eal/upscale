import sqlite3
import csv
import xlrd
from sqlite3 import Error
from os.path import isfile


database = r'data\out\upscale.db'
dataSource = r'data_builder.xlsx'


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
    vals = valueDict.values()
    
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
    
    # PARSE UNITS
    sheetName = SHEET_UNITS
    (headers, rows) = splitRowHeaders(xlDoc, sheetName)
    for row in rows:
        valueDict = defaultParse(headers, row)
        execute_sql(conn, getInsertString(valueDict, sheetName))
        
    unitDict = getIDs(conn, 'name', SHEET_UNITS)
    print(unitDict)
    
    
    # PARSE INGREDIENTS
    sheetName = SHEET_INGREDIENTS
    (headers, rows) = splitRowHeaders(xlDoc, sheetName)
    for row in rows:
        valueDict = defaultParse(headers, row)
        valueDict = insertForeignKeys('unit_id', unitDict, valueDict)
        execute_sql(conn, getInsertString(valueDict, sheetName))
        
    commit(conn)
    
    # PARSE STEP_TYPES
    sheetName = SHEET_STEP_TYPES
    (headers, rows) = splitRowHeaders(xlDoc, sheetName)
    for row in rows:
        valueDict = defaultParse(headers, row)
        execute_sql(conn, getInsertString(valueDict, sheetName))
    commit(conn)
    stepTypeDict = getIDs(conn, 'name', SHEET_STEP_TYPES)
    print(stepTypeDict)
    
    # PARSE STEPS
    sheetName = SHEET_STEPS
    (headers, rows) = splitRowHeaders(xlDoc, sheetName)
    for row in rows:
        valueDict = defaultParse(headers, row)
        valueDict = insertForeignKeys('step_type_id', stepTypeDict, valueDict)
        execute_sql(conn, getInsertString(valueDict, sheetName))
    commit(conn)
    stepDict = getIDs(conn, 'title', SHEET_STEPS)
    print(stepDict)
    
    # PARSE RECIPES
    sheetName = SHEET_RECIPES
    (headers, rows) = splitRowHeaders(xlDoc, sheetName)
    for row in rows:
        valueDict = defaultParse(headers, row)
        valueDict = insertForeignKeys('unit_id', unitDict, valueDict)
        execute_sql(conn, getInsertString(valueDict, sheetName))
    commit(conn)
    recipeDict = getIDs(conn, 'name', SHEET_RECIPES)
    print(recipeDict)
    
    
    # PARSE RECIPES_STEPS
    sheetName = SHEET_RECIPE_STEPS
    (headers, rows) = splitRowHeaders(xlDoc, sheetName)
    recipePos = 0 
    recipeValid = True
    lastRecipe = None
    
    for row in rows:
        valueDict = defaultParse(headers, row)
        
        currentRecipe = valueDict['recipe_id']
        #new recipe
        if currentRecipe != lastRecipe:
            #if all steps were valid for last recipe, commit it: 
                
            lastRecipe = currentRecipe
            recipeValid = True
            recipePos = 0
                    
        
        
        valueDict = insertForeignKeys('unit_id', unitDict, valueDict)
        
        
    commit(conn)



    return 
    
    stepTypeDict = {}
    unitDict = {}

    for table in tables: 
        filePath = 'data\\in\\' + table + '.csv'

        with open(filePath, 'r') as file:
            reader = csv.reader(file)
            headers = next(reader)

            #Iterate through row and build SQL statement 
            for row in reader: 

                #We will use this to build our SQL insert statement 
                cols = []
                vals = []

                
                # create tables
                if conn is not None:
                    # print(sqlString)
                    execute_sql(conn, sqlString)

                else:
                    print("Error! cannot create the database connection.")


            print("Attempting to commit changes")
            if conn is not None:
                # print(sqlString)
                conn.commit()

            else:
                print("Error! cannot create the database connection.")

        # After updating db, get list of IDs for foreign keys
        if table == 'step_types':
            stepTypeDict = getStepTypeIDs(conn)

        if table == 'units':
            unitDict = getUnitIDs(conn)



if __name__ == '__main__':
    main()
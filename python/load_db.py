import sqlite3
import csv
from sqlite3 import Error

# 

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
        print('successfully executed: ' + sql)
    except Error as e:
        print(e)

def getStepTypeIDs(conn):
    try:
        c = conn.cursor()
        c.execute('SELECT name, id FROM step_types')
        IDs = c.fetchall()
    except Error as e:
        print(e)

    print ('Fetched {count} step_type objects'.format(count=len(IDs)))
    return dict(IDs)

def getUnitIDs(conn):
    try:
        c = conn.cursor()
        c.execute('SELECT name, id FROM units')
        IDs = c.fetchall()
    except Error as e:
        print(e)

    print ('Fetched {count} unit objects'.format(count=len(IDs)))
    return dict(IDs)

def isNumber(val):
    try:
        float(val)
        return True
    except:
        return False

def isInt(val):
    try:
        return float(val).is_integer()
    except:
        return False


def main():
    database = r'C:\Users\msmil\workspaces_local\upscale\python\data\out\upscale.db'

    # create a database connection
    conn = create_connection(database)

    tables = ['step_types', 'steps', 'units', 'ingredients']

    sqlStart = 'INSERT INTO '
    sqlMid1 ='('
    sqlMid2 = ') VALUES('
    sqlEnd =  ');'

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

                for i in range(len(row)):
                    if row[i].strip():
                        val = row[i]
                        col = headers[i]

                        # if this is a foreign key, replace it
                        if col == 'step_type_id':
                            val = stepTypeDict[val]
                        elif col == 'unit_id':
                            val = unitDict[val]

                        if isInt(val):
                            #get rid of decimal point
                            val = int(val)
                        elif isNumber(val):
                            #cast to float
                            val = float(val)
                        else:
                            #otherwise val is string, wrap in quotes
                            val = "\"" + val + "\""

                        cols.append(col)
                        vals.append(str(val))

                sqlString = sqlStart + table + sqlMid1 + ', '.join(cols) + sqlMid2 + ', '.join(vals) + sqlEnd
                print('created SQL: ' + sqlString)

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
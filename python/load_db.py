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


def main():
    database = r'C:\Users\msmil\workspaces_local\upscale\python\data\out\upscale.db'

    # create a database connection
    conn = create_connection(database)

    with open(r'data\in\ingredients.csv', 'r') as file:
        reader = csv.reader(file)
        headers = next(reader)

        #Iterate through row and build SQL statement 
        for row in reader: 

            #We will use this to build our SQL insert statement 
            sqlStart = 'INSERT INTO ingredients('
            sqlMid = ') VALUES('
            sqlEnd =  ');'
            cols = []
            vals = []

            for i in range(len(row)):
                if row[i].strip():
                    cols.append(headers[i])
                    val = row[i]
                    try:
                    	float(val)
                    except:
                    	val = "\"" + val + "\""

                    vals.append(val)

            sqlString = sqlStart + ', '.join(cols) + sqlMid + ', '.join(vals) + sqlEnd

            # create tables
            if conn is not None:
                # print(sqlString)
                execute_sql(conn, sqlString)

            else:
                print("Error! cannot create the database connection.")


if __name__ == '__main__':
    main()
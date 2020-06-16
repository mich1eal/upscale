import sqlite3
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
        print('successfully executed sql')
    except Error as e:
        print(e)


def main():
    database = r'C:\Users\msmil\workspaces_local\upscale\python\data\upscale.db'



    with open('upscale_create.sql', 'r') as sqlFile:
    	# sqlite only executes one statement at a time, so we will split each statement out
    	sqlStrings = sqlFile.read().split(';')

    # add semicolon back in
    sqlStrings = [sqlString + ';' for sqlString in sqlStrings]

    # create a database connection
    conn = create_connection(database)

    # create tables
    if conn is not None:
        # create projects table
        execute_sql_list(conn, sqlStrings)

    else:
        print("Error! cannot create the database connection.")


if __name__ == '__main__':
    main()
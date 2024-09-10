package edu.upenn.cis.db.postgres;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.postgresql.util.PSQLException;

import edu.upenn.cis.db.datalog.simpleengine.LongSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.StringSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.Tuple;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.store.StoreResultSet;
import edu.upenn.cis.db.helper.Util;

public class Postgres {
	final static Logger logger = LogManager.getLogger(Postgres.class);

	private Connection conn = null;
	private Statement stmt = null;
	private String dbname = null;

	public String getDBname() {
		return dbname;
	}
	
	public boolean connect(String ip, int port, String username, String password, String name) {
//		System.out.println("[connect] conn: " + conn + " stmt: " + stmt);

		if (conn != null) {
			disconnect();
		}
		
		try {
			Class.forName("org.postgresql.Driver");
			conn = DriverManager
					.getConnection("jdbc:postgresql://" + ip + ":" + port +"/" + name, username, password);
//			conn.setAutoCommit(false);

			stmt = conn.createStatement();
			dbname = name;
			
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public void disconnect() {
		if (conn != null) {
			try {
				stmt.close();
				conn.close();
				conn = null;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
//		try {
//			if (conn.isClosed() == false) {
//				throw new IllegalArgumentException("conn is not closed!");
//			}
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	public void executeUpdate(String query) {
//		System.out.println("[executeUpdate] query: " + query + " stmt: " + stmt + " dbname: " + dbname);
//		int tid = Util.startTimer();
		
		Util.writeToFile("test.sql", query + "\n\n", true); // log sql
		
		try {
			stmt.execute("analyze N_g");
			stmt.execute("analyze E_g");

//			ResultSet rs = stmt.executeQuery("EXPLAIN " + query);
//			while(rs.next()) {
//				System.out.println("[Postgres] 4321999rs: " + rs.getShort(0));
//			}

			System.out.println("[executeUpdate] ... after analyze");
			stmt.executeUpdate(query);
		} catch (PSQLException e) {
			System.out.println("[ERR] query: " + query + " e: " + e + " msg: " + e.getMessage());
		} catch ( Exception e ) {
			System.out.println("[ERR] query2: " + query + " e: " + e + " msg: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void executeUpdate2(String query) {
//		System.out.println("[executeUpdate] query: " + query + " stmt: " + stmt + " dbname: " + dbname);
//		int tid = Util.startTimer();
		try {
//			stmt.executeUpdate("COMMIT; VACUUM; COMMIT;");
			ResultSet rs = stmt.executeQuery("EXPLAIN ANALYZE " + query);
			while(rs.next()) {
				System.out.println("[Postgres] 4321rs: " + rs.getShort(0));
			}
			System.out.println("[Postgres] ************************ sisdio23109847");
		} catch (PSQLException e) {
			Util.Console.errln("query: " + query + " e: " + e + " msg: " + e.getMessage());
		} catch ( Exception e ) {
			Util.Console.errln("query2: " + query + " e: " + e + " msg: " + e.getMessage());
			e.printStackTrace();
		}
	}

//	public void createTable(String name) {
//		try {
//			String sql = "CREATE TABLE " + name + " " +
//					"(ID INT PRIMARY KEY     NOT NULL," +
//					" NAME           TEXT    NOT NULL, " +
//					" AGE            INT     NOT NULL, " +
//					" ADDRESS        CHAR(50), " +
//					" SALARY         REAL)";
//			stmt.executeUpdate(sql);
//		} catch ( Exception e ) {
//			e.printStackTrace();
//		}
//	}

	public void insert(String name, int i) {
		Random rand = new Random();
		int rv = rand.nextInt(99)+1;

		String sql = "INSERT INTO " + name +" (ID,NAME,AGE,ADDRESS,SALARY) "
				+ "VALUES (" + i + ", 'Paul', " + rv + ", 'California', 20000.00 );";
		try {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ResultSet getResultSetFromSelect(String query) {
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery(query);		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rs;
	}

	public StoreResultSet select(String query) {
		ResultSet rs = null;
		StoreResultSet result = new StoreResultSet();

		Util.writeToFile("test.sql", query + "\n\n", true); // log sql
		
//		if (query.startsWith("((SELECT DISTINCT R0._1 AS _0 FROM E_g AS R0 CROSS JOIN") == true) {
//			query = "((SELECT count(DISTINCT R0._1) FROM E_g AS R0 CROSS JOIN E_g AS R2 CROSS JOIN E_g AS R4 CROSS JOIN E_g AS R6 CROSS JOIN N_g AS R8 CROSS JOIN N_g AS R10 CROSS JOIN N_g AS R12 CROSS JOIN N_g AS R14 WHERE R0._2 = R6._2 AND R0._2 = R8._0 AND R0._1 = R2._2 AND R0._1 = R10._0 AND R4._2 = R6._1 AND R4._2 = R12._0 AND R2._1 = R4._1 AND R2._1 = R14._0 AND R0._3 = 'KNOWS' AND R2._3 = 'HASCREATOR' AND R4._3 = 'REPLYOF' AND R6._3 = 'HASCREATOR' AND R8._1 = 'Person' AND R10._1 = 'Person' AND R12._1 = 'Post' AND R14._1 = 'Comment' AND R0._1 < 3945000));";
//		}
		
		int numRows = 0;
		int numCols = 0;

		try {
			System.out.println("[Postgres-342] query: " + query);

//			System.out.println("rs: " + rs.toString())		
//			stmt.executeUpdate("COMMIT; VACUUM; COMMIT;");

			stmt.execute("analyze N_g");
			stmt.execute("analyze E_g");
			
			rs = stmt.executeQuery("explain " + query);
//			rs = stmt.executeQuery("select plan_table_output from table(dbms_xplan.display())");
			while (rs.next())
			{
			  System.out.println(rs.getString(1));
			}
			
			System.out.println("[Postgres-342] queryA: " + query);
			
			int tid4 = Util.startTimer();
			rs = stmt.executeQuery(query);

			System.out.println("[Postgres-343] etime: " + Util.getElapsedTime(tid4) + " query: " + query);
//			Util.Console.logln("=== [BEGIN] QUERY ANSWER ===");

			ResultSetMetaData rsmd = rs.getMetaData();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				result.getColumns().add(rsmd.getColumnName(i));
			}
			numCols = rsmd.getColumnCount();

//			if (Config.isAnswerEnabled() == true) { 
//				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
//					System.out.print(rsmd.getColumnName(i) + " ");
//				}
//				System.out.println("\n=========");
//			} 

			while ( rs.next() ) {
				Tuple<SimpleTerm> t = new Tuple<SimpleTerm>();
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					if (rsmd.getColumnType(i) == Types.INTEGER) {
						int val = rs.getInt(i);
						t.getTuple().add(new LongSimpleTerm(val));
//						if (Config.isAnswerEnabled() == true) {
//							System.out.print(val + " ");
//						}
					} else {
						String val = rs.getString(i);
						t.getTuple().add(new StringSimpleTerm(val));
//						if (Config.isAnswerEnabled() == true) {
//							System.out.print(val + " ");
//						}
					} 
				}
				result.getResultSet().add(t);
				numRows++;
//				if (Config.isAnswerEnabled() == true) {;
//					System.out.println();
//				}
			}
			rs.close();	
//			Util.Console.logln("(" + numRows + " row(s) " + numCols + " col(s))");			         
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Util.Console.errln("select failed query: " + query);

			e.printStackTrace();
		}
		return result;
	}

	public boolean createDatabase(String name) {
		String sql = "CREATE DATABASE " + name;
		try {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
//			System.out.println("[createDatabase] sql: " + sql);
//			e.printStackTrace();
//			Util.Console.errln("Failed to create database [" + name + "]. It doesn't exists or error occurred.");
			return false;
		}
		return true;
	}	

	public boolean dropDatabase(String name) {
		String sql = "DROP DATABASE " + name;

		try {
//			System.out.println("dbname: " + dbname+ " drop: " + sql);
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("****FAILED TO DROP DB dbname: " + dbname+ " drop: " + sql);
			return false;
		}
		return true;
	}

//	public boolean useDatabase(String name) {
//		return connect(name);
//	}

	public void dropTable(String name) {
		String sql = "DROP TABLE IF EXISTS " + name;
		try {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void copy(String sql, String filePath) {
		CopyManager copyManager;
		try {
			copyManager = new CopyManager((BaseConnection) conn);
			FileReader fileReader = new FileReader(filePath);
			copyManager.copyIn(sql, fileReader );
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public long importFromCSV(String relName, String filePath) {
		// TODO Auto-generated method stub
		long rowsInserted = 0;
		try {
			String cols = "";
			if (relName.equalsIgnoreCase("n")) {
				cols = "(_0, _1)";
			} else {
				cols = "(_0, _1, _2, _3)";
			}
			rowsInserted = new CopyManager((BaseConnection) conn)
					.copyIn(
							"COPY " + relName + Config.relname_base_postfix + " " + cols + " FROM STDIN (FORMAT csv, HEADER)", 
							new BufferedReader(new FileReader(filePath))
							);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Util.Console.errln("File Not Exists [" + filePath +"]"); 
//			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rowsInserted;
	}
	
	/**
	 * @param dbName Database name
	 * @param filePath File path to the sql file to import
	 */
	public static void loadDBFromSql(String dbName, String filePath) {
		System.out.println("[Postgres] loadDbFromSql dbName[" + dbName + "] filePath[" + filePath + "]");

		String pgPassword = Config.get("postgres.password");
		String cmds[] = {
				"PGPASSWORD=" + pgPassword + " psql -X -U postgres -c \"drop database if exists " + dbName + "\"",
				"PGPASSWORD=" + pgPassword + " psql -X -U postgres -c \"DISCARD ALL\"",
				"PGPASSWORD=" + pgPassword + " psql -X -U postgres -c \"create database " + dbName + "\"",
				"PGPASSWORD=" + pgPassword + " psql -X -U postgres " + dbName + " < " + filePath,				
		};
		
		ProcessBuilder processBuilder = new ProcessBuilder();
		for (int i = 0; i < cmds.length; i++) {
			System.out.println("cmd i: " + i + " => " + cmds[i]);

			// Run a shell command
			processBuilder.command("bash", "-c", cmds[i]);
			try {

				Process process = processBuilder.start();

				StringBuilder output = new StringBuilder();

				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line + "\n");
				}

				int exitVal = process.waitFor();
				if (exitVal == 0) {
					System.out.println("Success!");
					System.out.println(output);
//					System.exit(0);
				} else {
					System.out.println("Failure!");
					//abnormal...
					System.exit(-1);
				}

			} catch (IOException e) {
				System.out.println("IOEXception");
				e.printStackTrace();
			} catch (InterruptedException e) {
				System.out.println("Exception");
				e.printStackTrace();
			}
		
		}
		System.out.println("[Postgres] loadDbFromSql Done");

		// -- Linux --

//				time PGPASSWORD=postgres@ psql -X -U postgres $DB_NAME < ${SQL_BACKUP}

	}
	
	public static void main(String[] args) {
		Postgres.loadDBFromSql("syn13", "experiment/dataset/postgres/SYN-10000-1000.sql");
		

		
		// TODO Auto-generated method stub
//		createDatabase("test123");
//		useDatabase("test123");

//		int et = Util.startTimer();
//		copy("COPY n_g(_0,_3) FROM STDIN WITH DELIMITER ',' CSV", "script_gen_n.csv");
//		copy("COPY e_g(_0,_3,_6,_9) FROM STDIN WITH DELIMITER ',' CSV", "script_gen_e.csv");

//		System.out.println("ET: " + Util.getElapsedTime(et));
		
//		dropDatabase("test123");
//		disconnect();
		
//		System.out.println("Done.");
	}	
}

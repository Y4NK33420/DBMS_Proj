package edu.upenn.cis.db.graphtrans.store.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.upenn.cis.db.helper.Util;

public class PGtest {
	private final static String url = "jdbc:postgresql://localhost/exp";
	private final static String user = "postgres";
	private final static String password = "postgres@";

//	private static final String QUERY = "((SELECT count(DISTINCT R0._1) AS cnt FROM E_g AS R0 CROSS JOIN E_g AS R2 CROSS JOIN E_g AS R4 CROSS JOIN E_g AS R6 CROSS JOIN N_g AS R8 CROSS JOIN N_g AS R10 CROSS JOIN N_g AS R12 CROSS JOIN N_g AS R14 WHERE R0._2 = R6._2 AND R0._2 = R8._0 AND R0._1 = R2._2 AND R0._1 = R10._0 AND R4._2 = R6._1 AND R4._2 = R12._0 AND R2._1 = R4._1 AND R2._1 = R14._0 AND R0._3 = 'KNOWS' AND R2._3 = 'HASCREATOR' AND R4._3 = 'REPLYOF' AND R6._3 = 'HASCREATOR' AND R8._1 = 'Person' AND R10._1 = 'Person' AND R12._1 = 'Post' AND R14._1 = 'Comment' AND R0._1 < 3945000))";

	private static ArrayList<String> updates = new ArrayList<String>();
	
	public void getUserById() {
		// using try-with-resources to avoid closing resources (boiler plate
		// code)

		int tid = Util.startTimer();
		Connection connection;
		Statement statement;
		try {
			connection = DriverManager.getConnection(url, user, password);
			statement = connection.createStatement();
				
			for (String q : updates) {
				System.out.println("#########update query q: " + q);
				statement.executeUpdate(q);
				System.out.println("*********time: " + Util.getElapsedTime(tid));
			}
			
		} catch (SQLException e) {
			printSQLException(e);
		}
	}

	public static void main(String[] args) {
		updates.add("DROP TABLE IF EXISTS GENNEWID_MAP;"
				+ "CREATE TABLE GENNEWID_MAP (\n"
				+ "  NEWID SERIAL PRIMARY KEY NOT NULL,\n"
				+ "  VIEWRULEID varchar(64) NOT NULL,\n"
				+ "  INPUTS integer[]\n"
				+ ");\n"
				+ "DROP INDEX IF EXISTS newid_vrm_idx;"
				+ "CREATE INDEX newid_vrm_idx ON GENNEWID_MAP (VIEWRULEID, INPUTS);\n"
				+ "ALTER SEQUENCE GENNEWID_MAP_NEWID_seq RESTART WITH 100000000 INCREMENT BY 1;\n"
				+ "CREATE OR REPLACE FUNCTION GENNEWID_CONST(varchar(64), VARIADIC arr int[])\n"
				+ "   RETURNS int AS $$\n"
				+ "DECLARE\n"
				+ "  inserted_id integer;\n"
				+ "  existing_id integer;\n"
				+ "BEGIN\n"
				+ "  SELECT NEWID INTO existing_id FROM GENNEWID_MAP\n"
				+ "  WHERE VIEWRULEID = $1 AND INPUTS = $2;\n"
				+ "  IF not found THEN\n"
				+ "    INSERT INTO GENNEWID_MAP (VIEWRULEID, INPUTS) VALUES ($1,$2) RETURNING NEWID INTO inserted_id;\n"
				+ "    RETURN inserted_id;\n"
				+ "  ELSE\n"
				+ "    RETURN existing_id;\n"
				+ "  END IF;\n"
				+ "END;\n"
				+ "$$ LANGUAGE 'plpgsql';\n"
				+ "\n"
				+ "\n"
				+ "CREATE OR REPLACE FUNCTION GENNEWID_MAP_v1_kk1(int,int)\n"
				+ "	RETURNS int AS $$\n"
				+ "DECLARE\n"
				+ "	existing_id integer;\n"
				+ "BEGIN\n"
				+ "SELECT * INTO existing_id FROM GENNEWID_CONST('v1_kk1', VARIADIC Array[$1,$2]) AS T;\n"
				+ "RETURN existing_id;\n"
				+ "END;\n"
				+ "$$ LANGUAGE 'plpgsql';\n"
				+ "\n"
				+ "\n"
				+ "CREATE OR REPLACE FUNCTION GENNEWID_MAP_v1_kk2(int,int)\n"
				+ "	RETURNS int AS $$\n"
				+ "DECLARE\n"
				+ "	existing_id integer;\n"
				+ "BEGIN\n"
				+ "SELECT * INTO existing_id FROM GENNEWID_CONST('v1_kk2', VARIADIC Array[$1,$2]) AS T;\n"
				+ "RETURN existing_id;\n"
				+ "END;\n"
				+ "$$ LANGUAGE 'plpgsql';\n"
				+ "\n"
				+ "");
		
		updates.add("CREATE OR REPLACE VIEW MATCH_v1_0 AS ((SELECT DISTINCT R0._0 AS _0, R0._1 AS _1, R0._2 AS _2, R1._0 AS _3, R1._2 AS _4 FROM E_g AS R0 CROSS JOIN E_g AS R1 CROSS JOIN N_g AS R2 CROSS JOIN N_g AS R3 CROSS JOIN N_g AS R4 WHERE R0._1 = R1._1 AND R0._1 = R2._0 AND R0._2 = R3._0 AND R1._2 = R4._0 AND R0._2 < 5000 AND R0._3 = 'W' AND R1._3 = 'P' AND R2._1 = 'P' AND R3._1 = 'A' AND R4._1 = 'V'));\n"
				+ "CREATE OR REPLACE  VIEW MATCH_v1_1 AS ((SELECT DISTINCT R0._0 AS _0, R0._1 AS _1, R0._2 AS _2, R1._0 AS _3, R1._2 AS _4 FROM E_g AS R0 CROSS JOIN E_g AS R1 CROSS JOIN N_g AS R2 CROSS JOIN N_g AS R3 CROSS JOIN N_g AS R4 WHERE R0._1 = R1._1 AND R0._1 = R2._0 AND R0._2 = R3._0 AND R1._2 = R4._0 AND R0._2 < 100 AND R0._3 = 'W' AND R1._3 = 'W' AND R2._1 = 'P' AND R3._1 = 'A' AND R4._1 = 'A'));\n"
				+ "CREATE OR REPLACE  VIEW DMAP_v1 AS ((SELECT DISTINCT R0._0 AS _0, R0._0 AS _1 FROM N_g AS R0));\n"
				+ "CREATE OR REPLACE  VIEW N_v1 AS ((SELECT DISTINCT R1._2 AS _0, 'A' AS _1 FROM MATCH_v1_0 AS R1) UNION (SELECT DISTINCT R1._4 AS _0, 'V' AS _1 FROM MATCH_v1_0 AS R1) UNION (SELECT DISTINCT R1._2 AS _0, 'A' AS _1 FROM MATCH_v1_1 AS R1) UNION (SELECT DISTINCT R1._4 AS _0, 'A' AS _1 FROM MATCH_v1_1 AS R1) UNION (SELECT DISTINCT R0._0 AS _0, R0._1 AS _1 FROM N_g AS R0));\n"
				+ "CREATE OR REPLACE  VIEW E_v1 AS ((SELECT DISTINCT GENNEWID_MAP_v1_kk1(R1._2,R1._4) AS _0, R1._2 AS _1, R1._4 AS _2, 'PUBVEN' AS _3 FROM MATCH_v1_0 AS R1) UNION (SELECT DISTINCT GENNEWID_MAP_v1_kk2(R1._2,R1._4) AS _0, R1._2 AS _1, R1._4 AS _2, 'COAUTHOR' AS _3 FROM MATCH_v1_1 AS R1) UNION (SELECT DISTINCT R0._0 AS _0, R1._1 AS _1, R2._1 AS _2, R0._3 AS _3 FROM E_g AS R0 CROSS JOIN DMAP_v1 AS R1 CROSS JOIN DMAP_v1 AS R2 WHERE R0._2 = R2._0 AND R0._1 = R1._0));\n"
				+ "");
		
		updates.add("CREATE MATERIALIZED VIEW INDEX_v1_0 AS ((SELECT DISTINCT GENNEWID_MAP_v1_kk1(R0._2,R1._2) AS _0, R0._2 AS _1, R1._2 AS _2 FROM E_g AS R0 CROSS JOIN E_g AS R1 CROSS JOIN N_g AS R2 CROSS JOIN N_g AS R3 CROSS JOIN N_g AS R4 WHERE R0._3 = 'W' AND R1._3 = 'P' AND R2._1 = 'P' AND R3._1 = 'A' AND R4._1 = 'V' AND R0._1 = R1._1 AND R0._1 = R2._0 AND R0._2 = R3._0 AND R1._2 = R4._0 AND R0._2 < 5000));");
		
		PGtest example = new PGtest();
		example.getUserById();
		
		
		System.out.println("FINISHED...");
		return;
	}

	public static void printSQLException(SQLException ex) {
		for (Throwable e: ex) {
			if (e instanceof SQLException) {
				e.printStackTrace(System.err);
				System.err.println("SQLState: " + ((SQLException) e).getSQLState());
				System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
				System.err.println("Message: " + e.getMessage());
				Throwable t = ex.getCause();
				while (t != null) {
					System.out.println("Cause: " + t);
					t = t.getCause();
				}
			}
		}
	}
}


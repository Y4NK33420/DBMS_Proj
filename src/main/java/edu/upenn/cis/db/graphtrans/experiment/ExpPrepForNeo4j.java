package edu.upenn.cis.db.graphtrans.experiment;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import edu.upenn.cis.db.Neo4j.Neo4jServerThread;
import edu.upenn.cis.db.graphtrans.Config;

public class ExpPrepForNeo4j {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("[ExpPrepForNeo4j] Start...");
		
		// TODO Auto-generated method stub
		String database = "neo4j";
		Config.load("graphview.conf");

		String basebaseDir = "experiment/dataset/targets";
		File basebaseDirFile = new File(basebaseDir);

		if(!basebaseDirFile.exists()) {
			System.out.println("basebaseDirFile: " + basebaseDirFile + " doesn't exist.");
			System.exit(0);
		}
		if((new File(basebaseDir + "/neo4j")).exists() == false) {
			new File(basebaseDir + "/neo4j").mkdirs();
		}

		File directoryPath = new File(basebaseDir);
		String contents[] = directoryPath.list();
		System.out.println("List of files and directories in the specified directory:");
		String[] excluedDirs = {"backup", "neo4j", "logicblox", "postgres"};
		List<String> excludedDirsArr = Arrays.asList(excluedDirs);
		for(int i = 0; i < contents.length; i++) {
			if ((new File(basebaseDir + "/" + contents[i])).isDirectory() == true) {
				if (excludedDirsArr.contains(contents[i]) == false) {
					System.out.println("DBNAME: " + contents[i]);
					
					String dbName = contents[i]; //"SYN-10000-1000";		
					String srcPath = basebaseDir + "/" + dbName + "/neo4j";
					String dstPath = basebaseDir + "/neo4j/" + dbName;

					Neo4jServerThread.prepareDatabase(dbName, srcPath, dstPath);
				}
			}
		}
		System.out.println("[ExpPrepForNeo4j] End...");
	}

}

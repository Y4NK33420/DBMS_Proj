package edu.upenn.cis.db.logicblox;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.logicblox.connect.ConnectBlox;
import com.logicblox.connect.BloxCommand.AddBlock;
import com.logicblox.connect.BloxCommand.Command;
import com.logicblox.connect.BloxCommand.ExecBlock;
import com.logicblox.connect.BloxCommand.GetPredicatePopcount;
import com.logicblox.connect.BloxCommand.GlobalPredicateName;
import com.logicblox.connect.BloxCommand.LocalPredicateName;
import com.logicblox.connect.BloxCommand.PredicateName;
import com.logicblox.connect.BloxCommand.RemoveBlock;
import com.logicblox.connect.ConnectBlox.AdminRequest;
import com.logicblox.connect.ConnectBlox.AdminResponse;
import com.logicblox.connect.ConnectBlox.CreateWorkSpace;
import com.logicblox.connect.ConnectBlox.DeleteWorkSpace;
import com.logicblox.connect.ConnectBlox.ListWorkSpacesResponse;
import com.logicblox.connect.ConnectBlox.Request;
import com.logicblox.connect.ConnectBlox.Response;
import com.logicblox.connect.ConnectBlox.Transaction;

import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.helper.Util;

public class LogicBlox {
	private static String lb_ip;
	private static int lb_port;
	private static int lb_adminPort;
	
	private static Socket socket;
	private static DataOutputStream dos;
	private static DataInputStream dis;

	private static Socket adminSocket;
	private static DataOutputStream adminDos;
	private static DataInputStream adminDis;	

	private final static String client_id = "prov_graph_trans";
	private static int guid = 1;
	
	public static boolean connect(String ip, int port, int adminPort) {
		lb_ip = ip;
		lb_port = port;
		lb_adminPort = adminPort; // FIXME: admin socket becomes invalid after some time...?

//		System.out.println("[LogiBlox-connect] ip: " + ip + " port: " + port);
		try {   
			socket = new Socket(lb_ip, lb_port);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
		} catch (ConnectException ce) {
//			ce.printStackTrace();
			return false;
		} catch (IOException ie) {
//			ie.printStackTrace();
			return false;
		} catch (Exception e) {
//			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static void disconnect() {
		try {
			dos.close();
			dis.close();
			socket.close();

			if (adminDos != null) {
				adminDos.close();
				adminDis.close();
				adminSocket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static String getGuid() {
		return Integer.toString(guid++);	
	}
	
	public static long getPredicatePopcount(String workspace, String blockName, String pred) {
		GlobalPredicateName globalPred = GlobalPredicateName.newBuilder()
				.setQualifiedName(pred)
				.build();

		PredicateName predName = PredicateName.newBuilder()
				//.setLocalName(localPred)
				.setGlobalName(globalPred)
				.build();
				
		GetPredicatePopcount pop = GetPredicatePopcount.newBuilder()
				.setAll(false)
				.addPredicate(predName)
//				.setPredicate(0, predName)
				.build();

		Command cmd = Command.newBuilder()
				.setPredPopcount(pop)
				.build();	
		
		Transaction tr = Transaction.newBuilder()
				.addCommand(cmd)
				.setWorkspace(workspace)
				.build();
		
		Request req = 
				ConnectBlox.Request.newBuilder()
					.setGuid(getGuid())
					.setClientId(client_id)
					.setTransaction(tr)
				    .build();		
		
		Response res = runRequest(req);
		
		long count = 0;
		try {
			count = res.getTransaction().getCommand(0).getPredPopcount().getPopcount(0).getPopcount();
		} catch(Exception e) {
			throw new IllegalArgumentException("Popcount of predicate [" + pred + "] does not exist.");
		}
		
//		System.out.println("time: " + Util.getElapsedTime(tid));
		return count;
	}
	
	
	public static ArrayList<String> getListWorkSpaces() {
		// FIXME: This should be on 5519 (admin port)
		AdminRequest req = 
				ConnectBlox.AdminRequest.newBuilder()
					.setClientId(client_id)
					.setListWorkspaces(true)
				    .build();
		
		AdminResponse res = runAdminRequest(req);
		
		ListWorkSpacesResponse lws = res.getListWorkspaces();
		
		ArrayList<String> ws = new ArrayList<String>();
		for (int i = 0; i < lws.getNameCount(); i++) {
			ws.add(lws.getName(i));
		}
		return ws;
	}
	
	public static boolean createWorkspace(String workspace, boolean overwrite) {
		CreateWorkSpace cws = CreateWorkSpace.newBuilder()
				.setName(workspace)
				.setOverwrite(true)
				.build();		

		Request req = 
				ConnectBlox.Request.newBuilder()
					.setGuid(getGuid())
					.setClientId(client_id)
					.setCreate(cws)
				    .build();
		
		Response res = runRequest(req);
		
		return res.hasCreate();
	}
	
	public static boolean deleteWorkspace(String workspace) {
		DeleteWorkSpace dws = DeleteWorkSpace.newBuilder()
				.setName(workspace)
				.build();
		
		Request req = 
				ConnectBlox.Request.newBuilder()
					.setGuid(getGuid())
					.setClientId(client_id)
					.setDelete(dws)
				    .build();
		
		Response res = runRequest(req);
		
		return res.hasDelete();
	}
	
	public static Response runAddBlock(String workspace, String blockname, String logic) {
//		Util.resetTimer();
		AddBlock ab;
		if (blockname == null) {
			ab = AddBlock.newBuilder()
					.setLogic(ByteString.copyFromUtf8(logic))
					.build();
		} else {
			ab = AddBlock.newBuilder()
					.setBlockName(blockname)
					.setLogic(ByteString.copyFromUtf8(logic))
					.build();
		}
		System.out.println("[runAddBlock] block[" + blockname + "] logic: \n" + logic);
		int tid = Util.startTimer();
		System.out.println("[runAddBlock] before addblock logic: " + logic);

		Response res = runAddBlockCommand(workspace, ab);
//		System.out.println("[ELAPSED TIME] tc2: " + Util.getElapsedTime(tc));
		
//		long time = Util.getLapTime();
		long et = Util.getElapsedTime(tid);
//		System.out.println("[LogicBlox-runExecBlock] et[" + et + "]");
		System.out.println("[runAddBlock] after addblock et[" + et + "] res: \n" + res);
		
		return res;
	}
	
	public static Response runRemoveBlock(String workspace, String blockname) {
		RemoveBlock rb = RemoveBlock.newBuilder()
				.addBlockName(blockname)
				//.setBlockName(0, blockname)
				.build();
				
		return runRemoveBlockCommand(workspace, rb);
	}
	
	public static Response runExecBlock(String workspace, String logic, boolean existReturn) {
		ArrayList<String> returnLocal = new ArrayList<String>();
		returnLocal.add("_");
		
		ExecBlock ex;
		
		if (existReturn == true) {
			ex = ExecBlock.newBuilder()
				.setLogic(ByteString.copyFromUtf8(logic))				
				.addAllReturnLocal(returnLocal)
				.build();
		} else {
			ex = ExecBlock.newBuilder()
					.setLogic(ByteString.copyFromUtf8(logic))
					.build();
		}
//		System.out.println("[LogicBlox-runExecBlock] logic: \n" + logic);
		int tid = Util.startTimer();
		Response res = runExecBlockCommand(workspace, ex);
		long et = Util.getElapsedTime(tid);
//		System.out.println("[LogicBlox-runExecBlock] et[" + et + "]");
//		System.out.println("[runExecBlock] et[" + et + " res: \n" + res);
		return res;
	}
	
	
	private static Response runExecBlockCommand(String workspace, ExecBlock ex) {
		Command cmd = Command.newBuilder()
				.setExec(ex)
				.build();	

		Transaction tr = Transaction.newBuilder()
				.addCommand(cmd)
				.setWorkspace(workspace)
				.build();
		
		Request req = 
				ConnectBlox.Request.newBuilder()
					.setGuid(getGuid())
					.setClientId(client_id)
					.setTransaction(tr)
					.setLogLevel("info")
				    .build();		
		
//		int et = Util.startTimer();
		Response res = runRequest(req);
//		System.out.println(Util.getElapsedTime(et));
		return res;
	}
	
	private static Response runAddBlockCommand(String workspace, AddBlock ab) {
		Command cmd = Command.newBuilder()
				.setAddBlock(ab)
				.build();	
		
		Transaction tr = Transaction.newBuilder()
				.addCommand(cmd)
				.setWorkspace(workspace)
				.build();
		
		Request req = 
				ConnectBlox.Request.newBuilder()
					.setGuid(getGuid())
					.setClientId(client_id)
					.setLogLevel("info")
					.setTransaction(tr)
				    .build();		
		
//		int tc = Util.startTimer();
		Response res = runRequest(req);
//		System.out.println("[ELAPSED TIME] [runAddBlockCommand] tc: " + Util.getElapsedTime(tc));

		return res;
	}
	
	private static Response runRemoveBlockCommand(String workspace, RemoveBlock rb) {
		/*
		 *  FIXME: Not working yet.
		 */
		Command cmd = Command.newBuilder()
				.setRemoveBlock(rb)
				.build();
		
		Transaction tr = Transaction.newBuilder()
				.addCommand(cmd)
				.setWorkspace(workspace)
				.build();
		
		Request req = 
				ConnectBlox.Request.newBuilder()
					.setGuid(getGuid())
					.setClientId(client_id)
					.setTransaction(tr)
				    .build();		
		
		return runRequest(req);
	}
	
	private static AdminResponse runAdminRequest(AdminRequest req) {	
		AdminResponse res = null;
		
		try { // socket to adminport seems to become invalidated in some time
			adminSocket = new Socket(lb_ip, lb_adminPort);
			adminDos = new DataOutputStream(adminSocket.getOutputStream());
			adminDis = new DataInputStream(adminSocket.getInputStream());
		} catch (ConnectException ce) {
//			ce.printStackTrace();
		} catch (IOException ie) {
//			ie.printStackTrace();
		} catch (Exception e) {
//			e.printStackTrace();
		}
		
		try {
			adminDos.writeInt(174); // ID
			adminDos.writeInt(req.getSerializedSize());
			byte[] bbs = req.toByteArray();
			adminDos.write(bbs);
			adminDos.flush();
			
			int sss = adminDis.readInt();
			int sss2 = adminDis.readInt();
			
			byte[] bss = new byte[sss2];
			int bss1 = adminDis.read(bss);
						 
			res = ConnectBlox.AdminResponse.parseFrom(bss);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return res;
	}
	
	private static Response runRequest(Request req) {	
		Response res = null;
		
		try {
			int tc = Util.startTimer();

			dos.writeInt(173); // ID
			dos.writeInt(req.getSerializedSize());
			byte[] bbs = req.toByteArray();
			dos.write(bbs);
			dos.flush();
			
//			System.out.println("[ELAPSED TIME] tc1: " + Util.getElapsedTime(tc) + " bbs.length: " + bbs.length);
			
			int sss = dis.readInt();
			int bodyLength = dis.readInt();

			int received = 0;
			byte[] bss = new byte[bodyLength];
			while (received < bodyLength) {
				int bss1 = dis.read(bss, received, bodyLength - received);
				received += bss1;
			}
//			System.out.println("[ELAPSED TIME] tc2: " + Util.getElapsedTime(tc) + " bodyLen: " + bodyLength);

			res = ConnectBlox.Response.parseFrom(bss);
//			System.out.println("[ELAPSED TIME] tc3: " + Util.getElapsedTime(tc));
//			System.out.println("res: " + res);

		} catch(InvalidProtocolBufferException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return res;
	}
	
	/**
	 * @param dbName Database name
	 * @param filePath File path to the sql file to import
	 */
	public static void loadDBFromBackup(String dbName, String filePath) {
		System.out.println("[LogicBlox] loadDBFromBackup dbName[" + dbName + "] filePath[" + filePath + "]");

		String lb_bin_dir = Config.get("logicblox.lb_bin_dir");
		if (lb_bin_dir != null && lb_bin_dir.trim().contentEquals("") == false) {
			lb_bin_dir += "/";
		} else {
			lb_bin_dir = "";
		}
		String cmds[] = {
				lb_bin_dir + "lb import-workspace " + dbName + " " + filePath,
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
					System.exit(-1);
					//abnormal...
				}

			} catch (IOException e) {
				System.out.println("IOEXception");
				e.printStackTrace();
			} catch (InterruptedException e) {
				System.out.println("Exception");
				e.printStackTrace();
			}
		
		}
		System.out.println("[LogicBlox] loadDbFromSql Done");

		// -- Linux --

//				time PGPASSWORD=postgres@ psql -X -U postgres $DB_NAME < ${SQL_BACKUP}

	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("LogicBlox Start");
		
		String dbName = "SYN-10000-1000";
		String filePath = "experiment/dataset/targets/logicblox/" + dbName;
		LogicBlox.loadDBFromBackup(dbName, filePath);
		
//		LogicBlox.connect("127.0.0.1", 5518, 5519);
//		
//		LogicBlox.createWorkspace("t",  false);
//		
//		String logic;
//		Response res;
//		
//		String rule = " +E_g_add(22222201, 22222221, 2222222, \"X\").";
//		System.out.println("InsertRuleGen rule: " + rule);
//		res = LogicBlox.runExecBlock(Config.getWorkspace(), rule, false);
//		
//		System.out.println("ws: " + LogicBlox.getListWorkSpaces());
		
//		//logic = "parent(a,b)->string(a), string(b). ";
//		logic = "parent(\"33\",\"34\").";
//		logic += "parent(\"34\",\"35\").";
//		res = LogicBlox.runAddBlock("testws1",  "block1", logic);
//
//		//logic = "parent(a,b)->string(a), string(b). ";
//		logic = "parent(\"35\",\"36\").";
//		logic += "parent(\"37\",\"38\").";
//		res = LogicBlox.runAddBlock("testws1",  "block2", logic);
//
//		res = LogicBlox.runRemoveBlock("testws1", "block1");
//		
//		res = LogicBlox.runExecBlock("testws1", "_(a,b) <- parent(a,b).", true);
//		
//		System.out.println("res: " + res);
//		
//		Relation rel = res.getTransaction().getCommand(0).getExec().getReturnLocal(0);
//		
//		//int numCcols = rel.getColumnCount();
//		List<Column> columns = rel.getColumnList();
//		int numRows = 0;
//		int numCols = rel.getColumnCount();
//		
//		for (int i = 0; i < columns.size(); i++) {
//			if (columns.get(i).hasStringColumn() == true) {
//				numRows = rel.getColumn(0).getStringColumn().getValuesCount(); 
//			}
//		}
//		
//		for (int i = 0; i < numCols; i++) {
//			for (int j = 0; j < numRows; j++) {
//				String val = rel.getColumn(i).getStringColumn().getValues(j);
//				System.out.print(val + " ");
//			}
//			System.out.println();
//		}
//		
//		
//		
//		Util.resetTimer();
//		res = LogicBlox.runAddBlock("testws1",  "block222", "__grand(a,b) <- parent(a,c),parent(c,b).");
//		res = LogicBlox.runAddBlock("testws1",  "block2223", "grand2(a,b) <- parent(a,c),parent(c,b).");
//		System.out.println("Elapsed time1: " + Util.getLapTime());
//
//		
//		Util.resetTimer();
//		long count = LogicBlox.getPredicatePopcount("testws1","block2","parent");
//		System.out.println("Elapsed time2: " + Util.getLapTime());
//		System.out.println("res1: " + count);
		
		//LogicBlox.deleteWorkspace("testws1");
		
//		Util.resetTimer();
//		logic = "" +
////				"R_14a_MAP_v1v(v_359,v_349,v_0,v_358,v_2,v_1,v_4,label,v_3,v_351,v_6,v_352,v_5,v_350,v_7,from_r,to_r,id_l,from,id,to,to_l,id_r,from_l) <- v_382=\"C\",E_g(v_0,v_360,v_361,v_362,v_363,v_364,v_365,v_366,v_367,v_368),v_1=1,N_g(v_376,v_377,v_378,v_383),v_368=\"X\",N_g(v_365,v_366,v_367,v_381),v_7=1,v_379=\"X\",E_g(v_369,v_370,v_371,v_365,v_366,v_367,v_350,v_351,v_352,v_372),v_359=0,N_g(v_362,v_363,v_364,v_380),R_11_E_delta_v1v(from_r,to_r,id_l,from,id,to,label,to_l,v_7,id_r,from_l),v_372=\"X\",v_358=v_359,E_g(id,id_l,id_r,v_350,v_351,v_352,v_3,v_4,v_5,v_6),N_g(v_350,v_351,v_352,v_382),v_349=\"S\",v_381=\"B\",v_380=\"A\",E_g(v_373,v_374,v_375,v_365,v_366,v_367,v_376,v_377,v_378,v_379),v_2=1,v_383=\"D\".\n" + 
////				"R_14a_MAP_v1v(v_359,v_349,v_0,v_358,v_2,v_1,v_4,label,v_3,v_351,v_6,v_352,v_5,v_350,v_7,from_r,to_r,id_l,from,id,to,to_l,id_r,from_l) <- E_g(v_400,v_401,v_402,v_389,v_390,v_391,v_350,v_351,v_352,v_403),v_7=1,v_406=\"C\",v_407=\"D\",v_405=\"B\",N_g(v_350,v_351,v_352,v_407),v_349=\"S\",v_359=0,v_399=\"X\",N_g(v_386,v_387,v_388,v_404),R_11_E_delta_v1v(from_r,to_r,id_l,from,id,to,label,to_l,v_7,id_r,from_l),v_392=\"X\",v_358=v_359,v_2=1,E_g(id,id_l,id_r,v_350,v_351,v_352,v_3,v_4,v_5,v_6),N_g(v_396,v_397,v_398,v_406),N_g(v_389,v_390,v_391,v_405),E_g(v_0,v_384,v_385,v_386,v_387,v_388,v_389,v_390,v_391,v_392),E_g(v_393,v_394,v_395,v_389,v_390,v_391,v_396,v_397,v_398,v_399),v_404=\"A\",v_1=1,v_403=\"X\".\n" + 
////				"R_14a_MAP_v1v(v_359,v_349,v_0,v_358,v_2,v_1,v_4,label,v_3,v_351,v_6,v_352,v_5,v_350,v_7,from_r,to_r,id_l,from,id,to,to_l,id_r,from_l) <- N_g(v_417,v_418,v_419,v_423),v_7=1,N_g(v_350,v_351,v_352,v_421),v_359=0,R_11_E_delta_v1v(from_r,to_r,id_l,from,id,to,label,to_l,v_7,id_r,from_l),v_422=\"E\",v_358=v_359,v_1=1,E_g(id,id_l,id_r,v_350,v_351,v_352,v_3,v_4,v_5,v_6),N_g(v_410,v_411,v_412,v_422),E_g(v_0,v_408,v_409,v_350,v_351,v_352,v_410,v_411,v_412,v_413),v_349=\"T\",E_g(v_414,v_415,v_416,v_410,v_411,v_412,v_417,v_418,v_419,v_420),v_413=\"X\",v_421=\"A\",v_2=2,v_420=\"X\",v_423=\"B\".\n" + 
////				"R_14a_MAP_v1v(v_359,v_349,v_0,v_358,v_2,v_1,v_4,label,v_3,v_351,v_6,v_352,v_5,v_350,v_7,from_r,to_r,id_l,from,id,to,to_l,id_r,from_l) <- E_g(v_0,v_424,v_425,v_426,v_427,v_428,v_350,v_351,v_352,v_429),v_7=1,v_429=\"X\",v_2=2,v_359=0,N_g(v_426,v_427,v_428,v_437),v_437=\"A\",v_438=\"E\",R_11_E_delta_v1v(from_r,to_r,id_l,from,id,to,label,to_l,v_7,id_r,from_l),N_g(v_350,v_351,v_352,v_438),v_358=v_359,E_g(id,id_l,id_r,v_350,v_351,v_352,v_3,v_4,v_5,v_6),v_1=1,v_436=\"X\",N_g(v_433,v_434,v_435,v_439),v_349=\"T\",E_g(v_430,v_431,v_432,v_350,v_351,v_352,v_433,v_434,v_435,v_436),v_439=\"B\".\n" + 
//				"";
//		System.out.println("before");
//		LogicBlox.runAddBlock("t", null, logic);
//		System.out.println("after");
////		System.out.println("Elapsed time3: " + Util.getLapTime());
//		LogicBlox.disconnect();
//		
		System.out.println("LogicBlox Stop");
	}

}

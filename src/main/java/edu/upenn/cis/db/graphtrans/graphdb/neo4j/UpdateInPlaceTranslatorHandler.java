package edu.upenn.cis.db.graphtrans.graphdb.neo4j;


public class UpdateInPlaceTranslatorHandler implements TranslatorHandler {
	
	private static final UpdateInPlaceTranslatorHandler instance = new UpdateInPlaceTranslatorHandler();

	private UpdateInPlaceTranslatorHandler() {
		
	}
	
	public static UpdateInPlaceTranslatorHandler getInstance() {
		return instance;
	}
	
	@Override
	public void test() {
		// TODO Auto-generated method stub
		System.out.println("update test");
	}

	@Override
	public void handleMatchClause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleConstructClause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleMapClause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleDeleteClause() {
		// TODO Auto-generated method stub
		
	}
}

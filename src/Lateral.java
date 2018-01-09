import java.util.ArrayList;

import simple_soccer_lib.PlayerCommander;
import simple_soccer_lib.perception.FieldPerception;
import simple_soccer_lib.perception.MatchPerception;
import simple_soccer_lib.perception.PlayerPerception;
import simple_soccer_lib.utils.EFieldSide;
import simple_soccer_lib.utils.EMatchState;
import simple_soccer_lib.utils.Vector2D;

public class Lateral extends Thread {
	private static final double ERROR_RADIUS = 1.0d;
	
	private enum State { ATTACKING, RETURN_TO_HOME, BLOCKING, WAITING, CORN_KICK };

	private PlayerCommander commander;
	private State state;
	
	private PlayerPerception selfInfo;
	private FieldPerception  fieldInfo;
	private MatchPerception  matchInfo;
	
	private Vector2D homebase; //posição base do jogador
	//private Vector2D attackHomeBase;
	
	private int[] numerosCamisa = {0,0,0,0,0,0};	
	private boolean flag;
	
	public Lateral(PlayerCommander player, double x, double y) {
		commander = player;
		homebase = new Vector2D(x, y);
		flag = true;
		//attackHomeBase = new Vector2D();
	}
	
	private void getCamisa(){
		flag = false;
		ArrayList<PlayerPerception> players = new ArrayList<PlayerPerception>();
		players.addAll(fieldInfo.getTeamPlayers(selfInfo.getSide()));
				
		for(PlayerPerception p:players){
			if(arrivedAtAt(new Vector2D(-52,0), p.getPosition())){				
				numerosCamisa[0] = p.getUniformNumber();			
				
			}else if(arrivedAtAt(new Vector2D(-25,20), p.getPosition())){				
				numerosCamisa[1] = p.getUniformNumber();
				
			}else if(arrivedAtAt(new Vector2D(-25,-20), p.getPosition())){				
				numerosCamisa[2] = p.getUniformNumber();			
				
			}else if(arrivedAtAt(new Vector2D(-10,0), p.getPosition())){				
				numerosCamisa[3] = p.getUniformNumber();		
				
			}else if(arrivedAtAt(new Vector2D(-5,28), p.getPosition())){				
				numerosCamisa[4] = p.getUniformNumber();
				
			}else if(arrivedAtAt(new Vector2D(-5,-28), p.getPosition())){				
				numerosCamisa[5] = p.getUniformNumber();		
				
			}else{
				System.out.println("Lateral ----- astofo"+ p.getUniformNumber());
			}
			
		}
	}
	
	/*private void getCamisa(){
		flag = false;
		ArrayList<PlayerPerception> players = new ArrayList<PlayerPerception>();
		players.addAll(fieldInfo.getTeamPlayers(selfInfo.getSide()));
				
		for(PlayerPerception p:players){
			if(arrivedAtAt(new Vector2D(-52,0), p.getPosition())){
				
				System.out.println(">>>>>>>>>porra1");
				numerosCamisa[0] = p.getUniformNumber();
				
			}else if(arrivedAtAt(new Vector2D(-25,20), p.getPosition())){
				
				numerosCamisa[1] = p.getUniformNumber();
				System.out.println(">>>>>>>>>porra2");
				
			}else if(arrivedAtAt(new Vector2D(-25,-20), p.getPosition())){
				
				numerosCamisa[2] = p.getUniformNumber();
				System.out.println(">>>>>>>>>porra3");
				
			}else if(arrivedAtAt(new Vector2D(-10,0), p.getPosition())){
				
				numerosCamisa[3] = p.getUniformNumber();
				System.out.println(">>>>>>>>>porra4");
				
			}else if(arrivedAtAt(new Vector2D(-5,28), p.getPosition())){
				
				numerosCamisa[4] = p.getUniformNumber();
				System.out.println(">>>>>>>>>porra5");
				
			}else if(arrivedAtAt(new Vector2D(-5,-28), p.getPosition())){
				
				numerosCamisa[5] = p.getUniformNumber();
				System.out.println(">>>>>>>>>porra6");
				
			}else{
				System.out.println("astofo");
			}
			//System.out.println(p.getPosition()+" ");
		}
		
//		for(int i=0;i<numerosCamisa.length;i++){
//			System.out.print(" "+numerosCamisa[i]);
//		}
	}*/
		
	private boolean arrivedAtAt(Vector2D targetPosition, Vector2D agentPosition) {
		//Vector2D myPos = selfInfo.getPosition();
		return Vector2D.distance(agentPosition, targetPosition) <= ERROR_RADIUS;
	}
	
	@Override
	public void run() {
		_printf("Waiting initial perceptions...");
		selfInfo  = commander.perceiveSelfBlocking();
		fieldInfo = commander.perceiveFieldBlocking();
		matchInfo = commander.perceiveMatchBlocking();
		
		state = State.RETURN_TO_HOME; //todos começam neste estado
		
		//commander.doMoveBlocking(Math.random() * (selfInfo.getSide() == EFieldSide.LEFT ? -52.0 : 52.0), (Math.random() * 68.0) - 34.0);
 
		if (selfInfo.getSide() == EFieldSide.RIGHT) { //ajusta a posição base de acordo com o lado do jogador (basta mudar o sinal do x)
			homebase.setX(- homebase.getX());
		}
		commander.doMoveBlocking(homebase.getX(), homebase.getY());
		
		try {
			Thread.sleep(5000); // espera, para dar tempo de ver as mensagens iniciais
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		while (commander.isActive()) {
			updatePerceptions();  //deixar aqui, no começo do loop, para ler o resultado do 'move'
			if(flag) getCamisa();
			
			//_printf(" "+state);
			
			if (matchInfo.getState() == EMatchState.PLAY_ON) {			
				switch (state) {
				case ATTACKING:
					stateAttacking();
					break;
				case RETURN_TO_HOME:
					stateReturnToHomeBase();
					break;
				case BLOCKING:
					stateBlocking();
					break;
				default:
					_printf("Invalid state: %s", state);
					break;	
				}			
			}else if(matchInfo.getState() == EMatchState.CORNER_KICK_LEFT || matchInfo.getState() == EMatchState.CORNER_KICK_RIGHT){
				//state = State.CORN_KICK;
				stateCornerKick();
				//return;
			}
		}			
	}
	
	private void updatePerceptions() {
		PlayerPerception newSelf = commander.perceiveSelf();
		FieldPerception newField = commander.perceiveField();
		MatchPerception newMatch = commander.perceiveMatch();
		
		// só atualiza os atributos se tiver nova percepção (senão, mantém as percepções antigas)
		if (newSelf != null) {
			this.selfInfo = newSelf;
		}
		if (newField != null) {
			this.fieldInfo = newField;
		}
		if (newMatch != null) {
			this.matchInfo = newMatch;
		}
	}
	
	/////// Estado CORNER_KICK ///////
	private void stateCornerKick(){
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		Vector2D attackPosition = fieldInfo.getTeamPlayer(selfInfo.getSide(), numerosCamisa[3]).getPosition();
		if(isMySide()){
			if (arrivedAt(ballPosition)) {
				commander.doKickToPoint(100, attackPosition);
				state = State.ATTACKING;			
			} else {
				if (isAlignedTo(ballPosition)) {
					//_printf("ATK: Running to the ball...");
					commander.doDashBlocking(100.0d);
				} else {
					//_printf("ATK: Turning...");
					turnTo(ballPosition);
				}
			}	
		}
	}
	
	/////// Estado Blocking ///////
	private void stateBlocking(){
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		//Vector2D playerPosition = selfInfo.getPosition();
		
		if(ballPosition.getX() < 10){
			state = State.RETURN_TO_HOME;
			//System.out.println(selfInfo.getUniformNumber()+">>>>>>"+"Blocking>>>>>>>>>>>>>>>Return to home;");
			return;
		}
		if(withMyTeam()){
			state = State.ATTACKING;
			//System.out.println(selfInfo.getUniformNumber()+">>>>>>"+"Blocking>>>>>>>>>>>>>>>Attacking");
			return;
		}
		
		if(isMySide()){
			if (arrivedAt(ballPosition)) {		
				commander.doKickToPointBlocking(100, new Vector2D(52,0));
				state = State.RETURN_TO_HOME;
				//System.out.println(selfInfo.getUniformNumber()+">>>>>>"+"Blocking>>>>>>>222222222>>>>>>>>Return to home");
				return;
			} else {
				if (isAlignedTo(ballPosition)) {
					//_printf("ATK: Running to the ball...");
					commander.doDashBlocking(100.0d);				
				} else {
					//_printf("ATK: Turning...");
					turnTo(ballPosition);
				}
			}		
		}
		
	}
	
	private boolean withMyTeam(){
		ArrayList<PlayerPerception> players = new ArrayList<PlayerPerception>();
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		//players = fieldInfo.getTeamPlayers(selfInfo.getSide()==EFieldSide.LEFT?EFieldSide.RIGHT:EFieldSide.LEFT);
		players = fieldInfo.getTeamPlayers(selfInfo.getSide());
		for(PlayerPerception p: players){
			if(arrivedAtAt(p.getPosition(), ballPosition)){
				////System.out.println("----------------------------------------->Out");
				return true;				
			}
		}
		return false;
	}
	
	private boolean isMySide(){ //verifica se a bola está na metade do campo que o jogador está
		if(((selfInfo.getPosition().getY() > 0)&&(fieldInfo.getBall().getPosition().getY() > 0))||
				((selfInfo.getPosition().getY() <= 0)&&(fieldInfo.getBall().getPosition().getY() <= 0))){
			return true;
		}
		return false;
	}

	/////// estado RETURN_TO_HOMEBASE ///////
	private void stateReturnToHomeBase() {
		if (withMyTeam()) {
			state = State.ATTACKING;
			//System.out.println(selfInfo.getUniformNumber()+">>>>>>"+"Return to home>>>>>>>>>>>>>>>Attacking");
			return;
		}
				
		if (! arrivedAt(homebase)) {			
			if (isAlignedTo(homebase)) {
				//_printf("RTHB: Running to the base...");
				commander.doDash(100.0d);			
			} else {
				//_printf("RTHB: Turning...");
				turnTo(homebase);
			}			
		}	
	}

//	private boolean closerToTheBall() {				
//		ArrayList<PlayerPerception> players = new ArrayList<PlayerPerception>();
//		int distanceIndex=0;
//		double auxA, auxB;
//		
//		players.addAll(fieldInfo.getTeamPlayers(selfInfo.getSide()));
//
//		for(PlayerPerception jogador : players){
//			if(jogador.getClass().equals(PlayerGoalkeeper.class)){
//				players.remove(jogador);
//			}
//		}
//		
//		Vector2D ballPosition = fieldInfo.getBall().getPosition();
//		auxA = pointDistance(players.get(0).getPosition(), ballPosition);
//		
//		for(int i=0;i<players.size();i++){
//			auxB = pointDistance(players.get(i).getPosition(), ballPosition);
//			if(auxA > auxB){
//				distanceIndex = i;
//			}
//		}
//		
//		distanceIndex++;
//		////System.out.println(">>>>>>>>>>"+distanceIndex);
//		return selfInfo.getUniformNumber() == distanceIndex;  
//	}
	
	private boolean arrivedAt(Vector2D targetPosition) {
		Vector2D myPos = selfInfo.getPosition();
		return Vector2D.distance(myPos, targetPosition) <= ERROR_RADIUS;
	}

	private void turnTo(Vector2D targetPosition) {
		Vector2D myPos = selfInfo.getPosition();
		////System.out.println(" => Target = " + targetPosition + " -- Player = " + myPos);
		
		Vector2D newDirection = targetPosition.sub(myPos);
		
		commander.doTurnToDirectionBlocking(newDirection);
	}
	
	private boolean isAlignedTo(Vector2D targetPosition) {
		Vector2D myPos = selfInfo.getPosition();
		if (targetPosition == null || myPos == null) {
			return false;			
		}
		double angle = selfInfo.getDirection().angleFrom(targetPosition.sub(myPos));
		return angle < 15.0d && angle > -15.0d;
	}
	
	/////// Estado ATTACKING ///////	
	
	private void stateAttacking() {
		if (!withMyTeam()) {
			state = State.BLOCKING;
			//System.out.println(selfInfo.getUniformNumber()+">>>>>>"+"Attacking>>>>>>>>>>>>>>>Blocking");
			////System.out.println(selfInfo.getUniformNumber()+">>>>>>>>>>>>>>>>>>>>>>>>>>>>blooooooooocccckkkkkkiiiiiiiinnnnnnnnggggg");
			return;
		}		

		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		Vector2D playerPosition = selfInfo.getPosition();
						
		if (arrivedAt(ballPosition)) {
			commander.doTurnToPoint(new Vector2D(52.0, 0.0));
			if(playerPosition.getX() >= 26){
				commander.doKick(100, 0);
				state = State.RETURN_TO_HOME;
				//System.out.println(selfInfo.getUniformNumber()+">>>>>>"+"Attacking>>>>>>>>>>>>>>>Return to home");
				return;
			}
			else{
				commander.doKickToPoint(100, new Vector2D(fieldInfo.getTeamPlayer(selfInfo.getSide(), numerosCamisa[3]).getPosition()));
				//state = State.RETURN_TO_HOME;
				////System.out.println(selfInfo.getUniformNumber()+">>>>>>"+ "Attacking>>>>>>>>>>>>>>>Return to home");
				//return;
			}
		}
		else{
			if(!isAlignedTo(ballPosition)){
				commander.doTurnToPointBlocking(ballPosition);
			}			
			commander.doDashBlocking(100);
		}			
	}
	
//	private boolean withPlayer(){
//		return true;
//	}

	//for debugging
	public void _printf(String format, Object...objects) {
		String teamPlayer = "";
		if (selfInfo != null) {
			teamPlayer += "[" + selfInfo.getTeam() + "/" + selfInfo.getUniformNumber() + "] ";
		}
		System.out.printf(teamPlayer + format + "%n", objects);
	}

}
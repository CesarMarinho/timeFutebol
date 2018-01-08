//import java.util.ArrayList;

import java.util.ArrayList;

import simple_soccer_lib.PlayerCommander;
import simple_soccer_lib.perception.FieldPerception;
import simple_soccer_lib.perception.MatchPerception;
import simple_soccer_lib.perception.PlayerPerception;
import simple_soccer_lib.utils.EFieldSide;
import simple_soccer_lib.utils.EMatchState;
import simple_soccer_lib.utils.Vector2D;

public class Atacante extends Thread{
private static final double ERROR_RADIUS = 1.0d;
	
	private enum State { ATTACKING, RETURN_TO_HOME, WAITING, KICKOFF};

	private PlayerCommander commander;
	private State state;
	
	private PlayerPerception selfInfo;
	private FieldPerception  fieldInfo;
	private MatchPerception  matchInfo;
	
	private Vector2D homebase; //posi��o base do jogador
	
	private int[] numerosCamisa = {0,0,0,0,0,0};
	private boolean flag;
	
	public Atacante(PlayerCommander player, double x, double y) {
		commander = player;
		homebase = new Vector2D(x, y);
		flag = true;
	}
	
	@Override
	public void run() {
		_printf("Waiting initial perceptions...");
		selfInfo  = commander.perceiveSelfBlocking();
		//System.out.println(">>>>>>>>>>>>>"+selfInfo.getUniformNumber());
		fieldInfo = commander.perceiveFieldBlocking();
		matchInfo = commander.perceiveMatchBlocking();
		
		state = State.RETURN_TO_HOME; //todos come�am neste estado
		
		
		//_printf("Starting in a random position...");
		//commander.doMoveBlocking(Math.random() * (selfInfo.getSide() == EFieldSide.LEFT ? -52.0 : 52.0), (Math.random() * 68.0) - 34.0);
 
		if (selfInfo.getSide() == EFieldSide.RIGHT) { //ajusta a posi��o base de acordo com o lado do jogador (basta mudar o sinal do x)
			homebase.setX(- homebase.getX());
		}
		
		commander.doMoveBlocking(homebase.getX(), homebase.getY());
		
		try {
			Thread.sleep(5000); // espera, para dar tempo de ver as mensagens iniciais
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		while (commander.isActive()) {
			updatePerceptions();  //deixar aqui, no come�o do loop, para ler o resultado do 'move'
			
			if(flag && matchInfo.getState() == EMatchState.PLAY_ON) getCamisa();
			
			if (matchInfo.getState() == EMatchState.PLAY_ON) {				
				switch (state) {
				case ATTACKING:
					stateAttacking();
					break;
				case RETURN_TO_HOME:
					stateReturnToHomeBase();
					break;
				case WAITING:
					stateWaiting();
					break;
				case KICKOFF:
					stateKickoff();
					break;
				default:
					_printf("Invalid state: %s", state);
					break;	
				}				
			}else if(matchInfo.getState() == EMatchState.KICK_OFF_LEFT){				
				//state = State.KICKOFF;
				stateKickoff();
				//return;
			}
		}
			
	}
	
	private void updatePerceptions() {
		PlayerPerception newSelf = commander.perceiveSelf();
		FieldPerception newField = commander.perceiveField();
		MatchPerception newMatch = commander.perceiveMatch();
		
		// s� atualiza os atributos se tiver nova percep��o (sen�o, mant�m as percep��es antigas)
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
				System.out.println("astofo");
			}
			
		}
	}
	
	private boolean arrivedAtAt(Vector2D targetPosition, Vector2D agentPosition) {
		//Vector2D myPos = selfInfo.getPosition();
		return Vector2D.distance(agentPosition, targetPosition) <= ERROR_RADIUS+3;
	}
	
	/////// Estado kickoff ///////
	private void stateKickoff(){
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		//Vector2D playerPosition = selfInfo.getPosition();
		//Vector2D lateralPosition
		//System.out.println("<><><><><><><><><><><><><><><><><><><><><><");
		Vector2D point = new Vector2D(52,0);
		
		if (arrivedAt(ballPosition)) {
			//commander.doKickToPoint(80, fieldInfo.getTeamPlayer(selfInfo.getSide(), numerosCamisa[4]).getPosition());
			commander.doKickToPoint(100, point);
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
	
	/////// Estado WAITING ///////
	private void stateWaiting(){
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		
		if(ballPosition.getX() >= 0){
			state = State.ATTACKING;
			return;
		}
		commander.doTurnToPoint(ballPosition);
	}

	////// Estado RETURN_TO_HOME_BASE ///////
	
	private void stateReturnToHomeBase() {
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		
		if (ballPosition.getX() > 0) {
			state = State.ATTACKING;
			return;
		}
		
		if (! arrivedAt(homebase)) {			
			if (isAlignedTo(homebase)) {
				//_printf("RTHB: Running to the base...");
				commander.doDashBlocking(100.0d);			
			} else {
				//_printf("RTHB: Turning...");
				turnTo(homebase);
			}			
		}
		else{
			state = State.WAITING;
			return;	
		}
			
	}

//	private boolean closerToTheBall() {
//				
//		ArrayList<PlayerPerception> players = new ArrayList<PlayerPerception>();
//		int distanceIndex=0;
//		double auxA, auxB;
//		
//		players.addAll(fieldInfo.getTeamPlayers(selfInfo.getSide()));
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
//		//System.out.println(">>>>>>>>>>"+distanceIndex);
//		return selfInfo.getUniformNumber() == distanceIndex;  
//	}
	
//	private double pointDistance(Vector2D player, Vector2D ball){
//		double termX = player.getX() - ball.getX();
//		double termY = player.getY() - ball.getY();
//		return Math.sqrt((termX*termX)+(termY*termY));
//	}
	
	private boolean arrivedAt(Vector2D targetPosition) {
		Vector2D myPos = selfInfo.getPosition();
		return Vector2D.distance(myPos, targetPosition) <= ERROR_RADIUS;
	}

	private void turnTo(Vector2D targetPosition) {
		Vector2D myPos = selfInfo.getPosition();
		//System.out.println(" => Target = " + targetPosition + " -- Player = " + myPos);
		
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
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		Vector2D playerPosition = selfInfo.getPosition();
		
		if (ballPosition.getX() < -15) {
			state = State.RETURN_TO_HOME;
			return;
		}		
		
		if (arrivedAt(ballPosition)) {
			commander.doTurnToPointBlocking(new Vector2D(52.0, 0.0));
			if(playerPosition.getX() >= 26){
				commander.doKickBlocking(100, 0);
			}
			
			commander.doKickBlocking(15, 0);
			
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

	//for debugging
	public void _printf(String format, Object...objects) {
		String teamPlayer = "";
		if (selfInfo != null) {
			teamPlayer += "[" + selfInfo.getTeam() + "/" + selfInfo.getUniformNumber() + "] ";
		}
		System.out.printf(teamPlayer + format + "%n", objects);
	}
}

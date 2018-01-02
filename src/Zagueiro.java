//import java.util.ArrayList;

import java.util.ArrayList;

import simple_soccer_lib.PlayerCommander;
import simple_soccer_lib.perception.FieldPerception;
import simple_soccer_lib.perception.MatchPerception;
import simple_soccer_lib.perception.PlayerPerception;
import simple_soccer_lib.utils.EFieldSide;
import simple_soccer_lib.utils.EMatchState;
import simple_soccer_lib.utils.Vector2D;

public class Zagueiro extends Thread{
private static final double ERROR_RADIUS = 2.0d;
	
	private enum State {RETURN_TO_HOME, BLOCKING, WAITING, ALINING, LATERAL_EXIT};

	private PlayerCommander commander;
	private State state;
	
	private PlayerPerception selfInfo;
	private FieldPerception  fieldInfo;
	private MatchPerception  matchInfo;
	
	private Vector2D homebase; //posição base do jogador
	
	private int[] numerosCamisa = {0,0,0,0,0,0};
	private boolean flag;
	
	public Zagueiro(PlayerCommander player, double x, double y) {
		commander = player;
		homebase = new Vector2D(x, y);
		flag = true;
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
		
//		for(int i=0;i<numerosCamisa.length;i++){
//			System.out.print(" "+numerosCamisa[i]);
//		}
	}
	
	@Override
	public void run() {
		_printf("Waiting initial perceptions...");
		selfInfo  = commander.perceiveSelfBlocking();
		fieldInfo = commander.perceiveFieldBlocking();
		matchInfo = commander.perceiveMatchBlocking();
		
		state = State.RETURN_TO_HOME; //todos começam neste estado
		
		//_printf(state+" ");
		
		//_printf("Starting in a random position...");
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
			//_printf(state.toString());
			//if(flag) getNumeroCamisa();
			if(flag && matchInfo.getState() == EMatchState.PLAY_ON) getCamisa();
			
			if (matchInfo.getState() == EMatchState.PLAY_ON) {
			
				switch (state) {
				case BLOCKING:
					stateBlocking();
					break;
				case RETURN_TO_HOME:
					stateReturnToHomeBase();
					break;
				case WAITING:
					stateWaiting();
					break;
				case ALINING:
					stateAlining();
					break;
				case LATERAL_EXIT:
					stateLateralExit();
					break;
				default:
					_printf("Invalid state: %s", state);
					break;	
				}				
			}else if((matchInfo.getState() == EMatchState.OFFSIDE_LEFT)||(matchInfo.getState() == EMatchState.OFFSIDE_RIGHT)){
				state = State.LATERAL_EXIT;
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
	
	/////// Estado lateralExit ///////
	private void stateLateralExit(){
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		
		if(ballPosition.getY() > -34 && ballPosition.getY() < 34){
			state = State.RETURN_TO_HOME;
		}
		
		if(isMySide()){
			if(!arrivedAt(ballPosition)){
				if(!isAlignedTo(ballPosition)){
					commander.doTurnToPoint(ballPosition);
				}else{
					commander.doDash(100);
				}				
			}else{
				commander.doKickToPoint(100, new Vector2D(0,0));
				state = State.RETURN_TO_HOME;
			}
		}
	}
	
	/////// Estado ALINING ///////
	private void stateAlining(){

		Vector2D playerPosition = selfInfo.getPosition();
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		if(!isMySide()){
			state = State.RETURN_TO_HOME;
			return;
		}
		
		if((playerPosition.getY() <= ballPosition.getY()+2)&&(playerPosition.getY()>= ballPosition.getY()-2)){
			state = State.WAITING;
			return;
		}else{
			if(ballPosition.getY() > playerPosition.getY()){
				if(!isAlignedTo(new Vector2D(playerPosition.getX(),34))){
					commander.doTurnToPoint(new Vector2D(playerPosition.getX(),34));
				}
			}else{
				if(!isAlignedTo(new Vector2D(playerPosition.getX(),-34))){
					commander.doTurnToPoint(new Vector2D(playerPosition.getX(),-34));
				}
			}
			commander.doDash(100);
		}
		
	}
	
	/////// Estado WAITING ///////
	private void stateWaiting(){
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		//Vector2D playerPosition = selfInfo.getPosition();
		///_printf("Entrou no waiting");
		if(ballPosition.getX() <= 0){
			state = State.BLOCKING;
			return;
		}else{
			if(isMySide()){
				state = State.ALINING;
				return;
			}else{
				state = State.RETURN_TO_HOME;
				return;
			}
		}
	}
	
	private boolean isMySide(){
		if(((selfInfo.getPosition().getY() > 0)&&(fieldInfo.getBall().getPosition().getY() > 0))||
				((selfInfo.getPosition().getY() <= 0)&&(fieldInfo.getBall().getPosition().getY() <= 0))){
			return true;
		}
		return false;
	}
	
	////// Estado RETURN_TO_HOME_BASE ///////
	
	private void stateReturnToHomeBase() {
		
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		
		if(ballPosition.getX() < 0){
			state = State.BLOCKING;
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
		}else{
			commander.doTurnToPoint(ballPosition);
			state = State.WAITING;
		}
		
	}

	private boolean arrivedAt(Vector2D targetPosition) {
		Vector2D myPos = selfInfo.getPosition();
		return Vector2D.distance(myPos, targetPosition) <= ERROR_RADIUS;
	}
	
	private boolean arrivedAtAt(Vector2D targetPosition, Vector2D agentPosition) {
		//Vector2D myPos = selfInfo.getPosition();
		return Vector2D.distance(agentPosition, targetPosition) <= ERROR_RADIUS;
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
	
	/////// Estado BLOCKING ///////	
	
	private void stateBlocking() {
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		if(ballPosition.getX() >= 0){
			state = State.RETURN_TO_HOME;
			return;
		}
		if(isMySide()){
			if (arrivedAt(ballPosition)) {			
				//commander.doKickToPointBlocking(100, new Vector2D(fieldInfo.getTeamPlayer(selfInfo.getSide(), 4).getPosition()));
				commander.doKickToPointBlocking(100, new Vector2D(fieldInfo.getTeamPlayer(selfInfo.getSide(), numerosCamisa[3]).getPosition()));
				state = State.RETURN_TO_HOME;
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

	//for debugging
	public void _printf(String format, Object...objects) {
		String teamPlayer = "";
		if (selfInfo != null) {
			teamPlayer += "[" + selfInfo.getTeam() + "/" + selfInfo.getUniformNumber() + "] ";
		}
		System.out.printf(teamPlayer + format + "%n", objects);
	}	
	
}
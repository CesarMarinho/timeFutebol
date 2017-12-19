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
	
	//private enum State { ATTACKING, RETURN_TO_HOME };
	private enum State {RETURN_TO_HOME, BLOCKING, WAITING, ALINING};

	private PlayerCommander commander;
	private State state;
	
	private PlayerPerception selfInfo;
	private FieldPerception  fieldInfo;
	private MatchPerception  matchInfo;
	
	private Vector2D homebase; //posi��o base do jogador
	
	private boolean flagAlign=true;
	
	public Zagueiro(PlayerCommander player, double x, double y) {
		commander = player;
		homebase = new Vector2D(x, y);
	}
	
	@Override
	public void run() {
		_printf("Waiting initial perceptions...");
		selfInfo  = commander.perceiveSelfBlocking();
		fieldInfo = commander.perceiveFieldBlocking();
		matchInfo = commander.perceiveMatchBlocking();
		
		state = State.RETURN_TO_HOME; //todos come�am neste estado
		
		_printf("Starting in a random position...");
		commander.doMoveBlocking(Math.random() * (selfInfo.getSide() == EFieldSide.LEFT ? -52.0 : 52.0), (Math.random() * 68.0) - 34.0);
 
		if (selfInfo.getSide() == EFieldSide.RIGHT) { //ajusta a posi��o base de acordo com o lado do jogador (basta mudar o sinal do x)
			homebase.setX(- homebase.getX());
		}
		
		try {
			Thread.sleep(5000); // espera, para dar tempo de ver as mensagens iniciais
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		while (commander.isActive()) {
			updatePerceptions();  //deixar aqui, no come�o do loop, para ler o resultado do 'move'
			
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
				default:
					_printf("Invalid state: %s", state);
					break;	
				}
				
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
	
	/////// Estado ALINING ///////
	private void stateAlining(){

		Vector2D playerPosition = selfInfo.getPosition();
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		if(!closerToTheBall()){
			state = State.RETURN_TO_HOME;
			return;
		}
		
		if((playerPosition.getY() <= ballPosition.getY()+2)&&(playerPosition.getY()>= ballPosition.getY()-2)){
			state = State.WAITING;
			return;
		}else{
			if(ballPosition.getY() > playerPosition.getY()){
				commander.doTurnToPoint(new Vector2D(playerPosition.getX(),34));
				flagAlign = false;
			}else{
				commander.doTurnToPoint(new Vector2D(playerPosition.getX(),-34));
				flagAlign = false;
			}
			commander.doDash(100);
		}
		
	}
	
	/////// Estado WAITING ///////
	private void stateWaiting(){
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		//Vector2D playerPosition = selfInfo.getPosition();
		_printf("Entrou no waiting");
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
		if((selfInfo.getPosition().getY() > 0)&&(fieldInfo.getBall().getPosition().getY()>0)){
			return true;
		}
		return false;
	}
	
	////// Estado RETURN_TO_HOME_BASE ///////
	
	private void stateReturnToHomeBase() {
		
		if (! arrivedAt(homebase)) {			
			if (isAlignedTo(homebase)) {
				_printf("RTHB: Running to the base...");
				commander.doDashBlocking(100.0d);		
			} else {
				_printf("RTHB: Turning...");
				turnTo(homebase);
			}			
		}else{
			state = State.WAITING;
		}
		
	}

	private boolean closerToTheBall() {
				
		ArrayList<PlayerPerception> players = new ArrayList<PlayerPerception>();
		int distanceIndex=0;
		double auxA, auxB;
		
		players.addAll(fieldInfo.getTeamPlayers(selfInfo.getSide()));
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		auxA = pointDistance(players.get(0).getPosition(), ballPosition);
		
		for(int i=0;i<players.size();i++){
			auxB = pointDistance(players.get(i).getPosition(), ballPosition);
			if(auxA > auxB){
				distanceIndex = i;
			}
		}
		
		distanceIndex++;
		//System.out.println(">>>>>>>>>>"+distanceIndex);
		return selfInfo.getUniformNumber() == distanceIndex;  
	}
	
	private double pointDistance(Vector2D player, Vector2D ball){
		double termX = player.getX() - ball.getX();
		double termY = player.getY() - ball.getY();
		return Math.sqrt((termX*termX)+(termY*termY));
	}
	
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
	
	private void stateBlocking() {
		if (! closerToTheBall()) {
			state = State.RETURN_TO_HOME;
			return;
		}

		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		
		if (arrivedAt(ballPosition)) {
			//commander.doKick(100.0d, 0);
			//commander.doTurnToPoint(new Vector2D(52.0, 0.0));
			commander.doKickToPoint(100, new Vector2D(52.0, 0.0));
			//TODO: chutar em dire��o ao gol advers�rio
			
		} else {
			if (isAlignedTo(ballPosition)) {
				_printf("ATK: Running to the ball...");
				commander.doDashBlocking(100.0d);
			} else {
				_printf("ATK: Turning...");
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

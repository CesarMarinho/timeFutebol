import java.util.ArrayList;

import simple_soccer_lib.PlayerCommander;
import simple_soccer_lib.perception.FieldPerception;
import simple_soccer_lib.perception.MatchPerception;
import simple_soccer_lib.perception.PlayerPerception;
import simple_soccer_lib.utils.EFieldSide;
import simple_soccer_lib.utils.EMatchState;
import simple_soccer_lib.utils.Vector2D;

public class Atacante extends Thread{
private static final double ERROR_RADIUS = 2.0d;
	
	private enum State { ATTACKING, RETURN_TO_HOME };

	private PlayerCommander commander;
	private State state;
	
	private PlayerPerception selfInfo;
	private FieldPerception  fieldInfo;
	private MatchPerception  matchInfo;
	
	private Vector2D homebase; //posição base do jogador
	
	public Atacante(PlayerCommander player, double x, double y) {
		commander = player;
		homebase = new Vector2D(x, y);
	}
	
	@Override
	public void run() {
		_printf("Waiting initial perceptions...");
		selfInfo  = commander.perceiveSelfBlocking();
		fieldInfo = commander.perceiveFieldBlocking();
		matchInfo = commander.perceiveMatchBlocking();
		
		state = State.RETURN_TO_HOME; //todos começam neste estado
		
		_printf("Starting in a random position...");
		commander.doMoveBlocking(Math.random() * (selfInfo.getSide() == EFieldSide.LEFT ? -52.0 : 52.0), (Math.random() * 68.0) - 34.0);
 
		if (selfInfo.getSide() == EFieldSide.RIGHT) { //ajusta a posição base de acordo com o lado do jogador (basta mudar o sinal do x)
			homebase.setX(- homebase.getX());
		}
		
		try {
			Thread.sleep(5000); // espera, para dar tempo de ver as mensagens iniciais
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		while (commander.isActive()) {
			updatePerceptions();  //deixar aqui, no começo do loop, para ler o resultado do 'move'
			
			if (matchInfo.getState() == EMatchState.PLAY_ON) {
			
				switch (state) {
				case ATTACKING:
					stateAttacking();
					break;
				case RETURN_TO_HOME:
					stateReturnToHomeBase();
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

	////// Estado RETURN_TO_HOME_BASE ///////
	
	private void stateReturnToHomeBase() {
		if (closerToTheBall()) {
			state = State.ATTACKING;
			return;
		}
		
		if (! arrivedAt(homebase)) {			
			if (isAlignedTo(homebase)) {
				_printf("RTHB: Running to the base...");
				commander.doDashBlocking(100.0d);			
			} else {
				_printf("RTHB: Turning...");
				turnTo(homebase);
			}			
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
	
	private void stateAttacking() {
		if (! closerToTheBall()) {
			state = State.RETURN_TO_HOME;
			return;
		}

		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		
		if (arrivedAt(ballPosition)) {
			//commander.doKick(100.0d, 0);
			//commander.doTurnToPoint(new Vector2D(52.0, 0.0));
			commander.doKickToPoint(100, new Vector2D(52.0, 0.0));
			//TODO: chutar em direção ao gol adversário
			
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

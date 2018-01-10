
import java.util.ArrayList;


import simple_soccer_lib.PlayerCommander;
import simple_soccer_lib.perception.FieldPerception;
import simple_soccer_lib.perception.MatchPerception;
import simple_soccer_lib.perception.PlayerPerception;
import simple_soccer_lib.utils.EFieldSide;
import simple_soccer_lib.utils.EMatchState;
import simple_soccer_lib.utils.Vector2D;

public class PlayerGoalkeeper extends Thread{
	
	private static final double ERROR_RADIUS = 2.0d;
	
	private enum State {RETURN_TO_HOME, BLOCKING};

	private PlayerCommander commander;
	private State state;
	
	private PlayerPerception selfInfo;
	private FieldPerception  fieldInfo;
	private MatchPerception  matchInfo;
	
	private Vector2D homebase; //centro do gol

	//variaveis para trocar de lado
	private int xBackLimitarMovimento = -52;
	private int xFrontLimitarMovimento = -36;
	
	public PlayerGoalkeeper(PlayerCommander player) {
		commander = player;
		homebase = new Vector2D(-52.0d, 0.0d);
	}
	
	@Override
	public void run() {
		_printf("Waiting initial perceptions...");
		selfInfo  = commander.perceiveSelfBlocking();
		fieldInfo = commander.perceiveFieldBlocking();
		matchInfo = commander.perceiveMatchBlocking();		
		
		state = State.RETURN_TO_HOME; //todos começam neste estado
		if (selfInfo.getSide() == EFieldSide.RIGHT) { //ajusta a posição base de acordo com o lado do jogador (basta mudar o sinal do x)
			swapSides();
		}		
		commander.doMoveBlocking(homebase.getX(), homebase.getY());
		commander.doTurnToPoint(new Vector2D(0,0));
		
		try {
			Thread.sleep(5000); // espera, para dar tempo de ver as mensagens iniciais
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		while (commander.isActive()) {
			updatePerceptions();  //deixar aqui, no começo do loop, para ler o resultado do 'move'
			
			if (matchInfo.getState() == EMatchState.PLAY_ON) {
			
				switch (state) {
				case RETURN_TO_HOME :
					stateRETURN_TO_HOME();
					break;
				case BLOCKING :
					stateBLOCKING();
					break;
				default:
					_printf("Invalid state: %s", state);
					break;	
				}				
			}
		}
	}
	
	private void swapSides(){
		homebase.setX(homebase.getX()*(-1));
		xBackLimitarMovimento = xBackLimitarMovimento*(-1);
		xFrontLimitarMovimento = xFrontLimitarMovimento*(-1);
	}
	
	private void stateRETURN_TO_HOME() {
		// TODO Auto-generated method stub
		Vector2D ball = fieldInfo.getBall().getPosition();

		if(closerToTheBall() && limitarMovimento()){
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
		}
		
		commander.doTurnToPoint(ball);
		
	}

	private void stateBLOCKING() {
		// TODO Auto-generated method stub
		
		Vector2D ball = fieldInfo.getBall().getPosition();
	
		if(!limitarMovimento()){
			state = State.RETURN_TO_HOME;
			return;
		}
		
		if(!isAlignedTo(ball)){
			commander.doTurnToPoint(ball);
		}
		
		commander.doDash(70.0d);
		if(arrivedAt(ball)){
			commander.doKickToDirection(100.0d, new Vector2D(0,0));
		}
	}
		

	private boolean arrivedAt(Vector2D targetPosition) {
		Vector2D myPos = selfInfo.getPosition();
		return Vector2D.distance(myPos, targetPosition) <= ERROR_RADIUS;
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

	
	private boolean closerToTheBall() {
		
		ArrayList<PlayerPerception> players = new ArrayList<PlayerPerception>();
		int distanceIndex=0;
		double auxA, auxB;
		
		players.addAll(fieldInfo.getAllPlayers());
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
	
	public boolean limitarMovimento(){
		Vector2D ballPos = fieldInfo.getBall().getPosition();
		Double x = ballPos.getX();
		Double y = ballPos.getY();
		if(x > xBackLimitarMovimento && x < xFrontLimitarMovimento && y > -20 && y < 20){
			return true;
		}
		else{
			return false;
		}
	}
	
	private void turnTo(Vector2D targetPosition) {
		Vector2D myPos = selfInfo.getPosition();		
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
	private double pointDistance(Vector2D player, Vector2D ball){
		double termX = player.getX() - ball.getX();
		double termY = player.getY() - ball.getY();
		return Math.sqrt((termX*termX)+(termY*termY));
	}
	public void _printf(String format, Object...objects) {
		String teamPlayer = "";
		if (selfInfo != null) {
			teamPlayer += "[" + selfInfo.getTeam() + "/" + selfInfo.getUniformNumber() + "] ";
		}
		System.out.printf(teamPlayer + format + "%n", objects);
	}
}

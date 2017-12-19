

import java.util.ArrayList;

import simple_soccer_lib.PlayerCommander;
import simple_soccer_lib.perception.FieldPerception;
import simple_soccer_lib.perception.MatchPerception;
import simple_soccer_lib.perception.PlayerPerception;
import simple_soccer_lib.utils.EMatchState;
import simple_soccer_lib.utils.Vector2D;

public class PlayerGoalkeeper extends Thread{
	
	private static final double ERROR_RADIUS = 2.0d;
	
	private enum State { RECEIVED_PASSING, WAITING, RETURN_TO_HOME, ATTACKING };

	private PlayerCommander commander;
	private State state;
	
	private PlayerPerception selfInfo;
	private FieldPerception  fieldInfo;
	private MatchPerception  matchInfo;
	
	private Vector2D homebase; //centro do gol
	private Vector2D home_left; //mais a esquerda
	private Vector2D home_right; //mais a direita
	private Vector2D max_attack; //posição mais adiantada do goleiro
	
	public PlayerGoalkeeper(PlayerCommander player) {
		commander = player;
		homebase = new Vector2D(-52.4d, 0.0d);
		selfInfo.setGoalie(true);
	}
	
	@Override
	public void run() {
		_printf("Waiting initial perceptions...");
		selfInfo  = commander.perceiveSelfBlocking();
		fieldInfo = commander.perceiveFieldBlocking();
		matchInfo = commander.perceiveMatchBlocking();
		
		state = State.RETURN_TO_HOME; //todos começam neste estado
		try {
			Thread.sleep(5000); // espera, para dar tempo de ver as mensagens iniciais
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		while (commander.isActive()) {
			updatePerceptions();  //deixar aqui, no começo do loop, para ler o resultado do 'move'
			
			if (matchInfo.getState() == EMatchState.PLAY_ON) {
			
				switch (state) {
				case RECEIVED_PASSING:
					stateRECEIVED_PASSING();
					break;
				case RETURN_TO_HOME:
					stateRETURN_TO_HOME();
					break;
				case WAITING:
					stateWAITING();
					break;
				case ATTACKING:
					stateATTACKING();
					break;
				default:
					_printf("Invalid state: %s", state);
					break;	
				}
				
			}
		}
	}
	
	private void stateATTACKING() {
		// TODO Auto-generated method stub
		if(fieldInfo.getBall().getPosition().getX() < 0){
			state = State.RETURN_TO_HOME;
		}
	}

	private void stateWAITING() {
		// TODO Auto-generated method stub
		if(fieldInfo.getBall().getPosition().getY() > 14){
			
		}
	}

	private void stateRETURN_TO_HOME() {
		// TODO Auto-generated method stub
		
	}

	private void stateRECEIVED_PASSING() {
		// TODO Auto-generated method stub
		
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


import simple_soccer_lib.AbstractTeam;
import simple_soccer_lib.PlayerCommander;

public class Team extends AbstractTeam {

	public Team(String suffix) {
		super("P" + suffix, 4);
	}

	@Override
	protected void launchPlayer(int ag, PlayerCommander commander) {
		//double targetX, targetY;
		
		if(ag==0){			
			//Zagueiro p = new Zagueiro(commander, -25, 20);
			PlayerGoalkeeper p = new PlayerGoalkeeper(commander);
			p.start();
		}else if(ag==1){			
			Zagueiro p = new Zagueiro(commander, -25, -20);
			p.start();
		}else if(ag==2){			
			//Atacante a = new Atacante(commander, 10, 0);
			Zagueiro p = new Zagueiro(commander, -25, 20);
			p.start();
		}else if(ag==3){
			Atacante a = new Atacante(commander, 10, 0);
			a.start();
		}
		
		
		
//		Player pl = new Player(commander, targetX, targetY);
//		//Defender pl = new Defender(commander, targetX, targetY);
//		pl.start();
	}


}

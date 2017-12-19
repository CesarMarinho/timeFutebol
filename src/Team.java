
import simple_soccer_lib.AbstractTeam;
import simple_soccer_lib.PlayerCommander;

public class Team extends AbstractTeam {

	public Team(String suffix) {
		super("P" + suffix, 2);
	}

	@Override
	protected void launchPlayer(int ag, PlayerCommander commander) {
		//double targetX, targetY;
		
		if(ag==0){
			Zagueiro p = new Zagueiro(commander, -25, 20);
			p.start();
		}else if(ag==1){
			Zagueiro p = new Zagueiro(commander, -25, -20);
			p.start();
		}
		
		
		
//		Player pl = new Player(commander, targetX, targetY);
//		//Defender pl = new Defender(commander, targetX, targetY);
//		pl.start();
	}


}

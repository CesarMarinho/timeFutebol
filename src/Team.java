
import simple_soccer_lib.AbstractTeam;
import simple_soccer_lib.PlayerCommander;

public class Team extends AbstractTeam {

	public Team(String suffix) {
		super("Ex2" + suffix, 2);
	}

	@Override
	protected void launchPlayer(int ag, PlayerCommander commander) {
		double targetX, targetY;
		
		targetX = -53d / 2;
		
//		if (ag == 0) {
//			targetY = 34.0d / 2; //posição que aparece mais baixa no monitor
//			Defender pl = new Defender(commander, targetX, targetY);
//			pl.start();
//		} else if(ag == 1){
//			targetY = -34.0d / 2;  //posição mais alta
//			Exercise2Player pl = new Exercise2Player(commander, targetX, targetY);
//			pl.start();
//		}else if(ag == 2){
//			targetY = -20.0d / 2;
//			Defender plb = new Defender(commander, targetX, targetY);
//			plb.start();
//		}else if(ag == 3){
//			targetY = 20.0d / 2;	
//			Exercise2Player plc = new Exercise2Player(commander, targetX, targetY);
//			plc.start();
//		}else{
//			targetY = 1.0d;
//		}
		if(ag%2 == 0)			targetY = 34.0d/2;
		else if(ag%2 == 1)	targetY = -34.0d/2;
		else				targetY = 0d;
		
		
		
		Player pl = new Player(commander, targetX, targetY);
		//Defender pl = new Defender(commander, targetX, targetY);
		pl.start();
	}


}

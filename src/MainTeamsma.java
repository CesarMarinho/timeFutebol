
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import simple_soccer_lib.team.A1.A1;

public class MainTeamsma {
	
	public static void main(String[] args) throws IOException {
		Team team1 = new Team("a");
		//Exercise2Team team2 = new Exercise2Team("b");
		//A1 time = new A1(null,0,null);
		
		team1.launchTeamAndServer();
		//time.run();
		//team2.launchTeam();
	}
	
}

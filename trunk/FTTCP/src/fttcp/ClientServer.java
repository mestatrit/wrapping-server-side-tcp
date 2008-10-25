
public class ClientServer {
  private static Client client; 
  private static Server server;
  	public ClientServer() {	}
  public static void main(String[] args) {
   Client c = new Client();
     c.start();
   Server s = new Server();
     s.start();
  }
  
   static class Client extends Thread
   	{
 public void run(){

do
  	for (int i = 0; i<20; i++)
  	{
  	sendPacket(i);
  	System.out.println("client sending" + i);
  	}
while (Serverreplied = true);
 }
   	}
  
   static class Server extends Thread
   	{
  public  void run(){
  	  	send(readPacket());
  	 System.out.println("server sending" + readPacket());
  	  	}
  }
   	}
}


 

   



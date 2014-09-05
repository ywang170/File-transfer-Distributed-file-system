package mp3;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Sendclass {

  public static int SOCKET_PORT;  // you may change this
  public static String FILE_TO_SEND;  // you may change this
  
  public Sendclass(int port, String file)
  {
	  SOCKET_PORT = port;
	  System.out.println(port);
	  FILE_TO_SEND = file;
  }

  public static void start () throws IOException {
    FileInputStream fis = null;
    BufferedInputStream bis = null;
    OutputStream os = null;
    ServerSocket servsock = null;
    Socket sock = null;
    
      servsock = new ServerSocket(SOCKET_PORT);
      
        System.out.println("Waiting...");
       
          sock = servsock.accept();
          System.out.println("Accepted connection : " + sock);
          // send file
          File myFile = new File (FILE_TO_SEND);
          byte [] mybytearray  = new byte [(int)myFile.length()];
          fis = new FileInputStream(myFile);
          bis = new BufferedInputStream(fis);
          bis.read(mybytearray,0,mybytearray.length);
          os = sock.getOutputStream();
          System.out.println("Sending " + FILE_TO_SEND + "(" + mybytearray.length + " bytes)");
          os.write(mybytearray,0,mybytearray.length);
          os.flush();
          System.out.println("Done.");
          System.out.println("______________________________________________" + "\n" + "Please give your order: ");
        
          if (bis != null) bis.close();
          if (os != null) os.close();
          if (sock!=null) sock.close();
        
      
    
    
      if (servsock != null) servsock.close();
    
  }
}
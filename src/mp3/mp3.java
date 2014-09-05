package mp3;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.swing.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.awt.*;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

//every server will send just his own heart beats to all servers he know
//as long as some server doesn't receive from a server for some time it will set it as not activated
//if he received messages from a new server, it will add him to his list
//if it received message of the not activated one, it will revive the dead server
//when a server first connect to the network it will send information to threshold and threshold will send him information about every other server
//all message are in form of "port_number heart_beats"

public class mp3 {
	
	//declare socket to send and receive message
 static DatagramSocket sendSock;
 static DatagramSocket recvSock;
 static DatagramSocket tempSock;
 static int myPort;//the port number that every one will used to recieve
 static int wholePort;//port number of threshold
 static InetAddress thresholdIP;//the IP of the threshold，门番!
 static int myheartBeats = 1000;
 static a_server[] servers;
 static Thread listener;
 static Thread checker;
 static Thread door;//threshold only
 static Thread watcher;
 static int maxServerNum = 5;
 static int serversWeHave = 0;
 static int connectionSteps = 0;
 static boolean holder = false;
 static a_server waitOne;
 static int sendNum = 1;//the number of information send
 static long myPreTime = 0;
 static int serve_welcome = 0;
 static Logger logger1;
 
 static a_server Leader = null;//this will be null for the leader himself
 static int myNum = 0;
 static String[] myContent;
 static boolean busy = false;//if the server is busy
 static int permit = -1;//permission from leader, it also becomes the number of which server you want to connect when leader reply
 //static byte[] bigData;
 //static int count;
 //static int bytesReceived;
 //static DatagramPacket recvPack;
 //static DatagramPacket sendPack;
 static boolean sendingData = false;
 //static boolean recvingData = false;
 static int MAX_LENGTH = -1;
 static int DataSendSock;
 static int DataRecvSock;
 
	//used to record the information of its peers...
	public static class a_server{
		int port;//the port number
		InetAddress IPAd;//IP address
		long preTime;//last time it is renewed
		int heartBeats;//heartbeat number right now
		boolean alive;//does it still alive
		boolean isServer;
		int num;
		boolean isLeader;
		String[] contents;
		
		public a_server()
		{
			port = 0;
			IPAd = null;
			preTime = 0;
			heartBeats = 1000;
			alive = false;
			isServer = false;
			num = 999;
			isLeader = false;
			contents = new String[32];
			for(int i = 0; i < 32 ; i ++)
			{
				contents[i] = null;
			}
		}
	}
	
	public static void main(String argv[]) throws IOException
	{
		//log test
		{
			PropertyConfigurator.configure("log4j.properties");
			   
	        logger1 = Logger.getLogger(mp3.class);

	        logger1.setLevel(Level.DEBUG);
	     
	        
		}
		//getting started
		System.out.println("Welcome to my network");
		servers = new a_server[maxServerNum];
		//declare place for servers
		for(int i = 0; i < maxServerNum; i++)
		{
			servers[i] = new a_server();
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Please type in your port number:");//type in your port number
		String s = in.readLine();
		myPort = Integer.parseInt(s);
		System.out.println("Are you the thresholder? y/n");//check if you will be the holder of the whole system
		s = in.readLine();
		if(s.equals("y"))
			holder = true;
		if(!holder)//if you are not holder then you should input the information of holder
		{
			System.out.println("please type in your machine number:");//holder's ip
			s = in.readLine();
			myNum = Integer.parseInt(s);
			System.out.println("please type in threshold's IP address:");//holder's ip
			s = in.readLine();
			thresholdIP = InetAddress.getByName(s);
			System.out.println("Please type in threshold's port number:");//holder's port
			s = in.readLine();
			wholePort = Integer.parseInt(s);
			
		}

		waitOne = new a_server();
		recvSock = new DatagramSocket(myPort);//declare receive sock
		sendSock = new DatagramSocket();//declare send sock
		//tempSock = new DatagramSocket(1994 + myNum);
		DataSendSock = 1994 + myNum;
		DataRecvSock = 1800 + myNum;
		
		Runtime r = Runtime.getRuntime();
		r.exec("mkdir sdfs");	//make a new dir for files
		
		
		if(!holder)
		//connect to the threshold and get information of others by: first send message to threshold so it will add this server as a member send information back
		getIntoNetwork();
		
		//this is used to listen to informations
		listener = new Thread(new listen());
		listener.start();
		
		//start to check heart beat
		checker = null;
		//then get to work
		for(int i = 0; i < maxServerNum; i++)
		{
			if(servers[i].IPAd == null)
				continue;
			//servers[i].preTime = new Date().getTime();
			servers[i].alive = true;
		}
		
		
		
		while(true)
		{
			
			
			if(new Date().getTime() - myPreTime <= 1000)
				continue;
			myPreTime = new Date().getTime();
			myheartBeats++;
			String message = Integer.toString(myPort) + " " + Integer.toString(myheartBeats) + " " + myNum + " dsdad";
			byte[] message_b  = message.getBytes();//the message we want to send
			for(int i = 0; i < maxServerNum; i++)
			{
				if(servers[i].alive || servers[i].isServer)
				{
					DatagramPacket sendData = new DatagramPacket(message_b, 0, message_b.length, servers[i].IPAd, servers[i].port);
					sendSock.send(sendData);
				}
			}
			if(checker == null)//start to check after build connection with others
				{checker = new Thread(new check());
				checker.start();
				Thread worker = new Thread(new work());
				worker.start();
				}
		}
	}
	
	public static class work implements Runnable
	{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				updateToLeader();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			while(true)
			{
				System.out.println("______________________________________________" + "\n" + "Please give your order: ");
				String message = null;
				try {
					message = in.readLine();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(busy)
					{
						System.out.println("system busy!");
						continue;
					}
				String[] messages = message.split(" ");
				if(messages[0].equals("put"))
				{
					
					try {
							if(askingPermit(message))
							{
								
								String command = "cp " + messages[1] + " sdfs/" + messages[2];
								
								Runtime r = Runtime.getRuntime();
								try {
									r.exec(command);
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								continue;
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					continue;
				}
				if(messages[0].equals("delete"))
				{
					try {
						askingPermit(message);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
				}
				if(messages[0].equals("get"))
				{
					try {
						askingPermit(message);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						transferFile(permit, messages[1], messages[2]);
					} catch (SocketException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
				}
				System.out.println("Invalid command!");
			}
		}
		
	}
	
	public static void transferFile(int machineNum, String fileName, String newName) throws SocketException, FileNotFoundException, InterruptedException
	{
		if(machineNum >= 0)
		{System.out.println("Connecting with machine " + machineNum + " !");
		//return;
		}
		else
		{
			if(machineNum == -9)
				System.out.println("That server is busy!");
			else
			System.out.println("No such file found!");
			return;
		}
		if(machineNum == myNum)
		{
			String command = "cp " + "sdfs/" + fileName  + " " + newName;
			
			Runtime r = Runtime.getRuntime();
			try {
				r.exec(command);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			logger1.info("Getting a new file " + fileName + " named as " + newName);
			return;
		}
		else
		{
			MAX_LENGTH = -1;
			for(int i = 0; i < maxServerNum; i++)
			{
				if(servers[i].alive && servers[i].num == machineNum)
				{
					Runtime r = Runtime.getRuntime();
					Process p = null;
					try {
						p = r.exec("pwd");
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					String myPath = null;
					BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
					try {
						myPath = stdInput.readLine();
						//System.out.println(myPath.toString());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					myPath =  myPath+ "/" + newName;
					String message = "connection " + fileName +" " + DataRecvSock + " " + myNum + " end";
					byte[] message_b  = message.getBytes();//the message we want to send
					DatagramPacket sendData = new DatagramPacket(message_b, 0, message_b.length, servers[i].IPAd, servers[i].port);
					try {
						sendSock.send(sendData);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					long startTime = new Date().getTime();
					while(MAX_LENGTH == -1)
					{
						if((new Date().getTime() - startTime) > 5000)
						{
							System.out.println("The other server is busy or transaction error...please try again!");
							return;
						}
						System.out.println("loading... " + MAX_LENGTH);
					}
					Thread.sleep(500);
					Recvclass rr = new Recvclass(DataRecvSock, servers[i].IPAd.toString(), myPath );
					try {
						rr.start();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					logger1.info("Getting a new file " + fileName + " named as " + newName);
					MAX_LENGTH = -1;
					return;
				}
			}
			
			/*permit = -1;
			bytesReceived = -2;
			bigData = new byte[10000];
			recvPack = new DatagramPacket(bigData, 0, 10000);
			Thread recver = new Thread(new recvDataThread());
			recver.start();
			
			for(int i = 0; i < maxServerNum; i++)
			{
				if(servers[i].alive && servers[i].num == machineNum)
				{
					String message = "connection " + fileName +" " + tempSockNum + " " + myNum + " end";
					byte[] message_b  = message.getBytes();//the message we want to send
					DatagramPacket sendData = new DatagramPacket(message_b, 0, message_b.length, servers[i].IPAd, servers[i].port);
					try {
						sendSock.send(sendData);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					while(MAX_LENGTH < 0)///////////////////////++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
					{
						System.out.println("loading... " + MAX_LENGTH);
					}
					
					count = 0;
					bytesReceived = 0;
					
					break;
				}
			}
			long startTime = new Date().getTime();
			while(permit == -1)
			{
				if((new Date().getTime()) - startTime > 5000)
				{
					logger1.info("Leader is down or transaction problems happen! Please try again later");
					MAX_LENGTH = -1;
					return;
				}
				System.out.println("loading... " + permit);////////////////////////////////////////////////////
			}
			Runtime r = Runtime.getRuntime();
			Process p = null;
			try {
				p = r.exec("pwd");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			String myPath = null;
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			try {
				myPath = stdInput.readLine();
				//System.out.println(myPath.toString());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Path path = Paths.get(myPath+"/" + newName);

	        try {
				Files.createDirectories(path.getParent());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

	            try {
					Files.createFile(path);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			FileOutputStream output = new FileOutputStream(new File(newName));
			try {
				output.write(recvPack.getData());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			MAX_LENGTH = -1;
			logger1.info("Getting a new file " + fileName + " named as " + newName);*/
		}
		
	}
	
	
	public static boolean askingPermit(String message) throws IOException
	{
		permit = -1;
		String[] messages = message.split(" ");
		if(messages[0].equals("put"))
		{
			if(Leader == null)
				{
				
					for(int i = 0; i < 32; i++)
					{
						if(myContent[i] == null)
						{
							myContent[i] = messages[2];
							break;
						}
					}
					logger1.info("adding a new file " + messages[2]);
					return true;
				
				}
			message = "report " + message + " " + myNum+ " end";
			byte[] message_b  = message.getBytes();//the message we want to send
			DatagramPacket sendData = new DatagramPacket(message_b, 0, message_b.length, Leader.IPAd, Leader.port);
			sendSock.send(sendData);
			long startTime =new Date().getTime();
			while(permit == -1)
			{
				if((new Date().getTime()) - startTime > 5000)
				{
					logger1.info("Leader is down or transaction problems happen! Please try again later");
					//reElectLeader();
					return false;
				}
				System.out.println("loading... " + permit);///////////////////////////////////////////////////////
				
			}
			logger1.info("adding a new file " + messages[2]);
			return true;
		}
		if(messages[0].equals("delete"))
		{
			if(Leader == null)
			{
				for(int i = 0; i < 32; i++)
				{
					if(myContent[i] != null && myContent[i].equals(messages[1]))
					{
						String command = "rm sdfs/" + messages[1];//delete the file
						Runtime r = Runtime.getRuntime();
						try {
							r.exec(command);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						logger1.info("delete a file " + messages[1]);
						myContent[i] = null;
						return true;
					}
				}
				for(int i = 0; i < maxServerNum; i++)
				{
					if(servers[i].alive)
					{
						for(int j = 0; j < 32; j++)
						{
							if(servers[i].contents[j] != null && servers[i].contents[j].equals(messages[1]))
							{
				
								permit = -1;
								String delete_request = "delete " + messages[1] +" end";
								byte[] delete_b = delete_request.getBytes();
								DatagramPacket sendData = new DatagramPacket(delete_b, 0, delete_b.length, servers[i].IPAd, servers[i].port);
								sendSock.send(sendData);
								long startTime =new Date().getTime();
								while(permit == -1)
								{
									if((new Date().getTime()) - startTime > 5000)
									{
										logger1.info("Leader is down or transaction problems happen! Please try again later");
										return false;
									}
									System.out.println("loading... " + permit);////////////////////////////////////////////////////
								}
								logger1.info("delete a file " + messages[1]);
								servers[i].contents[j] = null;
								return true;
							}
						}
					}
				}
				System.out.println("No such file found!");
				return false;
			}
			//if Im not a leader
			permit = -1;
			message = "report " + message + " " + myNum+ " end";
			byte[] message_b  = message.getBytes();//the message we want to send
			DatagramPacket sendData = new DatagramPacket(message_b, 0, message_b.length, Leader.IPAd, Leader.port);
			sendSock.send(sendData);
			long startTime =new Date().getTime();
			while(permit == -1)
			{
				if((new Date().getTime()) - startTime > 5000)
				{
					logger1.info("Leader is down or transaction problems happen! Please try again later");
					//reElectLeader();
					return false;
				}
				System.out.println("loading... " +permit);/////////////////////////////////////////////
			}
			if(permit == 0)
			logger1.info("delete a file " + messages[1]);
			else
			System.out.println("No such file found!");
			return true;
		}
		if(messages[0].equals("get"))
		{
			if(Leader == null)
			{
				for(int i = 0; i < 32; i++)
				{
					if(myContent[i] != null && myContent[i].equals(messages[1]))
					{
						permit = myNum;
						return true;
					}
				}
				for(int i = 0; i < maxServerNum; i++)
				{
					if(servers[i].alive)
					{
						for(int j = 0; j < 32; j++)
						{
							if(servers[i].contents[j] != null && servers[i].contents[j].equals(messages[1]))
							{
								permit = servers[i].num;
								return true;
							}
								
						}
					}
				}
				System.out.println("No such file found!");
				return false;
			}
			permit = -1;
			message = "report " + message + " " + myNum+ " end";
			byte[] message_b  = message.getBytes();//the message we want to send
			DatagramPacket sendData = new DatagramPacket(message_b, 0, message_b.length, Leader.IPAd, Leader.port);
			sendSock.send(sendData);
			long startTime =new Date().getTime();
			while(permit == -1)
			{
				if((new Date().getTime()) - startTime > 5000)
				{
					logger1.info("Leader is down or transaction problems happen! Please try again later");
					//reElectLeader();
					return false;
				}
				System.out.println("loading... " +permit);/////////////////////////////////////////////
			}
			
		}
		return false;
	}
	
	public static void updateToLeader() throws IOException
	{
		busy = true;
		Runtime r = Runtime.getRuntime();
		Process p = null;
		p = r.exec("ls sdfs");
		myContent = new String[32];
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String message = "content " + myNum;
		String a;
		int i = 0;
		while((a = stdInput.readLine()) != null )
		{
			myContent[i] = a;
			i++;
			message += " ";
			message += a;
		}
		if(Leader != null)
		{
			message += " ThatsAll end";
			byte[] mess_t = message.getBytes();	
			DatagramPacket temp_reporter = new DatagramPacket(mess_t, 0, mess_t.length, Leader.IPAd, Leader.port);
			sendSock.send(temp_reporter);
		}
		
		busy = false;
		System.out.println("______________________________________________" + "\n" + "Please give your order: ");
	}
	
	public static void reElectLeader() throws IOException
	{
		busy = true;
		System.out.println("Re-Electing leader!");
		int min = 999;
		int numb = 0;
		for(int i = 0; i < maxServerNum; i++)
		{
			if(servers[i].alive && servers[i].num < min)
				{
					min = servers[i].num;
					numb = i;
				}
		}
		if(min == Leader.num)//leader has not failed
			{
				busy = false;
				return;
			}
		if(min >= myNum)//set myself as leader
		{
			Leader = null;
			logger1.info("this machine is leader now!");
			updateToLeader();
			
		}
		else
		{
			Leader = servers[numb];
			logger1.info("machine " + min + " is new leader");
			updateToLeader();
		}

	}
	public static class processDelete implements Runnable
	{
		String fileName;
		int machineNum;
		public processDelete(String file, String num)
		{
			fileName = file;
			machineNum = Integer.parseInt(num);
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			for(int i = 0; i < 32; i++)
			{
				if(myContent[i] != null && myContent[i].equals(fileName))
				{
					String command = "rm sdfs/" + fileName;//delete the file
					Runtime r = Runtime.getRuntime();
					try {
						r.exec(command);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					logger1.info("delete a file " + fileName);
					myContent[i] = null;
					for(int j = 0; j < maxServerNum; j++)
					{
						if(servers[j].alive && machineNum == servers[j].num)
						{
							
							String message = "permit 0 end";
							byte[] mess_t = message.getBytes();	
							DatagramPacket temp_reporter = new DatagramPacket(mess_t, 0, mess_t.length,servers[j].IPAd, servers[j].port);
							try {
								sendSock.send(temp_reporter);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							System.out.println("______________________________________________" + "\n" + "Please give your order: ");
							return;
						}
					}
				}
			}
			for(int i = 0; i < maxServerNum; i++)
			{
				if(servers[i].alive)
				{
					for(int j = 0; j < 32; j++)
					{
						if(servers[i].contents[j] != null && servers[i].contents[j].equals(fileName))
						{
			
							permit = -1;
							String delete_request = "delete " + fileName +" end";
							byte[] delete_b = delete_request.getBytes();
							DatagramPacket sendData = new DatagramPacket(delete_b, 0, delete_b.length, servers[i].IPAd, servers[i].port);
							try {
								sendSock.send(sendData);
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							long startTime =new Date().getTime();
							while(permit == -1)
							{
								if((new Date().getTime()) - startTime > 5000)
								{
									logger1.info("Leader is down or transaction problems happen! Please try again later");
									return;
								}
								System.out.println("loading... " + permit);//////////////////////////////////
							}
							logger1.info("delete a file " + fileName);
							servers[i].contents[j] = null;
							for(int k = 0; k < maxServerNum; k++)
							{
								if(servers[k].alive && machineNum == servers[k].num)
								{
									
									String message = "permit 0 end";
									byte[] mess_t = message.getBytes();	
									DatagramPacket temp_reporter = new DatagramPacket(mess_t, 0, mess_t.length,servers[k].IPAd, servers[k].port);
									try {
										sendSock.send(temp_reporter);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									System.out.println("______________________________________________" + "\n" + "Please give your order: ");
									return;
								}
							}
						}
					}
				}
			}
			for(int j = 0; j < maxServerNum; j++)
			{
				if(servers[j].alive && machineNum == servers[j].num)
				{
					
					String message = "permit 1 end";
					byte[] mess_t = message.getBytes();	
					DatagramPacket temp_reporter = new DatagramPacket(mess_t, 0, mess_t.length,servers[j].IPAd, servers[j].port);
					try {
						sendSock.send(temp_reporter);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;
				}
			}
		}
		
		
	}

	public static class listen implements Runnable//new thing: content, report, delete
	{
		@Override
		public void run() {
			System.out.println("listener initializing");
			// TODO Auto-generated method stub
			DatagramPacket recvData = new DatagramPacket(new byte[512], 512);
			try {
				//receive message, if the message is from sb we know, then update its information, or we add it to our list
				while(true)
				{
					
					recvSock.receive(recvData);
					byte[] byte_t = new byte[512];
					byte_t = recvData.getData();
					String temp = new String(byte_t);
					String[] temp1 = temp.split(" ");
					
					if(temp1[0].equals("yooo"))
					{
						
						if(serversWeHave >= maxServerNum)
						{
							System.out.println("sorry we don't accept any more servers...");
							continue;
						}
						if(serve_welcome != 0)
						{
							System.out.println("system is busy...please try later...");
							continue;
						}
						
							
							waitOne = new a_server();
							waitOne.IPAd = recvData.getAddress();
							waitOne.port = recvData.getPort();//for the accepting part, servers will send message from "recvsock"
							waitOne.num =  Integer.parseInt(temp1[1]);
							//System.out.println("receiving a new server... ( " + recvData.getAddress()+ " " + recvData.getPort() +")" );
							System.out.println("receiving a new server... ( " + recvData.getAddress()+ " " + recvData.getPort() +")");
							if(waitOne.IPAd != null)
								System.out.println("please stand by and don't add any more before this is done...");
							serve_welcome = 1;
							if(serversWeHave == 0)
								{
								
								sendNum = 0;
								}
							door = new Thread(new welcome());
							door.start();
							
							continue;
						
					}
					
					String tem[] = temp.split(" ");
					if(tem[0].equals("content"))
					{
						//System.out.println(temp);
						busy = true;
						int hisNum = Integer.parseInt(tem[1]);
						for(int i = 0; i < maxServerNum; i++)
						{
							if(servers[i].num == hisNum && servers[i].alive)/////////////////////////////
							{
								//System.out.println(hisNum);
								servers[i].contents = new String[32];
								for(int a = 0; a < 32; a++)
								{
									servers[i].contents[a] = null;
								}
								int j = 2;
								while(true)
								{
									if(tem[j].equals("ThatsAll"))
										break;
									//System.out.println(tem[j]);
									servers[i].contents[j - 2] = tem[j];
									j++;
								}
								break;
							}
						}
						busy = false;
						
						continue;
					}
					if(tem[0].equals("report"))
					{
						System.out.print("command received: ");
						if(Leader != null)
							continue;
						if(tem[1].equals("put"))
							{
							System.out.println(" put");
							int hisNum = Integer.parseInt(tem[4]);
								for(int i = 0; i < maxServerNum; i++)
								{
									if(servers[i].alive && hisNum == servers[i].num )
									{
										
										String message = "permit 0 end";
										byte[] mess_t = message.getBytes();	
										DatagramPacket temp_reporter = new DatagramPacket(mess_t, 0, mess_t.length,servers[i].IPAd, servers[i].port);
										sendSock.send(temp_reporter);
										for(int j = 0; j < 32; j ++)
										{
											if(servers[i].contents[j] == null)
												{
													servers[i].contents[j] = tem[3];
													logger1.info("machine " + tem[4] + " adding a new file " + tem[3]);
													System.out.println("______________________________________________" + "\n" + "Please give your order: ");
													break;
												}
										}
										break;
									}
								}
								
								continue;
							}
						if(tem[1].equals("delete"))
						{
							System.out.println(" delete");
							Thread processDel = new Thread(new processDelete(tem[2], tem[3]));
							processDel.start();							
							continue;
						}
						if(tem[1].equals("get"))
						{
							System.out.println("get");
							boolean found = false;
							for(int i = 0; i < 32; i++)
							{
								if(myContent[i] != null && myContent[i].equals(tem[2]))
								{
									int hisNum = Integer.parseInt(tem[4]);
									for(int j = 0; j < maxServerNum; j++)
									{
										if(servers[j].alive && hisNum == servers[j].num )
										{
											
											String message = "permit " + 0 + " end";
											byte[] mess_t = message.getBytes();	
											DatagramPacket temp_reporter = new DatagramPacket(mess_t, 0, mess_t.length,servers[j].IPAd, servers[j].port);
											sendSock.send(temp_reporter);
											System.out.println("connecting server " + tem[4] + " and server 0...");
											System.out.println("______________________________________________" + "\n" + "Please give your order: ");
											found = true;
											break;
										}
									}
									break;
								}
							}
							if(found)
								continue;
							for(int i = 0; i < maxServerNum; i++)
							{
								if(servers[i].alive)
								{
									for(int j = 0; j < 32; j++)
									{
										if(servers[i].contents[j] != null && servers[i].contents[j].equals(tem[2]))
										{
											int hisNum = Integer.parseInt(tem[4]);
											for(int k = 0; k < maxServerNum; k++)
											{
												if(servers[k].alive && hisNum == servers[k].num )
												{
													
													String message = "permit " + servers[i].num + " end";
													byte[] mess_t = message.getBytes();	
													DatagramPacket temp_reporter = new DatagramPacket(mess_t, 0, mess_t.length,servers[k].IPAd, servers[k].port);
													sendSock.send(temp_reporter);
													System.out.println("connecting server " + tem[4] + " and server " + servers[i].num+  "...");
													System.out.println("______________________________________________" + "\n" + "Please give your order: ");
													found = true;
													break;
												}
											}
											break;
										}
											
									}
									if(found)
										break;
								}
							}
							if(!found)
							{
								for(int k = 0; k < maxServerNum; k++)
								{
									int hisNum = Integer.parseInt(tem[4]);
									if(servers[k].alive && hisNum == servers[k].num )
									{
										
										String message = "permit -2 end";
										byte[] mess_t = message.getBytes();	
										DatagramPacket temp_reporter = new DatagramPacket(mess_t, 0, mess_t.length,servers[k].IPAd, servers[k].port);
										sendSock.send(temp_reporter);
										System.out.println("No such file found!");
										System.out.println("______________________________________________" + "\n" + "Please give your order: ");
										found = true;
										break;
									}
								}
							}
							continue;
						}
					}
					if(tem[0].equals("delete"))
					{
						String command = "rm sdfs/" + tem[1];
						Runtime r = Runtime.getRuntime();
						r.exec(command);
						String message = "permit 0 end";
						byte[] mess_t = message.getBytes();	
						DatagramPacket temp_reporter = new DatagramPacket(mess_t, 0, mess_t.length,Leader.IPAd, Leader.port);
						sendSock.send(temp_reporter);
						continue;
					}
					if(tem[0].equals("connection"))
					{
						System.out.println("Connection request");
						if(sendingData)
						{
							System.out.println("busy in process...cant deal with that...");
							continue;
						}
						sendingData = true;
						Thread sender = new Thread(new sendData(Integer.parseInt(tem[2]), Integer.parseInt(tem[3]), tem[1], recvData.getAddress()));
						sender.start();
						continue;
					}
					if(tem[0].equals("max"))
					{
						
						
						DataRecvSock = Integer.parseInt(tem[2]);
						MAX_LENGTH = Integer.parseInt(tem[1]);
						System.out.println("the file is about " + MAX_LENGTH + " bytes!");
						continue;
					}
					if(tem[0].equals("permit"))
					{
						
						permit = Integer.parseInt(tem[1]);
						//System.out.println("permitted!");
						//System.out.println(permit);
						continue;
					}
					if(tem[0].equals("joining"))
					{
						//System.out.println(tem[1]);
						if(serve_welcome == 0)
							continue;
						//System.out.println(tem[0].length());
						String temp_string = tem[1];
						int temp_num = Integer.parseInt(temp_string);
						//System.out.println("here!!!!!!!!!!!!!!!!!!!"+temp_num);
						if(temp_num == 100)
							{
								serve_welcome = 0;
								sendNum = 1;
								waitOne = null;
								door.join();
								System.out.println("done adding server!");
								System.out.println("______________________________________________" + "\n" + "Please give your order: ");
							}
						else if(temp_num == sendNum)
							{
							
							//System.out.println("another servers' messages been sent to the new server...");
							
							if((sendNum + 1) > serversWeHave)
							{
								sendNum = 0;
								for(int i = 0; i < maxServerNum; i++)
								{
								
								if(servers[i].alive)
									continue;
								servers[i].IPAd = waitOne.IPAd;
								servers[i].port = waitOne.port;
								servers[i].num = waitOne.num;
								servers[i].preTime = new Date().getTime();
								servers[i].heartBeats = 1000;
								servers[i].alive = true;
								serversWeHave++;
								int ttttt = serversWeHave + 1;
								//System.out.println("finish adding new server! servers we have: " + serversWeHave);
								logger1.info("finish adding new server " + waitOne.num +" ! servers we have: " + ttttt);
								System.out.println("______________________________________________" + "\n" + "Please give your order: ");
								break;
								}
							}
							else
								sendNum ++;
							}
						continue;
					}
					temp = tem[1];
					if(tem[1].equals("Derpy"))
						continue;
					int heartBeats_t = Integer.parseInt(temp);//get the heart beat number
					InetAddress address_t = recvData.getAddress();//get sender's address
					temp = tem[0];
					int port_t = Integer.parseInt(temp);//get sender's port number
					boolean exist = false;
					int empty_pos = 0;
					for(int i = 0; i < maxServerNum; i++)
					{
						if(!servers[i].alive)
							empty_pos = i;
						if(servers[i].IPAd != null && servers[i].IPAd.equals(address_t) &&servers[i].port == port_t && (servers[i].alive || servers[i].isServer))
						{
							exist = true;
						
							if(servers[i].isServer && !servers[i].alive)
							{
								logger1.info("the contact(and leader) machine is back!");//contact machine back, set it as leader
								Leader = new a_server();
								Leader.IPAd = servers[i].IPAd;
								Leader.port = servers[i].port;
								Leader.num = servers[i].num;
								servers[i].isLeader = true;
								servers[i].alive = true;
								servers[i].heartBeats = 999;
								serversWeHave++;
								updateToLeader();
							}
							if(servers[i].heartBeats < heartBeats_t)
								{
								
								servers[i].heartBeats = heartBeats_t;
								servers[i].preTime = new Date().getTime();
								
								break;
								}
							
							
						}
					}
					if(exist)
						continue;
					//or we add it to list!
					
					servers[empty_pos].heartBeats = heartBeats_t;
					servers[empty_pos].port = port_t;
					servers[empty_pos].IPAd = address_t;
					servers[empty_pos].preTime = new Date().getTime();
					servers[empty_pos].alive = true;
					servers[empty_pos].num = Integer.parseInt(tem[2]);
					serversWeHave++;
					int tttt = serversWeHave + 1;
					//System.out.println("adding a new server! servers we have: " + serversWeHave);
					logger1.info("adding new server" + servers[empty_pos].num + " ! servers we have: " + tttt);
					System.out.println("______________________________________________" + "\n" + "Please give your order: ");
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public static class sendData implements Runnable
	{

		int target;
		InetAddress hisAd;
		int hisPort = -1;
		byte[] bigData_s;
		int count_s;
		int bytesSent;
		int fileLength;
		Path path;
		public sendData(int targ_port, int targ_num, String fileName, InetAddress ad) throws IOException
		{
			target = targ_num;
			hisPort = targ_port;
			hisAd = ad;
			path = Paths.get("sdfs/" + fileName);
			bigData_s = Files.readAllBytes(path);
			fileLength = bigData_s.length;
			for(int i = 0; i < maxServerNum; i++)
			{
				if(servers[i].alive && servers[i].num == target)
				{
					String message = "max " + fileLength + " " + DataSendSock + " end";
					byte[] mess_t = message.getBytes();	
					
					DatagramPacket temp_reporter = new DatagramPacket(mess_t, 0, mess_t.length, hisAd, servers[i].port);
					try {
						sendSock.send(temp_reporter);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		@Override
		public void run() {
			Sendclass ss =new Sendclass(DataSendSock, path.toString());
			try {
				ss.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sendingData = false;
			// TODO Auto-generated method stub
			/*if(hisPort == -1)
			{
				sendingData = false;
				return;
			}
			//System.out.println(fileLength);
			int len = (fileLength < 512)? fileLength:512;
			sendPack = new DatagramPacket(bigData_s, 0, len, hisAd, hisPort);
			bytesSent = 0;
			count_s = 0;
			while(bytesSent < bigData_s.length)
			{
				try {
					sendSock.send(sendPack);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				bytesSent += sendPack.getLength();
				int remain = bigData_s.length - bytesSent;
				int length = (remain > 512)? 512:remain;
				sendPack.setData(bigData_s, bytesSent, length);
			}
			
			
			sendingData = false;
			System.out.println("______________________________________________" + "\n" + "Please give your order: ");
			return;*/
			
		}
		
	}
	
	public static class check implements Runnable
	{

		@Override
		public void run() 
		{
			System.out.println("checker initializing");
			while(true)
			{
				long temp = new Date().getTime();
				for(int i = 0; i < maxServerNum; i++)
				{
					if(servers[i].alive)
					{
						
						//System.out.println("he is alive...");
						if(temp - servers[i].preTime > 4000)///////////////////////////////////////////////////
						{
							servers[i].alive = false;
							if(Leader != null && servers[i].num == Leader.num)
								try {
									reElectLeader();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							
							
						}
						if(servers[i].alive == false)
						{
							serversWeHave--;
							System.out.println("attention:");
							//System.out.println("server " + servers[i].IPAd.toString() + " " + servers[i].port +" has failed!");
							logger1.info("server " + servers[i].num +" " + servers[i].IPAd.toString() + " " + servers[i].port +" has failed!");
							System.out.println("______________________________________________" + "\n" + "Please give your order: ");
						}
					}
				}
			// TODO Auto-generated method stub
			
			}
		}
	}
	
	public static class welcome implements Runnable
	{

		@Override
		public void run() {
			System.out.println("start welcoming!");
			
			// TODO Auto-generated method stub
			long start_t = new Date().getTime();
			while(true)
			{
				if((new Date().getTime() - start_t) > 10000)
				{
					//System.out.println("joining server not responding for too long...ending receiving process...");
					logger1.info("joining server not responding for too long...ending receiving process...");
					serve_welcome = 0;
					sendNum = 1;
					waitOne = null;
					return;
				}
				if(serve_welcome == 0||waitOne == null ||waitOne.IPAd == null)
				{
					//there is possibility that after this line is passed then the main thread make waitOne = null...but it doesnt matter even this fail!!!!!!!!!
					break;
				}
				String message = null;
				if(sendNum != 0)
				{
					int sendNum_t = sendNum;
					for(int i = 0; i < maxServerNum; i++)
					{
						if(servers[i].alive)
							sendNum_t--;
						if(sendNum_t == 0)
						{
						message = Integer.toString(sendNum) +" " +  servers[i].IPAd.toString() +" " + servers[i].port +" " + servers[i].num + " dsasd";
						break;
						}
					}
				}
				else
					{
					message = "0 Derpy really loves my little ponies!";
					
				
					
					}
				byte[] message_b = message.getBytes();
				if(waitOne == null)
					break;
				DatagramPacket temp = new DatagramPacket(message_b, 0, message_b.length, waitOne.IPAd, waitOne.port);
				try {
					sendSock.send(temp);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//System.out.println(waitOne.IPAd.toString() + waitOne.port);
				
			}
			return;
		}
		
	}
	
	public static void getIntoNetwork() throws IOException
	{
		
		String dataS = "yooo " + myNum + " dasd";
		byte[] data = dataS.getBytes();
		System.out.println("connecting to server... please stand by...");
		
		DatagramPacket temp1 = new DatagramPacket(data, 0, data.length, thresholdIP, wholePort);
		//System.out.println(thresholdIP +" " + wholePort + " " + new String(data));
		recvSock.send(temp1);
		
		
		//now we throw a thread to watch the condition of connection
		connectionSteps = 0;
		watcher = new Thread(new watch());
		watcher.start();
		
		while(true)
		{
			DatagramPacket temp = new DatagramPacket(new byte[512], 512);
			recvSock.receive(temp);
			
			if(connectionSteps != 1)
			{
				//if we recevied message, then we are connected to threshold
			//System.out.println("connected!");
			connectionSteps = 1;
			}
			data = temp.getData();
			dataS = new String(data);
	
			String[] datas = dataS.split(" ");
			int num = Integer.parseInt(datas[0]);
			//System.out.println(num);
			//System.out.println(serversWehave);
			//System.out.println(num);
			//if all information got
			if(num == 0)
			{
				connectionSteps++;
				//add threshold to the list
				servers[serversWeHave].alive = false;
				servers[serversWeHave].heartBeats = 1000;
				servers[serversWeHave].preTime = new Date().getTime();
				servers[serversWeHave].port = wholePort;
				servers[serversWeHave].IPAd = thresholdIP;
				servers[serversWeHave].isServer = true;
				servers[serversWeHave].num = 0;
				servers[serversWeHave].isLeader = true;
				serversWeHave++;
				dataS = "joining 100 blablabla";
				data = dataS.getBytes();
				temp = new DatagramPacket(data, 0, data.length, thresholdIP, wholePort);
				recvSock.send(temp);//send him the last signal
				System.out.println("getting info done!");
				Leader = new a_server();
				Leader.IPAd = servers[serversWeHave- 1].IPAd;
				Leader.port = servers[serversWeHave-1].port;
				Leader.num = servers[serversWeHave-1].num;
				
				return;
			}
			if((num - serversWeHave) <= 0)
				{
				continue;
				}
			if((num - serversWeHave) > 1)
			{
				System.out.println("unknown errot!");
				System.exit(0);
			}
			else
			{
			servers[serversWeHave].alive = false;
			servers[serversWeHave].heartBeats = 1000;
			servers[serversWeHave].preTime = new Date().getTime();
			servers[serversWeHave].port = Integer.parseInt(datas[2]);
			servers[serversWeHave].num = Integer.parseInt(datas[3]);
			InetAddress tta =  InetAddress.getByName(datas[1].substring(1, datas[1].length()));
			servers[serversWeHave].IPAd = tta;
			serversWeHave++;
			//System.out.println("another server's data accepted!");
			dataS = "joining " + Integer.toString(serversWeHave) + " dasdada";
			data = dataS.getBytes();
			temp = new DatagramPacket(data, 0, data.length, thresholdIP, wholePort);
			recvSock.send(temp);//send him signal to tell him I received the info
			
			}
			
		}
		
		
	}
	
	public static class watch implements Runnable
	{

		@Override
		public void run() {
			System.out.println("watcher start!");
			// TODO Auto-generated method stub
			long my_time = new Date().getTime();
			while(connectionSteps == 0)//connect to servers
			{
				if(new Date().getTime() - my_time > 5000)
					{
					
					System.out.println("connection out of time!");
					System.exit(0);
					}
			}
			my_time = new Date().getTime();
			while(connectionSteps == 1)//get information about peers
			{
				if(new Date().getTime() - my_time > 15000)
					{
					System.out.println("getting info out of time!");
					System.exit(0);
					}
			}
			
		}
	}

	

	

}

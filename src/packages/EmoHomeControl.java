package packages;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.Timer;
import java.util.TimerTask;





/** Simple example of JNA interface mapping and usage. */
public class EmoHomeControl 
{      
	static HttpPost postChange;
	static Timer timer = new Timer();
    static boolean timeout = false;
	
    public static void main(String[] args) 
    {
    	Pointer eEvent			= Edk.INSTANCE.EE_EmoEngineEventCreate();
    	Pointer eState			= Edk.INSTANCE.EE_EmoStateCreate();
    	IntByReference userID 	= null;
    	short composerPort		= 1726;
    	short enginePort		= 3008;
    	int option 				= 1;
    	int state  				= 0;
    	
    	userID = new IntByReference(0);
    	
    	switch (option) {
		case 1:
		{
			//Connect to EmoEngine
			if (Edk.INSTANCE.EE_EngineRemoteConnect("127.0.0.1", enginePort, "Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
				System.out.println("Emotiv Engine start up failed.");
				return;
			}
			System.out.println("Connected to EmoEngine on [127.0.0.1]");
			break;
		}
		case 2:
		{
			//Connect to EmoComposer
			System.out.println("Target IP of EmoComposer: [127.0.0.1] ");

			if (Edk.INSTANCE.EE_EngineRemoteConnect("127.0.0.1", composerPort, "Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
				System.out.println("Cannot connect to EmoComposer on [127.0.0.1]");
				return;
			}
			System.out.println("Connected to EmoComposer on [127.0.0.1]");
			break;
		}
		default:
			System.out.println("Invalid option...");
			return;
    	}
    	
		while (true) 
		{
			state = Edk.INSTANCE.EE_EngineGetNextEvent(eEvent);
			
			// New event needs to be handled
			if (state == EdkErrorCode.EDK_OK.ToInt()) {

				int eventType = Edk.INSTANCE.EE_EmoEngineEventGetType(eEvent);
				Edk.INSTANCE.EE_EmoEngineEventGetUserId(eEvent, userID);

				// Log the EmoState if it has been updated
				if (eventType == Edk.EE_Event_t.EE_EmoStateUpdated.ToInt()) {

					Edk.INSTANCE.EE_EmoEngineEventGetEmoState(eEvent, eState);
					float timestamp = EmoState.INSTANCE.ES_GetTimeFromStart(eState);
					System.out.println(timestamp + " : New EmoState from user " + userID.getValue());
					
					System.out.print("WirelessSignalStatus: ");
					System.out.println(EmoState.INSTANCE.ES_GetWirelessSignalStatus(eState));

					//Logging Cognitive Actions 
					System.out.print("CognitivGetCurrentAction: ");
					System.out.println(EmoState.INSTANCE.ES_CognitivGetCurrentAction(eState));
					System.out.print("CurrentActionPower: ");
					System.out.println(EmoState.INSTANCE.ES_CognitivGetCurrentActionPower(eState));
					
					//Check for pushing action at a power over 5.0 and timeout false
					if ((EmoState.INSTANCE.ES_CognitivGetCurrentAction(eState) == 2) && (EmoState.INSTANCE.ES_CognitivGetCurrentActionPower(eState) > 0.5) && (timeout == false)) {
						System.out.println("Over");
						try {
							restPost();
							timerStart(true);
						} catch (ClientProtocolException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				}
				
			}
			else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
				System.out.println("Internal error in Emotiv Engine!");
				break;
			}
		}
    	
    	Edk.INSTANCE.EE_EngineDisconnect();
    	System.out.println("Disconnected!");
    }
    
    public static void restPost() throws ClientProtocolException, IOException {
    	  HttpClient client = HttpClientBuilder.create().build();
    	  //Gets the current status of the light
    	  HttpPost getStatus = new HttpPost("http://192.168.1.50:8083/ZWaveAPI/Run/devices%5B4%5D.instances%5B0%5D.commandClasses%5B0x25%5D.data.level.value");

    	  HttpResponse statusResponse = client.execute(getStatus);
    	  BufferedReader statusOut =  new BufferedReader(new InputStreamReader(statusResponse.getEntity().getContent()));
    	  int status = Integer.parseInt(statusOut.readLine());
    	  
    	  
    	  switch (status) {
    	  case 0:
  			{
  				//Set light value to off
  				postChange = new HttpPost("http://192.168.1.50:8083/ZWaveAPI/Run/devices%5B4%5D.instances%5B0%5D.commandClasses%5B0x25%5D.Set%28255%29");
  			}
  				break;
    	  case 255:
			{
				//Set Light value to on
				postChange = new HttpPost("http://192.168.1.50:8083/ZWaveAPI/Run/devices%5B4%5D.instances%5B0%5D.commandClasses%5B0x25%5D.Set%280%29");
			}
				break;
  		  }
  
    	  
    	  HttpClient switchClient = HttpClientBuilder.create().build();
    	  HttpResponse statusSwitch = switchClient.execute(postChange);
    	  BufferedReader rd = new BufferedReader(new InputStreamReader(statusSwitch.getEntity().getContent()));
    	  String line = "";

    	  while ((line = rd.readLine()) != null) {
    	   System.out.println(line);
    	  }
    	  
 
    }
    
    public static void timerStart(boolean startValue) {
    	if (startValue == true) {
    		timeout = true;
    		//Wait 10 seconds for head set
    		timer.schedule(new TimerTask() {
    			  @Override
    			  public void run() {
    			    timeout = false;
    			    System.out.println("Timout");
    			  }
    			}, 10000);
    	}
    }

}

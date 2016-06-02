package Twitter_Simulator

import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import scala.collection.mutable._
import spray.http._
import spray.client.pipelining._
import scala.util.Random

case class Start(system : ActorSystem)
case class send_userCount(userCount: Int)
case class send(user: Int)
case class stopChk(start: Long)
case class receive_self_tweets(userTweets:ListBuffer[Queue[String]],userNumber:Int)

object Client_Part 
{
    private val start:Long=System.currentTimeMillis
	//private def getStartTime = {start}
  
  
  def main(args: Array[String])
  {
	  val system = ActorSystem("ClientSystem")
	  //println("How many Users?")
	  val numOfUsers = 100
	  val client_actor =system.actorOf(Props(new ClientActor(system,numOfUsers)),name="ClientActor")
	 // val receiver =system.actorOf(Props(new clientReceiver()),name="ClientReceiver")
	  client_actor ! Start(system)
  }


class ClientActor(system : ActorSystem, userCount : Int) extends Actor 
{
  var clientBuffer= new ArrayBuffer[ActorRef]() 
  
	def receive = 
  	{       
  	 case Start(system) => 
  		{
  			//val start: Long = System.currentTimeMillis
  			val actorCount: Int = Runtime.getRuntime().availableProcessors()
  			//println("Actor Count => " + actorCount)
  			val client_driver = context.actorOf(Props(new Tweeting(system)),name="TwitterTable")

            //pass userCount from client to server for initializing followinglist and tweetstore
  			client_driver ! send_userCount(userCount)

  			Thread.sleep(5000)//Time for building following list and tweet store
  					
  			for(i <-0 until actorCount) 
  			{
  			 clientBuffer += context.actorOf(Props(new Tweeting(system)),name="Twitter"+i)
  			 println("Actor "+ clientBuffer(i)+" started")
  			 clientBuffer(i) ! send(userCount)
  			 
  			}
        }
  	}
}

class Tweeting(system:ActorSystem) extends Actor { 
  
  import system.dispatcher
  val pipeline1 = sendReceive
  val pipeline2 = sendReceive
 
  //var remote = context.actorFor("akka.tcp://ServerSystem@127.0.0.1:5150/user/Server_Part")
  //val start: Long = System.currentTimeMillis 

	def receive = 
  	{
  		case send_userCount(userCount) =>
  		{
            pipeline1(Post("http://localhost:8080/tweet/userCount?userCount="+userCount))
  			//remote ! receive_userCount(userCount)
  			//println("UserCount sent from Client is "+userCount)
  		}
    
  	case send(userCount: Int) =>
         {
         
         //Logic to send to server
          //println("Client Started Tweeting") 
         var userNum=Random.nextInt(userCount)
         var tweetLength=Random.nextInt(140)
         
         while(tweetLength==0)
        	 tweetLength=Random.nextInt(140) 
        	 
         var tweetString=""
	     tweetString += scala.util.Random.alphanumeric.take(tweetLength).mkString
	     println("Tweet: \n "+tweetString)        
	    //To simulate some statistics
	     
	    /* if (userNum%10==0)
	       Thread.sleep(1)
	     else
	       Thread.sleep(0)*/
	      
	     pipeline2(Post("http://localhost:8080/tweet/receive_tweets?userNum="+userNum+"&tweetString="+tweetString))

	     
	     
	     if(userNum%15==0)   
         pipeline2(Post("http://localhost:8080/tweet/receive_tweets?userNum="+userNum+"&tweetString="+"Favorite"+tweetString))

         
         if(userNum%10==0)
         {
           pipeline2(Post("http://localhost:8080/tweet/receive_reTweets?userNum="+userNum))
         }
	     //remote ! receive_tweets(userNum,tweetString)
	     //println("Tweeting Started")
	    
        if(userNum%15==0)
        {      
         pipeline2(Post("http://localhost:8080/tweet/get_favorites?userNum="+userNum))     
        }
        
        if(userNum%20==0)        
        { 
         if(tweetString.length()<130)
         pipeline2(Post("http://localhost:8080/tweet/receive_tweets?userNum="+userNum+"&tweetString="+tweetString+"#SemesterOver")) 
        }
        
        if(userNum%21==0)        
        { 
         if(tweetString.length()<130)
         pipeline2(Post("http://localhost:8080/tweet/receive_tweets?userNum="+userNum+"&tweetString="+tweetString+"#WorldCup")) 
        }
 
        if(userNum%20==0)        
        { 
         if(tweetString.length()<130)
         pipeline2(Post("http://localhost:8080/tweet/receive_hashTag?hashTag="+"#SemesterOver")) 
         pipeline2(Post("http://localhost:8080/tweet/receive_hashTag?hashTag="+"#WorldCup")) 

        }
       
	  if(System.currentTimeMillis - start < 300000) //Running Time specification here
        // Thread.sleep(2)   
	     self ! send(userCount)
        else
         {
           // println("client "+self+" is shutting down")
           //remote ! shut()
           //pipeline2(Post("http://localhost:8080/tweet/print_stats?i="+1))     
           context.system.shutdown()  
         }    
        }  
   case receive_self_tweets(userTweets:ListBuffer[Queue[String]], userNumber:Int)=>
      {
        println("Tweets for user "+userNumber+" are =>"+userTweets)
      } 
  	}
}
}
	









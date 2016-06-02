package Twitter_Simulator

import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.routing.RoundRobinRouter
import scala.collection.mutable._
import spray.routing.SimpleRoutingApp
import scala.util.Random
import java.io._

case class receive_userCount(userCount: Int)
case class receive_tweets(userNum: Int,tweetString: String)
case class receive_reTweets(userNum: Int)
case class get_favorites(userNum: Int)
case class print_stats()
case class showHashTags(hashTag: String)

//case class shutdown()
//case class shut()

object Server_Part extends App with SimpleRoutingApp
{ 
	 var tweetsHandled:Long=0
  
  override def main(args: Array[String]) 
  {
	 implicit val system = ActorSystem("Tweeter")
	 
	  val actorCount: Int = Runtime.getRuntime().availableProcessors()*100
	  
	  var tweetStore = new ListBuffer[Queue[String]] ()
	  var hashTagStore = new ListBuffer[Queue[String]] ()
	  var followingList = new ArrayBuffer[ArrayBuffer[Int]] ()
	  
	  println("Twitter Server Started....")
	  
	  
	  val server_actor = system.actorOf(Props(new Server_Part(tweetStore,followingList,hashTagStore)).withRouter(RoundRobinRouter(actorCount)))
	 

     //var allTweets = TweetObj.tweets

	 /* def getJson(route: Route) = get {
	    respondWithMediaType(MediaTypes.`application/json`) { route }
	  }*/

	startServer(interface = "localhost", port = 8080) {
	    get {
	      path("") { ctx =>
	        ctx.complete("Twitter Demo - REST API")
	      }
	    }~
    post {
      path("tweet"/"receive_tweets") {
        parameters("userNum".as[Int], "tweetString".as[String]) { (userNum, tweetString) =>
        server_actor!receive_tweets(userNum,tweetString)
         //val newTweet = PostedTweet(userNum, tweetString)
          //allTweets = newTweet :: allTweets
          complete {
            "OK"+tweetString
          }
        }
      }
    }~
    post {
      path("tweet" / "userCount") {
        parameters("userCount".as[Int]) { (userCount) =>
        server_actor!receive_userCount(userCount)
          complete {
            "number of users="+userCount
          }
        }
      }
    }~
    post {
      path("tweet" / "receive_reTweets") {
        parameters("userNum".as[Int]) { (userNum) =>
        server_actor!receive_reTweets(userNum)
          complete {
            "userNum="+userNum
          }
        }
      }
    }~  
    post {
      path("tweet" / "get_favorites") {
        parameters("userNum".as[Int]) { (userNum) =>
        server_actor!get_favorites(userNum)
          complete {
            "userNum="+userNum
          }
        }
      }
    }~
     post {
      path("tweet"/"print_stats") {
        parameters("i".as[Int]) { (i) =>
        server_actor ! print_stats()
          complete {
            "flag="+1
          }
        }
        }
      }~
     post {
      path("tweet"/"receive_hashTag") {
        parameters("hashTag".as[String]) { (hashTag) =>
        server_actor ! showHashTags(hashTag)
          complete {
            "flag="+1
          }
        }
        }
      }
      
      
    }   
	}
	  
  
  
class Server_Part(tweetStore: ListBuffer[Queue[String]],followingList : ArrayBuffer[ArrayBuffer[Int]],hashTagStore: ListBuffer[Queue[String]]) extends Actor 
{
  val writer = new FileWriter("Server_Output.txt",true )
  //var remote = context.actorFor("akka.tcp://ServerSystem@127.0.0.1:5152/user/Tweeting")
  def receive = 
  {    
    
    //FOLLOWING LIST AND TWEETSTORE
    
    case receive_userCount(userCount:Int)=>
      {
      //writer.write("Received User Count is " + userCount+"\n")
      //println("Received User Count is " + userCount)
      
      //Initializing Following List and TweetStore      
      for(i <- 0 to userCount-1)
      {
    	  val Q = new Queue[String]
    	  tweetStore+= Q
      }
      for(i <- 0 to 1)
      {
    	  val Q = new Queue[String]
    	  hashTagStore+= Q
      }
      for(k<-0 to userCount-1)
      {
    	  val F = new ArrayBuffer[Int]         
    	  followingList+= F
      }
      
      for(i <- 0 to userCount-1)
      {
    	  var fol= Random.nextInt(userCount/10) //number of followers
    	  while (fol==0)
    			fol=Random.nextInt(userCount/10)
    	  for(j<- 0 to fol-1)
    	  {
    		  var following_number=Random.nextInt(userCount-1)
    		  while (following_number==i)
    			following_number=Random.nextInt(userCount)
    		  if(!followingList(i).isEmpty)
    			while (followingList(i).contains(following_number))
    				following_number=Random.nextInt(userCount-1)

    		  followingList(i)+= following_number 
    	  }
      }  
      //writer.write("Following List is --> \n"+followingList+"\n")    
      println("Following List is --> \n"+followingList)
      }  
    
      
      
      
    //RECEIVE TWEETS -> USERTIMELINE AND HOMETIMELINE
      
    case receive_tweets(userNum: Int,tweetString: String)=>
      {
        //var tweetcount = Server_Part.inc
        //writer.write("Tweets handled until now =>"+tweetsHandled+"\n")
        Server_Part.tweetsHandled+=1
        println("Tweets handled until now =>"+Server_Part.tweetsHandled)
        
    	 if(tweetStore(userNum).length >= 100)
    	 {
    	  tweetStore(userNum).dequeue
    	 }     
    	 tweetStore(userNum)+= tweetString  
    	 if(tweetString.endsWith("#SemesterOver"))
    	 {
    	   hashTagStore(0)+=userNum+tweetString 
    	 } 
    	 if(tweetString.endsWith("#WorldCup"))
    	 {
    	   hashTagStore(1)+=userNum+tweetString 
    	 } 
    	 var temp=new ListBuffer[Queue[String]]
    	 for(i<-0 until followingList(userNum).length)
    	 {
    	   temp += tweetStore(followingList(userNum)(i))
    	 }
    	 
    	 //Last 100 tweets of each person whom this user follows //Get Statuses/home_timeline
    	 println("Last 100 tweets of people whom USER_ID:"+userNum+" follows are=>"+temp)
    	 
    	 //Last 100 tweets of this user //Get Statuses/user_timeline
    	 println("Last 100 tweets of USER_ID:"+userNum+" are =>"+tweetStore(userNum))

       //println("TweetStore is "+tweetStore)
      //writer.write("TweetStore is "+tweetStore+"\n")
     //Thread.sleep(10)
     }  
      
      
      
     //RETWEET FUNCTIONALITY 

     case receive_reTweets(userNum: Int)=>
     {
       //writer.write("Tweets handled until now =>"+tweetsHandled+"\n")
       Server_Part.tweetsHandled+=1
       println("Tweets handled until now =>"+Server_Part.tweetsHandled)
       
        
       if(followingList(userNum).length>0)
   
       if(tweetStore(userNum).length >= 100)
       {
    	  tweetStore(userNum).dequeue
       }     
       
       tweetStore(userNum)+= "ReTweeted"+tweetStore(followingList(userNum)(0))(0)
       println("User "+userNum+" retweeted "+tweetStore(followingList(userNum)(0))(0))

     }
     
     
     
     //FAVORITE FUNCTIONALITY (LAST 20)
     
     case get_favorites(userNum: Int)=>
     {
       //writer.write("Tweets handled until now =>"+tweetsHandled+"\n")
       Server_Part.tweetsHandled+=1
       println("Tweets handled until now =>"+Server_Part.tweetsHandled)
       var count=0      
       var temp=new Queue[String]
       if(tweetStore(userNum).length > 0)
       {
         //if(tweetStore(userNum).length  > 0)
        for(i<-0 until tweetStore(userNum).length)
        {
          if (tweetStore(userNum)(i).contains("Favorite") && count<20)
          { 
           
           temp += tweetStore(userNum)(i)
           count+=1 
          } 
        }
        println("Last 20 Favorite Tweets are =>"+temp)
         
        }
       }  
     
     
       //HASHTAG RETRIEVAL
     
        case showHashTags(hashTag)=>   
          {
           Server_Part.tweetsHandled+=1
            if (hashTag=="#SemesterOver")
              println("Tweets With HashTag #SemesterOver =>"+ hashTagStore(0))
            if  (hashTag=="#WorldCup")
              println("Tweets With HashTag #WorldCup =>"+ hashTagStore(1))
          }

     /*case print_stats()=>
       {
       println("Tweets handled until now =>"+Server_Part.tweetsHandled)
       }*/
     }
  
  }
}

package smpd

import java.net._
import java.io._

import scala.collection.immutable.Stream
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map

trait MPDConnection {
  var address: String
  var port: Int
	
  var version: String = _
	
  var sock: Socket = _
  var out: PrintWriter = _
  var in: BufferedReader = _
	
  def connect() {
    sock = new Socket(this.address, this.port)
    out = new PrintWriter(sock.getOutputStream, true)
    in = new BufferedReader(new InputStreamReader(sock.getInputStream))
  }
	
  def disconnect() {
    out.close
    in.close
    sock.close
  }

}


trait MPDPairs extends MPDConnection {
  type MPDPair = Tuple2[String, String]
  type MPDPairs = ListBuffer[MPDPair]
	
  def getPairs(message: String): Either[Exception, MPDPairs] = {
    try {
      out.println(message)
      var mpdPairs = new MPDPairs
		
      Stream.continually(in.readLine) foreach { line =>
        if (line == "OK") 
          return Right(mpdPairs)
        else if (line.matches("^ACK.*")) 
	  return Left(new Exception(line))
	else if (line.matches("^OK MPD.*")) 
	  version = line.substring(7)
	else {
	  val value = line.split("^[^:]+:\\s")
	  val key = line.split(": .*")
          if(value.length == 0) mpdPairs += new MPDPair(key(0), "Unknown")	
	  else mpdPairs += new MPDPair(key(0), value(1))
	}			
      } 
      out.flush
    } catch {
      case e: Exception => return Left(e)
    }		
		
    return Left(new Exception("UNKNOWN ERROR!"))
  }
}

trait MPDMap extends MPDConnection {
  type MPDMap = Map[String, String]
	
  def getMap(message: String): Either[Exception, MPDMap] = {
    try {
      out.println(message)
      var map = Map[String, String]()
			
      Stream.continually(in.readLine) foreach { line =>
        if (line == "OK") 
	  return Right(map)
	else if (line.matches("^ACK.*")) 
	  return Left(new Exception(line))
        else if (line.matches("^OK MPD.*")) 
          version = line.substring(7)
	else {
	  val value = line.split("^[^:]+:\\s")
	  val key = line.split(": .*")
          map += (key(0) -> value(1))
        }
        out.flush
      } 
    } catch {
      case e: Exception => return Left(e)
    }
		
    return Left(new Exception("UNKNOWN ERROR!"))
  }
}

class MPD(var address: String, var port: Int)  {				
  def this(address: String) = this(address, 6600)		
}

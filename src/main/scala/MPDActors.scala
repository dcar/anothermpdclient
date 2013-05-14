package dom.ampdclient

import smpd.{MPD, MPDMap, MPDPairs, MPDConnection}

import akka.actor.Actor
import akka.actor.Cancellable
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit.SECONDS
import scala.concurrent.ExecutionContext.Implicits.global

import android.util.Log
import android.widget.{TextView, SeekBar, ListView}

sealed trait MPDActor {
  val mpd: MPDConnection
  val connectionError: String
  
  def tryAndConnect() {
    try {
      mpd.connect
    } catch {
      case e: Exception => Log.e(connectionError, e.toString)
      case b: java.net.BindException => Log.e(connectionError, "Already connected..")
    }
  }
  
  def tryAndDisconnect() {
    try {
      mpd.disconnect
    }
    catch {
      case e: Exception => Log.e(connectionError, e.toString)
    }
  }
}

class PlayerActor(ip: String, port: Int) extends Actor with MPDActor {
  val mpd = new MPD(ip, port) with MPDMap
  val connectionError = "PlayerConnectionError"

  private var state = "stop"
  private var elapsed = 0
  private var time = 100

  private val duration = new FiniteDuration(1, SECONDS)
  private var scheduler: Option[Cancellable] = None

  def receive = {
    case Player(player) => setCurrentSong(player)
    case Increment(player) => increment(player)
    case Seek(progress) => seek(progress)
    case Disconnect => tryAndDisconnect
    case Connect => tryAndConnect
    case Stop => context.stop(self)
  }
	
  private def setCurrentSong(player: PlayerFragment): Unit = {
    val activity = player.getActivity.asInstanceOf[MainActivity]
    val songInfo = player.getView.findViewById(R.id.songInfo).asInstanceOf[TextView]

    mpd.getMap("currentsong") match {
      case Right(song) => {
        val artist = song.getOrElseUpdate("Artist", "N/A")
        val title = song.getOrElseUpdate("Title", "N/A")

        activity.run { () => songInfo.setText(artist + " - " + title) }

        mpd.getMap("status") match {
          case Right(status) => {
            state = status.getOrElseUpdate("state", "stop")
            
            val pSlider = player.getView.findViewById(R.id.playerSlider).asInstanceOf[SeekBar]
            elapsed = status.getOrElseUpdate("elapsed", "0").toDouble.asInstanceOf[Int]
            time = song.getOrElseUpdate("Time", "100").toInt

            scheduler match {
              case Some(schedulerExists) => schedulerExists.cancel
              case None => Log.i("Player", "Starting player...")
            }

            activity.run( () => {
              pSlider.setProgress(elapsed) 
              pSlider.setMax(time)
            })

            scheduler = Some(context.system.scheduler.schedule(duration, duration, self, Increment(player)))
          }
          case Left(error) => {
            activity.run( () => {
              songInfo.setText(error.toString)
              Log.e("PlayerError", "Could not retrieve status.")
            }) 
            state = "stop"
          }
        }

      }
      case Left(error) => 
        activity.run( () => {
          songInfo.setText(error.toString)
          Log.e("PlayerError", "Could not retrieve current song.")
        }) 
        state = "stop"
        if(MPDSystem.connected) {
          player.reconnect
          player.update
        }
        else {
          val duration = new FiniteDuration(5, SECONDS)
          context.system.scheduler.scheduleOnce(duration, self, Player(player))
        }
    }
    
  }

  private def increment(player: PlayerFragment) {
    state match {
      case "play" => {
        val pSlider = player.getView.findViewById(R.id.playerSlider).asInstanceOf[SeekBar]
        val activity = player.getActivity.asInstanceOf[MainActivity]
        elapsed += 1
        if (elapsed > time) scheduler.get.cancel; player.reconnect; player.update
        activity.run( () => {
          pSlider.setProgress(elapsed)
          Log.i("Progress", elapsed.toString)
        })
      }
      case "pause" => Log.i("Player", "Player has been paused."); scheduler.get.cancel
      case "stop" => Log.i("Player", "Player has been stopped."); scheduler.get.cancel
    }
  }

  private def seek(progress: Int) {
    mpd.getMap("seekcur " + progress.toString) match {
      case Right(success) => 
      case Left(error) => Log.e("SeekError", error.toString)
    }
  }

}

class DatabaseActor(ip: String, port: Int) extends Actor with MPDActor {
  val mpd = new MPD(ip, port) with MPDPairs

  private val updateOccurred: String = "Update"
  val connectionError = "DBConnectionError"
	
  def receive = {
    case Update => Log.i(updateOccurred, "An update has occurred!")
    case Database(db, kind) => lsInfo(db, kind)
    case Disconnect => tryAndDisconnect
    case Connect => tryAndConnect
    case Stop => context.stop(self)
  }
	
  private def lsInfo(db: DatabaseFragment, kind: String) {
    val dbView = db.getView.findViewById(android.R.id.list).asInstanceOf[ListView]
    val adapter = dbView.getAdapter.asInstanceOf[DatabaseTupleAdapter]
    val activity = db.getActivity.asInstanceOf[MainActivity]
    mpd.getPairs("list " + kind) match {
      case Right(pairs) => {
        pairs.foreach { tuple =>
          if(adapter.checkItem(tuple) != true) adapter.addItem(tuple);
        }
        activity.run( () => adapter.notifyDataSetChanged )
      }
      case Left(error) => {
        Log.e("DatabaseError", "Could not retrieve Database")
        adapter.clean
        activity.run( () => adapter.notifyDataSetChanged )
        if(MPDSystem.connected) {
          db.reconnect
          db.update
        }
        else {
          val duration = new FiniteDuration(5, SECONDS)
          context.system.scheduler.scheduleOnce(duration, self, Database(db, "artist"))
        }

      } 
    }
  }
}

class IdleActor(ip: String, port: Int) extends Actor with MPDActor {
  val mpd = new MPD(ip, port) with MPDIdle
  
  val connectionError = "IdleConnectionError"
   
  def receive = {
    case Connect => tryAndConnect
    case Disconnect => tryAndDisconnect
    case Stop => context.stop(self)
    case Idle(activity) => mpd.idle(true, activity, self)
  }
   
}

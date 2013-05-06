package dom.ampdclient

import smpd.MPDConnection

import android.app.Service
import android.content.Intent
import android.widget.SeekBar

sealed trait MPDAction
case object Update extends MPDAction
case object Playlist extends MPDAction
case object StoredPlaylist extends MPDAction
case object Mixer extends MPDAction
case object Output extends MPDAction
case object Options extends MPDAction
case object Sticker extends MPDAction
case object Subscription extends MPDAction
case object Message extends MPDAction
case class Player(player: PlayerFragment) extends MPDAction
case class Database(db: DatabaseFragment, kind: String) extends MPDAction
case class Idle(service: MainActivity) extends MPDAction
case class Increment(player: PlayerFragment) extends MPDAction
case class Seek(seekBar: Int) extends MPDAction


case object Stop
case object Connect
case object Disconnect

trait MPDIdle extends MPDConnection {
  import android.util.Log
  import akka.actor.ActorRef

  private val connectionError: String = "ConnectionError"
  private val mpdError: String = "MPDError"

  def idle(forever: Boolean = false, activity: MainActivity, actor: ActorRef) {
    try {
      out.println("idle")

      Stream.continually(in.readLine) foreach { line =>
        if (line == "OK") {
	  out.flush
	  if(forever) actor ! Idle(activity)
	  return
	}	
	else if (line.matches("^ACK.*")) {
	  Log.e(mpdError, line)
	  Thread.sleep(5000)
	  out.flush
	  return
	}
	else if (line.matches("^OK MPD.*"))
	  version = line.substring(7)
	else {
	  val value = line.split("^[^:]+:\\s")

          value(1) match {
            case "player" => {
              val player = activity.getFragment(0)
              player.update
            }
            case "database" => {
              val database = activity.getFragment(1)
              database.update
            }
            case _ => Log.i("MPDCallback", "Not needed..")
          }
        }
      }
    } catch {
      case n: NullPointerException => {
        Thread.sleep(5000)
        val player = activity.getFragment(0)
        val database = activity.getFragment(1)
        player.reconnect
        database.reconnect
        player.update
        database.update
        activity.reconnect

        Log.i(connectionError, "Retrying connection...")
	if(forever) actor ! Idle(activity)
      }
    }
  }
}

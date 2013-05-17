package dom.ampdclient

import smpd.{MPD, MPDMap, MPDPairs}

import android.app.Activity
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v4.app.FragmentActivity
import android.content.{Intent, Context, BroadcastReceiver, IntentFilter}
import android.util.Log

import akka.actor.{ActorSystem, ActorRef, Props}

object MPDSystem {
  var system: Option[ActorSystem] = Some(ActorSystem("MPDSystem"))
  var connected = false
  val ip = "192.168.1.146"
  val port = 6600
}

trait ActivityActor {
  var actor: Option[ActorRef]

  private def createActor[T <: akka.actor.Actor](instantiateActor: => T) {
    val props = Props(instantiateActor)
    val actorRef = MPDSystem.system.get.actorOf(props)
    actor = Some(actorRef)
    actor.get ! Connect
    Log.i(actor.get.toString, "Actor created and tried to connect.")

  }

  def connect[T <: akka.actor.Actor](instantiateActor: => T) {
    actor match {
      case Some(actorExists) => {
        actorExists ! Stop
        actor = None
        createActor(instantiateActor)
      }
      case None => createActor(instantiateActor)
    }
  }

  def disconnect() {
    actor.get ! Disconnect
  }

  def stop() {
    disconnect
    actor.get ! Stop
    actor = None
  }

  def reconnect() {
    actor match {
      case Some(actorExists) => actorExists ! Connect
      case None => Log.e("Actor", "Actor does not exist!")
    }
  }

  def checkActor(): Boolean = {
    actor match {
      case Some(actorExists) => return true
      case None => return false
    }
  }

}

trait FragmentActor extends ActivityActor {
  def update()
}


class MainActivity extends FragmentActivity with ActivityActor {
  var actor: Option[ActorRef] = None

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.main)
    Log.i("MainActivity", "ActivityCreated")
    MPDSystem.system match {
      case Some(systemExists) => Log.i("MPDSystem", "MPDSystem already exists")
      case None => MPDSystem.system = Some(ActorSystem("MPDSystem"))
    }
    initializePaging()
  }

  override def onStart() {
    super.onStart()
    Log.i("MainActivity", "Activity started.")
    connect(new IdleActor(MPDSystem.ip, MPDSystem.port))
  }

  override def onResume() {
    super.onResume()
    Log.i("MainActivity", "Activity resumed.")
    actor.get ! Idle(this)
  }

  override def onRestart() {
    super.onRestart()
    Log.i("MainActivity", "Activity restarted.")
    getFragment(0).connect(new PlayerActor(MPDSystem.ip, MPDSystem.port))
    getFragment(1).connect(new DatabaseActor(MPDSystem.ip, MPDSystem.port))
  }

  override def onStop() {
    super.onStop()
    Log.i("MainActivity", "Activity stopped.")
    stop
  }

  override def onDestroy() {
    super.onDestroy()
    Log.i("MainActivity", "Activity destroyed!.")
    MPDSystem.system.get.shutdown
    MPDSystem.system = None
  }

  private def initializePaging() {
    val pagerAdapter = new AMPDPagerAdapter(getSupportFragmentManager)
		 
    val viewPager = findViewById(R.id.viewPager).asInstanceOf[ViewPager]
    viewPager.setAdapter(pagerAdapter)
    viewPager.setOffscreenPageLimit(2)
    viewPager.setCurrentItem(0)
  }


  def getFragment(position: Int): FragmentActor = {
    val pager = this.findViewById(R.id.viewPager).asInstanceOf[ViewPager]
    val adapter = pager.getAdapter.asInstanceOf[AMPDPagerAdapter]
    val fragment: FragmentActor = adapter.instantiateItem(pager, position).asInstanceOf[FragmentActor]
    return fragment
  }

  def run(fun: => Unit): Unit = {
    this.runOnUiThread(new Runnable { def run() = { fun } } ) 
  }

}

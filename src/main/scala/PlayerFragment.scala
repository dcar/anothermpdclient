package dom.ampdclient

import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.content.IntentFilter
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener

import akka.actor.{ActorRef, Props}

class PlayerFragment extends Fragment with FragmentActor {
  var actor:Option[ActorRef] = None

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, bundle: Bundle):View = {
    inflater.inflate(R.layout.player_view, container, false)
  }
  
  override def onActivityCreated(bundle: Bundle) {
    super.onActivityCreated(bundle)
    val pSlider = getView.findViewById(R.id.playerSlider).asInstanceOf[SeekBar]
    connect(MPDSystem.system.actorOf(Props(new PlayerActor("192.168.0.2", 6600))))
    update
    val seekMPD = (progress: Int) => actor.get ! Seek(progress)
    setSeekBarListener(pSlider, seekMPD)
  }

  override def onPause() {
    super.onPause
    stop
  }

  override def onResume() {
    super.onResume
    connect(MPDSystem.system.actorOf(Props(new PlayerActor("192.168.0.2", 6600))))
    update
  }
  
  override def update() {
    actor.get ! Player(this)
  }
  
  private def setSeekBarListener(sb: SeekBar, actorMessage: Int => Unit) {
    sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      override def onStartTrackingTouch(seekBar: SeekBar) {}
      override def onStopTrackingTouch(seekBar: SeekBar) {
        actorMessage(seekBar.getProgress)
      }
      override def onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
    })
  }
}

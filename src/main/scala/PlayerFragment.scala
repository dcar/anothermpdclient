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

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    connect(() => new PlayerActor("192.168.0.2", 6600))
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, bundle: Bundle):View = {
    inflater.inflate(R.layout.player_view, container, false)
  }
  
  override def onActivityCreated(bundle: Bundle) {
    super.onActivityCreated(bundle)
    val pSlider = getView.findViewById(R.id.playerSlider).asInstanceOf[SeekBar]
    val seekMPD = (progress: Int) => actor.get ! Seek(progress)
    setSeekBarListener(pSlider, seekMPD)
  }

  override def onStop() {
    super.onStop
    stop
  }

  override def onStart() {
    super.onStart
    update
  }
  
  override def update() {
    actor match {
      case Some(actorExists) => actorExists ! Player(this)
      case None => Log.i("PlayerActor", "Actor does not exist.")
    }
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

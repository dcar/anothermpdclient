package dom.ampdclient

import android.util.Log
import android.support.v4.app.ListFragment
import android.os.Bundle
import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.{ListView, BaseAdapter, TextView, AdapterView}
import android.widget.AdapterView.OnItemClickListener

import scala.collection.mutable.ListBuffer
import akka.actor.{ActorRef, Props}

class DatabaseFragment extends ListFragment with FragmentActor {
  var actor:Option[ActorRef] = None

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    connect(new DatabaseActor(MPDSystem.ip, MPDSystem.port))
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, bundle: Bundle):View = {
    inflater.inflate(R.layout.database_view, container, false)
  }

  override def onActivityCreated(bundle: Bundle) {
    super.onActivityCreated(bundle)
    setListAdapter(new DatabaseTupleAdapter(this))
  }

  override def update() {
    val adapter = getListAdapter.asInstanceOf[DatabaseTupleAdapter]
    adapter.clean
    actor match {
      case Some(actorExists) => actorExists ! Database(this, "artist")
      case None => Log.i("DatabaseActor", "Actor does not Exist")
    }
  }

  override def onStop() {
    super.onStop
    stop
  }

  override def onListItemClick(lv: ListView, v: View, position: Int, id: Long) {
    val adapter = getListAdapter.asInstanceOf[DatabaseTupleAdapter]
    val tuple = adapter.getItem(position)

    if(tuple._1 == "Artist") {
      adapter.clean
      actor.get ! Database(this, "album " + "\"" + tuple._2 +"\"")
    }
  }


  override def onStart() {
    super.onStart
    update
  }


}

class DatabaseTupleAdapter(f: ListFragment) extends BaseAdapter {
  private var dbItems: ListBuffer[Tuple2[String, String]] = new ListBuffer[Tuple2[String, String]]

  def addItem(item: Tuple2[String, String]): Unit = dbItems += item 

  def checkItem(item: Tuple2[String, String]): Boolean = dbItems contains item

  def clean(): Unit = dbItems.clear

  def getCount(): Int = dbItems.length

  def getItem(position: Int): Tuple2[String, String] = dbItems(position)

  def getItemId(position: Int): Long = position.toLong

  def getView(x: Int, y: android.view.View, z: android.view.ViewGroup): android.view.View = {
    val tv = f.getActivity.getLayoutInflater.inflate(R.layout.row_view, null).asInstanceOf[TextView]
    val item = dbItems(x)
    tv.setText(item._2)
    return tv
  }
}

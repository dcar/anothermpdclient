package dom.ampdclient
import android.support.v4.app.{FragmentPagerAdapter, FragmentManager, Fragment}

class AMPDPagerAdapter(manager: FragmentManager) extends FragmentPagerAdapter(manager) {
  var fragmentList = new java.util.ArrayList[Fragment]
	
  def addFragment(fragment: Fragment) {
    fragmentList.add(fragment)
    notifyDataSetChanged
  }
	
  override def getCount():Int = {
    fragmentList.size
  }
	
  override def getItem(position: Int):Fragment = {
    fragmentList.get(position)
  }
}

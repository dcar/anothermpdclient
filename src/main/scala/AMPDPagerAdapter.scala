package dom.ampdclient
import android.support.v4.app.{FragmentPagerAdapter, FragmentManager, Fragment}

class AMPDPagerAdapter(manager: FragmentManager) extends FragmentPagerAdapter(manager) {
	
  override def getCount():Int = {
    return 2
  }
	
  override def getItem(position: Int):Fragment = {
    position match {
      case 0 => return new PlayerFragment
      case 1 => return new DatabaseFragment
    }
  }
}

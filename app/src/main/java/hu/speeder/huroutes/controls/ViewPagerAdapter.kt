package hu.speeder.huroutes.controls

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import hu.speeder.huroutes.utils.TabDataList
import hu.speeder.huroutes.controls.WebViewFragment
import hu.speeder.huroutes.web.HuroutesWebView

class ViewPagerAdapter constructor(
    owner: AppCompatActivity,
    val tabData: TabDataList
) : FragmentStateAdapter(owner.supportFragmentManager, owner.lifecycle) {
    override fun createFragment(position: Int): Fragment {
        return tabData.elementAt(position).makeFragment()
    }

    override fun getItemCount(): Int {
        return tabData.size
    }
}
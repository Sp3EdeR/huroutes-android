package hu.speeder.huroutes.utils

import hu.speeder.huroutes.controls.WebViewFragment

class TabData(
    val navRes: Int,
    val makeFragment: () -> WebViewFragment
)

typealias TabDataList = List<TabData>

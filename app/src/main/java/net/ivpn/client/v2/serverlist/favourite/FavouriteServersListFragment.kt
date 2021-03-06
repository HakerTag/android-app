package net.ivpn.client.v2.serverlist.favourite

/*
 IVPN Android app
 https://github.com/ivpn/android-app
 <p>
 Created by Oleksandr Mykhailenko.
 Copyright (c) 2020 Privatus Limited.
 <p>
 This file is part of the IVPN Android app.
 <p>
 The IVPN Android app is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 3 of the License, or (at your option) any later version.
 <p>
 The IVPN Android app is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 details.
 <p>
 You should have received a copy of the GNU General Public License
 along with the IVPN Android app. If not, see <https://www.gnu.org/licenses/>.
*/

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import net.ivpn.client.IVPNApplication
import net.ivpn.client.R
import net.ivpn.client.common.extension.checkVPNPermission
import net.ivpn.client.common.prefs.ServerType
import net.ivpn.client.databinding.FragmentFavouriteServersListBinding
import net.ivpn.client.ui.dialog.DialogBuilder
import net.ivpn.client.ui.dialog.Dialogs
import net.ivpn.client.v2.serverlist.ServerListTabFragment
import net.ivpn.client.v2.serverlist.all.ServerListFragment
import net.ivpn.client.v2.serverlist.dialog.Filters
import net.ivpn.client.v2.viewmodel.ConnectionViewModel
import net.ivpn.client.v2.viewmodel.ServerListFilterViewModel
import net.ivpn.client.v2.viewmodel.ServerListViewModel
import org.slf4j.LoggerFactory
import javax.inject.Inject

class FavouriteServersListFragment : Fragment(), ServerListViewModel.ServerListNavigator,
        ServerListFilterViewModel.OnFilterChangedListener {

    lateinit var binding: FragmentFavouriteServersListBinding

    @Inject
    lateinit var viewmodel: ServerListViewModel

    @Inject
    lateinit var filterViewModel: ServerListFilterViewModel

    @Inject
    lateinit var connect: ConnectionViewModel

    lateinit var adapter: FavouriteServerListRecyclerViewAdapter

    private var serverType: ServerType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filterViewModel.listeners.add(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            serverType = savedInstanceState.getSerializable(SERVER_TYPE_STATE) as ServerType
            LOGGER.info("serverType = $serverType")
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        IVPNApplication.getApplication().appComponent.provideActivityComponent().create().inject(this)
        if (parentFragment != null) {
            serverType = (parentFragment as ServerListTabFragment?)!!.getServerType()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_favourite_servers_list, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
        viewmodel.start(serverType)
    }

    override fun onResume() {
        super.onResume()
        viewmodel.navigators.add(this)
    }

    override fun onPause() {
        super.onPause()
        viewmodel.navigators.remove(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(SERVER_TYPE_STATE, serverType)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewmodel.favouriteListeners.remove(adapter)
        filterViewModel.listeners.remove(this)
    }

    private fun init(view: View) {
        viewmodel.setServerType(serverType)
        binding.viewmodel = viewmodel
        adapter = FavouriteServerListRecyclerViewAdapter(viewmodel.adapterListener,
                filterViewModel.filter.get())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setEmptyView(view.findViewById(R.id.empty_view))
        viewmodel.favouriteListeners.add(adapter)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            LOGGER.info("onActivityResult: RESULT_CANCELED")
            return
        }

        LOGGER.info("onActivityResult: RESULT_OK")
        when (requestCode) {
            CONNECT_BY_SERVER_LIST -> {
                connect.reconnectOrNothing()
            }
        }
    }

    override fun navigateBack() {
        (parentFragment as ServerListTabFragment).navigateBack()
    }

    override fun onServerSelected() {
        checkVPNPermission(CONNECT_BY_SERVER_LIST)
        (parentFragment as ServerListTabFragment).navigateBack()
    }

    override fun showDialog(dialogs: Dialogs) {
        DialogBuilder.createNotificationDialog(this.context, Dialogs.INCOMPATIBLE_SERVERS)
    }

    override fun openFastestSetting() {
        (parentFragment as ServerListTabFragment).openFastestSetting()
    }

    fun cancel() {}

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FavouriteServersListFragment::class.java)
        private const val SERVER_TYPE_STATE = "SERVER_TYPE_STATE"
        private const val CONNECT_BY_SERVER_LIST = 122
    }

    override fun onFilterChanged(filter: Filters?) {
        adapter.setFilter(filter)
    }
}
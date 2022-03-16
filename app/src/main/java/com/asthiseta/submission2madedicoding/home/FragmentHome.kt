package com.asthiseta.submission2madedicoding.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.asthiseta.core.data.Resource
import com.asthiseta.core.ui.ItemAdapter
import com.asthiseta.submission2madedicoding.R
import com.asthiseta.submission2madedicoding.databinding.FragmentHomeBinding
import com.asthiseta.submission2madedicoding.misc.ShowStates
import org.koin.android.ext.android.getKoin
import org.koin.android.viewmodel.ViewModelParameter
import org.koin.android.viewmodel.koin.getViewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

class FragmentHome : Fragment(), ShowStates {
    private var bindingHome : FragmentHomeBinding? = null
    private lateinit var homeAdapter : ItemAdapter
    private val homeVM : HomeVM by sharedGraphViewModel(R.id.main_navigation)

    private inline fun<reified VM: ViewModel> Fragment.sharedGraphViewModel (
        @IdRes navGraphId: Int,
        qualifier: Qualifier? = null,
        noinline parameters: ParametersDefinition?= null
    ) = lazy {
        val store = findNavController().getViewModelStoreOwner(navGraphId).viewModelStore
        getKoin().getViewModel(ViewModelParameter(VM::class, qualifier, parameters, store))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.title = getString(R.string.app_name)
        bindingHome = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return bindingHome!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        bindingHome?.errorLayout?.emptyText?.text = getString(R.string.searchForKos_txt)

        homeAdapter = ItemAdapter(arrayListOf()){name, iv ->
            findNavController().navigate(FragmentHomeDirections.actionHomeToDetailFragment(name),
                FragmentNavigatorExtras(iv to name)
            )
        }

        bindingHome?.recyclerHome?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = homeAdapter
        }

        bindingHome?.searchBar?.apply {
            queryHint = resources.getString(R.string.search_placeholderHint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String): Boolean {
                    homeVM.setForSearch(query)
                    bindingHome!!.searchBar.clearFocus()
                    return true

                }

                override fun onQueryTextChange(newText: String): Boolean {
                    return false
                }
            })
        }
        observeHome()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingHome = null
    }

    override fun homeSuccess(bindingHome: FragmentHomeBinding?){
        bindingHome?.apply {
            errorLayout.mainNotFound.visibility = gone
            progressBar.visibility = gone
            recyclerHome.visibility = visible
            resources
        }

    }

    override fun homeLoading(bindingHome: FragmentHomeBinding?) {
        bindingHome?.apply {
            errorLayout.apply {
                mainNotFound.visibility = gone

            }
            progressBar.visibility = visible
            recyclerHome.visibility = gone
        }

    }

    override fun homeError(bindingHome: FragmentHomeBinding?, message: String?) {
        bindingHome?.apply {
            progressBar.visibility = gone
            errorLayout.apply {
                mainNotFound.visibility = visible
                noResult.visibility = visible
                if(message == null){
                    emptyText.text = resources.getString(R.string.empty_data)
                }else{
                    emptyText.text = message
                }
            }
            recyclerHome.visibility = gone
        }

    }

    private fun observeHome() {
        homeVM.item.observe(viewLifecycleOwner) {
            if (it != null) {
                when (it) {
                    is Resource.Success -> {
                        homeSuccess(bindingHome)
                        it.data?.let { data -> homeAdapter.setData(data) }
                    }
                    is Resource.Loading -> homeLoading(bindingHome)
                    is Resource.Error -> homeError(bindingHome, it.message)
                }
            }
        }
    }
}
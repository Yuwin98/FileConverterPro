package com.yuwin.fileconverterpro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.yuwin.fileconverterpro.Constants.Companion.IMAGE_CHOOSE
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.databinding.FragmentConvertBinding
import java.util.*

private const val IMAGE_REQUEST_CODE = 100
private const val PDF_REQUEST_CODE = 600

class ConvertFragment : Fragment() {

    private val args by navArgs<ConvertFragmentArgs>()
    private var data = mutableListOf<ConvertInfo>()

    private val IMAGE_LIMIT = 50

    private var binding: FragmentConvertBinding? = null

    private var qualityInt: Int  = 0
    private var pdfQuality: Int = 0
    private var mimeType = ""

    private val convertViewModel : ConvertViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private val itemViewModel: ItemViewModel by viewModels()

    private val convertAdapter by lazy { ConvertAdapter() }
    private var itemTouchHelperCallBack: SimpleItemTouchCallBack? = SimpleItemTouchCallBack(convertAdapter)
    private var touchHelper: ItemTouchHelper? = ItemTouchHelper(itemTouchHelperCallBack!!)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        binding = FragmentConvertBinding.inflate(inflater, container, false)
        binding?.viewModel = convertViewModel
        binding?.lifecycleOwner = viewLifecycleOwner

        val uriList = args.UriList.items
        data = setupData(uriList)
        mimeType = data[0].fileType.toLowerCase(Locale.ROOT)
        setupRecyclerView()

        data.forEach {
            Log.d("PDFUriCorrect", it.uri.toString())
        }

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Convert Images (${convertAdapter.itemCount}/$IMAGE_LIMIT)"
        Log.d("mimeTypePDF", mimeType)
        if(mimeType == "pdf") {
            Log.d("mimeTypePDF", "In PDF set true")
            convertViewModel.setIsPdfConversion(true)
        }else {
            convertViewModel.setIsPdfConversion(false)
        }

        mainViewModel.readDefaultFormat.observeOnce(viewLifecycleOwner, {
            binding?.convertAllSpinner?.setSelection(it)
        })

        convertViewModel.isPdfConversion.observe(viewLifecycleOwner, {
            val newData = convertAdapter.getAdapterData()
            val iterator = newData.iterator()
            while (iterator.hasNext()) {
                val oldValue = iterator.next()
                oldValue.isPdfConversion = it
            }
            updateData(newData)

            if(it) {
                binding?.convertAllPdfSpinner?.visibility = View.VISIBLE
                binding?.convertAllSpinner?.visibility = View.GONE
            }else {
                binding?.convertAllPdfSpinner?.visibility = View.GONE
                binding?.convertAllSpinner?.visibility = View.VISIBLE
            }
        })

        binding?.convertAllCheckBox?.setOnCheckedChangeListener { _, isChecked -> convertViewModel.setOnConvertAllCheckChanged(isChecked) }
        convertViewModel.allConvert.observe(viewLifecycleOwner, {
            val newData = convertAdapter.getAdapterData()
            val iterator = newData.iterator()
            while (iterator.hasNext()) {
                val oldValue = iterator.next()
                oldValue.convertAll = it
            }
            updateData(newData)

        })

        binding?.convertAllPdfSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                convertViewModel.setDefaultSpinnerPosition(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }
        binding?.convertAllSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                convertViewModel.setDefaultSpinnerPosition(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }
        convertViewModel.defaultPosition.observe(viewLifecycleOwner, {
            val newData = convertAdapter.getAdapterData()
            val iterator = newData.iterator()
            while (iterator.hasNext()) {
                val oldValue = iterator.next()
                oldValue.defaultConvertFormat = it
            }
            updateData(newData)
        })
        mainViewModel.readQuality.observe(viewLifecycleOwner, {
            binding?.qualitySeekBar?.progress = it
        })

        binding?.qualitySeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                convertViewModel.setQualityValue(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        convertViewModel.qualityValue.observe(viewLifecycleOwner, { quality ->
            qualityInt = quality.toInt()
        })

        binding?.pdfQualitySpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                pdfQuality = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }

        binding?.convertButton?.setOnClickListener {
            val data = ConvertInfoList(convertAdapter.getAdapterData())
            if(data.items.isNotEmpty()) {
                val action = ConvertFragmentDirections.actionConvertToConvertProgressFragment(data, qualityInt, pdfQuality)
                findNavController().navigate(action)
            }else {
                Toast.makeText(requireContext(), "Add files to convert", Toast.LENGTH_SHORT).show()
            }
        }

        return binding?.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.convert_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.addImages -> {
                if(mimeType != "pdf") {
                    chooseImages()
                }else {
                    choosePDF()
                }
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uriList = mutableListOf<Uri>()
        if (data != null && (requestCode == IMAGE_REQUEST_CODE || requestCode == PDF_REQUEST_CODE) && resultCode == Activity.RESULT_OK) {
            if (data.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    uriList.add(imageUri)
                }
            } else {
                val imageUri = data.data!!
                uriList.add(imageUri)
            }


            val myUriList = UriList(uriList)
            var newData: MutableList<ConvertInfo> = setupData(myUriList.items)
            val oldData: MutableList<ConvertInfo> = convertAdapter.getAdapterData()
            if(newData.size + oldData.size > IMAGE_LIMIT) {
                val take = IMAGE_LIMIT - oldData.size
                if(take > 0) {
                    newData = newData.take(take).toMutableList()
                    updateNewData(newData, oldData[0].convertAll, oldData[0].isPdfConversion, oldData[0].defaultConvertFormat )
                    newData.let{ lst1 -> oldData.let(lst1::addAll) }
                }else {
                    newData = oldData
                }
                (requireActivity() as AppCompatActivity).supportActionBar?.title = "Convert Images ($IMAGE_LIMIT/$IMAGE_LIMIT)"
                Toast.makeText(activity, "Max Convert Limit $IMAGE_LIMIT reached", Toast.LENGTH_SHORT).show()
            }else {
                if(oldData.isNotEmpty()) {
                    updateNewData(newData, oldData[0].convertAll, oldData[0].isPdfConversion, oldData[0].defaultConvertFormat )
                    newData.let{ lst1 -> oldData.let(lst1::addAll) }
                }
                (requireActivity() as AppCompatActivity).supportActionBar?.title = "Convert Images (${newData.size}/$IMAGE_LIMIT)"
            }

            this.data = newData
            convertAdapter.setData(newData)
            convertAdapter.notifyDataSetChanged()
        }
    }

    private fun setupData(uriList: List<Uri>): MutableList<ConvertInfo> {
        val data = mutableListOf<ConvertInfo>()
        for(uri in uriList) {
            val (fileName, fileSize) = Util.getImageDetails(requireContext(), uri)
            val fileType = Util.getMimeType(requireContext(), uri)
            val convertInfo = ConvertInfo(
                    uri,
                    fileName,
                    fileSize,
                    uri.path ?: "N/A",
                    fileType ?: "N/A",
                    false,
                false,
                    0,
                    0,
            )
            data.add(convertInfo)
        }
        return data
    }

    private fun setupRecyclerView() {
        binding?.imageQueue?.layoutManager = LinearLayoutManager(requireContext())
        binding?.imageQueue?.adapter = convertAdapter
        touchHelper?.attachToRecyclerView(binding?.imageQueue)
        convertAdapter.setData(data)
    }

    private fun updateNewData(list: MutableList<ConvertInfo>, convertAll: Boolean?, isPdfConversion: Boolean? ,defaultConvertType: Int?) {
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val oldValue = iterator.next()
            oldValue.convertAll = convertAll
            oldValue.defaultConvertFormat = defaultConvertType
            oldValue.isPdfConversion = isPdfConversion
        }
    }

    private fun updateData(data: List<ConvertInfo>) {
        this.data = data as MutableList<ConvertInfo>
        convertAdapter.setData(data)
        convertAdapter.notifyDataSetChanged()
    }

    private fun chooseImages() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.putExtra("uris", args.UriList)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Images"), IMAGE_REQUEST_CODE)
    }

    private fun choosePDF() {
        val intent = Intent()
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PDF_REQUEST_CODE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.imageQueue?.adapter = null
        binding = null
        itemTouchHelperCallBack = null
        touchHelper = null

    }



}






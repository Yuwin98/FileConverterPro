package com.yuwin.fileconverterpro

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.databinding.FragmentConvertBinding
import java.util.*

private const val IMAGE_REQUEST_CODE = 100
private const val PDF_REQUEST_CODE = 600

class ConvertFragment : Fragment() {

    private val args by navArgs<ConvertFragmentArgs>()
    private var data = mutableListOf<ConvertInfo>()

    private var imageLimit = 5

    private var convertAll = false
    private lateinit var convertInto: String

    private var binding: FragmentConvertBinding? = null

    private var qualityInt: Int = 100
    private var pdfQuality: Int = 0
    private var mimeType = ""

    private val convertViewModel: ConvertViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    private val convertAdapter by lazy { ConvertAdapter() }
    private var itemTouchHelperCallBack: SimpleItemTouchCallBack? =
        SimpleItemTouchCallBack(convertAdapter)
    private var touchHelper: ItemTouchHelper? = ItemTouchHelper(itemTouchHelperCallBack!!)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        activity?.let {
            val moveCopy = it.findViewById<MotionLayout>(R.id.moveCopyLayout)
            moveCopy.visibility = View.GONE
        }
        binding = FragmentConvertBinding.inflate(inflater, container, false)
        binding?.viewModel = convertViewModel
        binding?.lifecycleOwner = viewLifecycleOwner

        convertAll = args.convertAll
        convertInto = args.convertInto

        val uriList = args.UriList.items
        data = setupData(uriList)
        setupRecyclerView()
        mimeType = data[0].fileType.toLowerCase(Locale.ROOT)

        mainViewModel.readIsPremium.observeOnce(viewLifecycleOwner, { isPremium ->
            if (isPremium == 1 && !isPdfConversion()) {
                imageLimit = 50
                setSpinnerAdapter(isPremium, false)
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Convert Images (${data.size}/$imageLimit)"
            } else if (isPremium == 0 && !isPdfConversion()) {
                imageLimit = 5
                setSpinnerAdapter(isPremium, false)
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Convert Images (${data.size}/$imageLimit)"
            } else if (isPremium == 1 && isPdfConversion()) {
                imageLimit = 2
                setSpinnerAdapter(isPremium, true)
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Convert Images (${data.size}/$imageLimit)"
            } else if (isPremium == 0 && isPdfConversion()) {
                imageLimit = 2
                setSpinnerAdapter(isPremium, true)
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Convert Images (${data.size}/$imageLimit)"
            }
        })

        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            "Convert Images (${data.size}/$imageLimit)"

        if (mimeType == "pdf") {
            Log.d("mimeTypePDF", "In PDF set true")
            convertViewModel.setIsPdfConversion(true)
        } else {
            convertViewModel.setIsPdfConversion(false)
        }

        mainViewModel.readQuality.observe(viewLifecycleOwner, {
            binding?.qualitySeekBar?.progress = it
        })

        binding?.qualitySeekBar?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
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



        binding?.fileQualitySpinner?.onItemSelectedListener = object : OnItemSelectedListener {
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
            if (data.items.isNotEmpty()) {
                if (args.isPdfMerge && data.items.size < 2) {
                    Toast.makeText(
                        requireContext(),
                        "2 Files needed for merging",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {
                    val action = ConvertFragmentDirections.actionConvertToConvertProgressFragment(
                        data,
                        qualityInt,
                        pdfQuality,
                        args.isPdfMerge,
                        args.mergeImagesIntoPdf,
                        args.convertInto,
                        convertAll,
                        args.singleImageToPdf,
                        args.pdfIntoImages
                    )
                    findNavController().navigate(action)
                }
            } else {
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
        when (item.itemId) {
            R.id.addImages -> {
                if (mimeType != "pdf") {
                    chooseImages()
                } else {
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
            imageLimit = if (requestCode == PDF_REQUEST_CODE) 2 else imageLimit

            val myUriList = UriList(uriList)
            var newData: MutableList<ConvertInfo> = setupData(myUriList.items)
            val oldData: MutableList<ConvertInfo> = convertAdapter.getAdapterData()
            if (newData.size + oldData.size > imageLimit) {
                val take = imageLimit - oldData.size
                if (take > 0) {
                    newData = newData.take(take).toMutableList()
                    updateNewData(
                        newData,
                        args.convertInto,
                    )
                    newData.let { lst1 -> oldData.let(lst1::addAll) }
                } else {
                    newData = oldData
                }
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Convert Images ($imageLimit/$imageLimit)"
                Toast.makeText(
                    activity,
                    "Max Convert Limit $imageLimit reached",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (oldData.isNotEmpty()) {
                    updateNewData(
                        newData,
                        args.convertInto,
                    )
                    newData.let { lst1 -> oldData.let(lst1::addAll) }
                }
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Convert Images (${newData.size}/$imageLimit)"
            }

            this.data = newData
            convertAdapter.setData(newData)
            convertAdapter.notifyDataSetChanged()
        }
    }


    private fun setupData(uriList: List<Uri>): MutableList<ConvertInfo> {
        val data = mutableListOf<ConvertInfo>()
        for (uri in uriList) {
            val (fileName, fileSize) = Util.getImageDetails(requireContext(), uri)
            val fileType = Util.getMimeType(requireContext(), uri)
            val isPdfConversion = fileType == "PDF"
            val convertInfo = ConvertInfo(
                uri,
                fileName,
                fileSize,
                uri.path ?: "N/A",
                fileType ?: "N/A",
                args.convertInto
            )
            data.add(convertInfo)
        }
        return data
    }



    private fun isPdfConversion(): Boolean {
        return args.isPdfMerge || args.pdfIntoImages
    }

    private fun setupRecyclerView() {
        binding?.imageQueue?.layoutManager = LinearLayoutManager(requireContext())
        binding?.imageQueue?.adapter = convertAdapter
        touchHelper?.attachToRecyclerView(binding?.imageQueue)
        convertAdapter.setData(data)
    }

    private fun updateNewData(
        list: MutableList<ConvertInfo>,
        convertInto: String
    ) {
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val oldValue = iterator.next()
            oldValue.convertInto = convertInto
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

    private fun setSpinnerAdapter(isPremium: Int, isPdfConversion: Boolean) {
        val premiumQuality = arrayListOf("72 PPI", "200 PPI", "300 PPI", "400 PPI")
        val freeQuality = arrayListOf("72 PPI")
        val premiumSpinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            premiumQuality
        )
        val freeSpinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            freeQuality
        )

        if (isPremium == 1) {
            if (isPdfConversion) {
                binding?.fileQualitySpinner?.visibility = View.GONE
                binding?.pdfQualityDefaultText?.visibility = View.VISIBLE
                binding?.fileQualitySpinner?.adapter = freeSpinnerAdapter
            } else {
                binding?.fileQualitySpinner?.visibility = View.VISIBLE
                binding?.pdfQualityDefaultText?.visibility = View.GONE
                binding?.fileQualitySpinner?.adapter = premiumSpinnerAdapter
            }
        } else {
            binding?.fileQualitySpinner?.visibility = View.GONE
            binding?.pdfQualityDefaultText?.visibility = View.VISIBLE
            binding?.fileQualitySpinner?.adapter = freeSpinnerAdapter
        }
    }


}






package com.yuwin.fileconverterpro

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.yuwin.fileconverterpro.Constants.Companion.FREE_IMAGE_LIMIT
import com.yuwin.fileconverterpro.Constants.Companion.PDF_LIMIT
import com.yuwin.fileconverterpro.Constants.Companion.PREMIUM_IMAGE_LIMIT
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.databinding.FragmentConvertBinding
import java.lang.Exception
import java.util.*

private const val IMAGE_REQUEST_CODE = 100
private const val PDF_REQUEST_CODE = 600

class ConvertFragment : Fragment() {

    private val args by navArgs<ConvertFragmentArgs>()
    private var data = mutableListOf<ConvertInfo>()

    private var imageLimit = FREE_IMAGE_LIMIT

    private var convertAll = false
    private lateinit var convertInto: String

    private var binding: FragmentConvertBinding? = null

    private var qualityInt: Int = 100
    private var paddingPage: Int = 50
    private var fileQuality: Int = 0
    private var pageSize: Int = 0
    private var mimeType = ""

    private val convertViewModel: ConvertViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private val convertAdapter by lazy { ConvertAdapter() }
    private var itemTouchHelperCallBack: SimpleItemTouchCallBack? =
        SimpleItemTouchCallBack(convertAdapter)
    private var touchHelper: ItemTouchHelper? = ItemTouchHelper(itemTouchHelperCallBack!!)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (activity as MainActivity).requestInterstitial()

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

        mimeType = if(data[0] != null) {
            data[0].fileType.toLowerCase(Locale.ROOT)
        }else {
            "N/A"
        }


        mainViewModel.readIsPremium.observeOnce(viewLifecycleOwner, { isPremium ->
            if (isPremium == 1 && !isPdfConversion()) {
                imageLimit = PREMIUM_IMAGE_LIMIT
                setSpinnerAdapter(isPremium, false)
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Convert Images (${data.size}/$imageLimit)"
            } else if (isPremium == 0 && !isPdfConversion()) {
                imageLimit = FREE_IMAGE_LIMIT
                setSpinnerAdapter(isPremium, false)
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Convert Images (${data.size}/$imageLimit)"
            } else if (isPremium == 1 && isPdfConversion()) {
                imageLimit = PDF_LIMIT
                setSpinnerAdapter(isPremium, true)
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Convert Images (${data.size}/$imageLimit)"
            } else if (isPremium == 0 && isPdfConversion()) {
                imageLimit = PDF_LIMIT
                setSpinnerAdapter(isPremium, true)
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "Convert Images (${data.size}/$imageLimit)"
            }
        })



        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            "Convert Images (${data.size}/$imageLimit)"

        if (mimeType == "pdf") {
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

        binding?.paddingNumberEditText?.doOnTextChanged { text, start, before, count ->
            when {
                text.toString().isBlank() -> {
                    binding?.paddingNumberEditText?.setText(getString(R.string.value_0))
                    convertViewModel.setPaddingValue(0)
                }
                text.toString().toInt() <= 1000 -> {
                    convertViewModel.setPaddingValue(text.toString().toInt())
                }
                text.toString().toInt() > 1000 -> {
                    binding?.paddingNumberEditText?.setText(getString(R.string.value_1000))
                    convertViewModel.setPaddingValue(1000)
                }
            }

        }
        convertViewModel.paddingValue.observe(viewLifecycleOwner, {
            paddingPage = it
        })


        binding?.fileQualitySpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                fileQuality = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

        binding?.pdfPageSizeSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                pageSize = position
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }

        binding?.convertButton?.setOnClickListener {
            mainViewModel.readIsPremium.observe(viewLifecycleOwner, {isPremium ->
                if(isPremium == 0) {
                    (activity as MainActivity).showInterstitial()
                }else {
                    (activity as MainActivity).mainViewModel?.setIsLoading(false)
                }
            })

        }

        (activity as MainActivity).mainViewModel?.isLoading?.observe(viewLifecycleOwner, {isLoading ->
            if(!isLoading) {
                startConverting()
            }
        })




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

            if (args.pdfIntoImages || args.isPdfMerge) {
                val oldUriList = args.UriList.items.toMutableList()
                val currentSize = convertAdapter.getAdapterData().size + uriList.size
                val newIndex = oldUriList.size

                if (currentSize > PDF_LIMIT) {
                    Toast.makeText(
                        requireContext(),
                        "PDF Convert Limit reached",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                } else {
                    oldUriList.addAll(uriList)
                    val toPreviewUriList = UriList(oldUriList)
                    val action = ConvertFragmentDirections.actionConvertToPdfReviewFragment(
                        toPreviewUriList,
                        args.convertAll,
                        args.convertInto,
                        args.isPdfMerge,
                        args.mergeImagesIntoPdf,
                        args.singleImageToPdf,
                        args.pdfIntoImages,
                        newIndex,
                        args.pageInfoList
                    )
                    findNavController().navigate(action)
                    return
                }

            }

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
        for(i in uriList.indices) {
            try {
                val currentIndex = args.pageInfoList?.items?.get(i)?.pdfIndex
                val totalPages = currentIndex?.let { args.pageInfoList?.items?.get(it)?.totalPages }
                val selectedPages =
                    currentIndex?.let { args.pageInfoList?.items?.get(it)?.selectedPages?.size }
                val pageString = "$selectedPages/$totalPages"
                val (fileName, fileSize) = Util.getImageDetails(requireContext(), uriList[i])
                val fileType = Util.getMimeType(requireContext(), uriList[i])
                val convertInfo = ConvertInfo(
                    uriList[i],
                    fileName,
                    fileSize,
                    uriList[i].path ?: "N/A",
                    fileType ?: "N/A",
                    pageString,
                    isPdfConversion(),
                    args.convertInto
                )
                data.add(convertInfo)
            }catch (e: Exception) {
                continue
            }
        }
        return data
    }

    private fun startConverting() {
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
                    fileQuality,
                    args.isPdfMerge,
                    args.mergeImagesIntoPdf,
                    args.convertInto,
                    convertAll,
                    args.singleImageToPdf,
                    args.pdfIntoImages,
                    args.pageInfoList,
                    paddingPage,
                    pageSize
                )
                findNavController().navigate(action)
            }
        } else {
            Toast.makeText(requireContext(), "Add files to convert", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPdfConversion(): Boolean {
        return args.isPdfMerge || args.pdfIntoImages
    }

    private fun isImgIntoPdf(): Boolean {
        return args.singleImageToPdf || args.mergeImagesIntoPdf
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
        val premiumQuality = arrayListOf("72 PPI", "96 PPI", "150 PPI", "300 PPI")
        val freeQuality = arrayListOf("72 PPI")
        val imageQuality = arrayListOf(
            "Original",
            "Instagram Profile",
            "Instagram Landscape",
            "Instagram Portrait",
            "Instagram Square",
            "Instagram Story",
            "Twitter Profile",
            "Twitter Header",
            "Twitter Post",
            "Facebook Profile",
            "Facebook Cover",
            "Facebook Post",
            "Facebook Event Cover",
            "Pinterest Pin",
            "Pinterest Square Pin",
            "Pinterest Long Pin",
            "Pinterest Carousel"


        )
        val pdfPageSize = arrayListOf("A0", "A1", "A2", "A3", "A4", "A5")

        val pdfPageSizeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            pdfPageSize
        )

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

        val imageQualityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            imageQuality
        )

        if (isPremium == 1) {
            if (isPdfConversion || isImgIntoPdf()) {
                binding?.fileQualityTextView?.text = getString(R.string.pdf_quality_text)
                binding?.fileQualityTextView?.visibility = View.VISIBLE
                binding?.pdfQualityDefaultText?.visibility = View.VISIBLE



                binding?.fileQualitySpinner?.visibility = View.GONE
                binding?.fileQualitySpinner?.adapter = freeSpinnerAdapter

                if (isImgIntoPdf()) {
                    binding?.pdfPageSizeTextView?.visibility = View.VISIBLE
                    binding?.pdfPageSizeSpinner?.visibility = View.VISIBLE
                    binding?.pdfQualityDefaultText?.visibility = View.GONE
                    binding?.paddingTextView?.visibility = View.VISIBLE
                    binding?.paddingPixelTextView?.visibility = View.VISIBLE
                    binding?.pdfPageSizeSpinner?.adapter = pdfPageSizeAdapter
                    binding?.fileQualitySpinner?.visibility = View.VISIBLE
                    binding?.paddingNumberEditText?.visibility = View.VISIBLE
                    binding?.fileQualitySpinner?.adapter = premiumSpinnerAdapter
                }


            } else {
                binding?.fileQualityTextView?.text = getString(R.string.img_quality_text)
                binding?.fileQualityTextView?.visibility = View.VISIBLE
                binding?.pdfQualityDefaultText?.visibility = View.GONE
                binding?.paddingTextView?.visibility = View.GONE
                binding?.paddingPixelTextView?.visibility = View.GONE
                binding?.pdfPageSizeTextView?.visibility = View.GONE


                binding?.fileQualitySpinner?.adapter = imageQualityAdapter
                binding?.fileQualitySpinner?.visibility = View.VISIBLE


                binding?.pdfPageSizeSpinner?.visibility = View.GONE
                binding?.pdfPageSizeSpinner?.adapter = pdfPageSizeAdapter

                binding?.paddingNumberEditText?.visibility = View.GONE
            }
        } else {

            if (isPdfConversion || isImgIntoPdf()) {
                binding?.fileQualityTextView?.text = getString(R.string.pdf_quality_text)
                binding?.fileQualityTextView?.visibility = View.VISIBLE
                binding?.pdfQualityDefaultText?.visibility = View.VISIBLE


                binding?.fileQualitySpinner?.adapter = freeSpinnerAdapter
                binding?.fileQualitySpinner?.visibility = View.GONE

                if (isImgIntoPdf()) {
                    binding?.pdfPageSizeTextView?.visibility = View.VISIBLE
                    binding?.pdfPageSizeSpinner?.visibility = View.VISIBLE
                    binding?.pdfQualityDefaultText?.visibility = View.VISIBLE
                    binding?.pdfPageSizeSpinner?.adapter = pdfPageSizeAdapter
                    binding?.paddingTextView?.visibility = View.VISIBLE
                    binding?.paddingPixelTextView?.visibility = View.VISIBLE
                    binding?.paddingNumberEditText?.visibility = View.VISIBLE
                }

            } else {
                binding?.fileQualityTextView?.text = getString(R.string.img_quality_text)
                binding?.paddingTextView?.visibility = View.GONE
                binding?.pdfQualityDefaultText?.visibility = View.GONE
                binding?.fileQualityTextView?.visibility = View.VISIBLE
                binding?.paddingPixelTextView?.visibility = View.GONE
                binding?.pdfPageSizeTextView?.visibility = View.GONE


                binding?.fileQualitySpinner?.adapter = imageQualityAdapter
                binding?.fileQualitySpinner?.visibility = View.VISIBLE


                binding?.pdfPageSizeSpinner?.visibility = View.GONE
                binding?.pdfPageSizeSpinner?.adapter = pdfPageSizeAdapter

                binding?.paddingNumberEditText?.visibility = View.GONE
            }
        }
    }


}






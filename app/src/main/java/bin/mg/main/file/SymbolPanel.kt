package bin.mg.main.file

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class SymbolPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface OnSymbolClickListener {
        fun onSymbolClick(symbol: String)
    }

    private var symbolClickListener: OnSymbolClickListener? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var dragHandle: BottomSheetDragHandleView
    private lateinit var rowsContainer: LinearLayout
    
    private companion object {
        private val SYMBOL_ROWS = arrayOf(
            arrayOf("→", "/", "+", "-", "*", "=", "<"),
            arrayOf(">", "\"", "'", ";", "|", "\\", "_"),
            arrayOf("()", "[]", "{}", "...")
        )
        
        private val INSERT_ROWS = arrayOf(
            arrayOf(null, "/", "+", "-", "*", "=", "<"),
            arrayOf(">", "\"", "'", ";", "|", "\\", "_"),
            arrayOf("()", "[]", "{}", "...")
        )
        
        private const val ROW_HEIGHT_DP = 48
        private const val PEEK_HEIGHT_DP = 56
        private const val EXPANDED_RATIO = 0.4f
    }

    init {
        orientation = VERTICAL
        initView()
        setupBottomSheetBehavior()
        setupKeyboardInsetsListener()
    }

    private fun initView() {
        // Create drag handle
        dragHandle = BottomSheetDragHandleView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER_HORIZONTAL)
        }
        addView(dragHandle)

        // Create rows container
        rowsContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }
        addView(rowsContainer)

        // Create symbol rows
        val density = resources.displayMetrics.density
        SYMBOL_ROWS.forEachIndexed { rowIndex, _ ->
            val row = createSymbolRow(rowIndex, density)
            rowsContainer.addView(row)
            
            // Hide rows beyond first when collapsed
            if (rowIndex > 0) {
                row.visibility = GONE
            }
        }
    }

    private fun createSymbolRow(rowIndex: Int, density: Float): LinearLayout {
        val labels = SYMBOL_ROWS[rowIndex]
        val inserts = INSERT_ROWS[rowIndex]
        
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                (ROW_HEIGHT_DP * density).toInt()
            )
            
            labels.forEachIndexed { colIndex, label ->
                val button = TextView(context).apply {
                    text = label
                    setTextColor(0xFFCCCCCC.toInt())
                    textSize = 15f
                    typeface = android.graphics.Typeface.MONOSPACE
                    gravity = Gravity.CENTER
                    layoutParams = LayoutParams(
                        0,
                        LayoutParams.MATCH_PARENT,
                        1f
                    )
                    isClickable = true
                    isFocusable = true
                    
                    // Material design ripple effect
                    ViewCompat.setBackground(this, createRippleDrawable())
                    
                    setOnClickListener {
                        val symbol = when {
                            rowIndex == 0 && colIndex == 0 -> buildTabString()
                            else -> inserts[colIndex] ?: return@setOnClickListener
                        }
                        symbolClickListener?.onSymbolClick(symbol)
                    }
                }
                addView(button)
            }
        }
    }

    private fun createRippleDrawable() = MaterialShapeDrawable(
        ShapeAppearanceModel.builder()
            .setAllCorners(com.google.android.material.shape.CornerFamily.ROUNDED, 0f)
            .build()
    ).apply {
        fillColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        setTint(0x33FFFFFF.toInt())
    }

    private fun buildTabString(): String {
        val prefs = context.getSharedPreferences("editor_prefs", Context.MODE_PRIVATE)
        val useTabs = prefs.getBoolean("use_tabs", false)
        
        return if (useTabs) {
            "\t"
        } else {
            val tabSize = prefs.getInt("tab_size", 4)
            " ".repeat(tabSize)
        }
    }

    private fun setupBottomSheetBehavior() {
        bottomSheetBehavior = BottomSheetBehavior.from(this).apply {
            peekHeight = (PEEK_HEIGHT_DP * resources.displayMetrics.density).toInt()
            expandedOffset = 0
            isHideable = false
            isFitToContents = false
            halfExpandedRatio = EXPANDED_RATIO
            
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> showAllRows()
                        BottomSheetBehavior.STATE_COLLAPSED -> showFirstRowOnly()
                    }
                }
                
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // slideOffset: 0 = collapsed, 1 = expanded
                    updateRowsVisibility(slideOffset)
                }
            })
        }
    }

    private fun updateRowsVisibility(slideOffset: Float) {
        rowsContainer.children.forEachIndexed { index, view ->
            if (index > 0) { // Skip first row (index 0)
                val targetAlpha = slideOffset.coerceIn(0f, 1f)
                view.alpha = targetAlpha
                view.visibility = if (targetAlpha > 0.01f) VISIBLE else GONE
            }
        }
    }

    private fun showAllRows() {
        rowsContainer.children.forEach { it.visibility = VISIBLE }
    }

    private fun showFirstRowOnly() {
        rowsContainer.children.forEachIndexed { index, view ->
            view.visibility = if (index == 0) VISIBLE else GONE
        }
    }

    private fun setupKeyboardInsetsListener() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Position the panel above the keyboard
            val bottomPadding = imeInsets.bottom - systemBarsInsets.bottom
            if (bottomPadding > 0) {
                setPadding(paddingLeft, paddingTop, paddingRight, 0)
                bottomSheetBehavior.peekHeight = (PEEK_HEIGHT_DP * resources.displayMetrics.density).toInt()
            } else {
                setPadding(paddingLeft, paddingTop, paddingRight, systemBarsInsets.bottom)
            }
            
            insets
        }
    }

    fun setOnSymbolClickListener(listener: OnSymbolClickListener) {
        symbolClickListener = listener
    }

    fun expand() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun collapse() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun toggle() {
        when (bottomSheetBehavior.state) {
            BottomSheetBehavior.STATE_COLLAPSED -> expand()
            else -> collapse()
        }
    }

    fun isExpanded(): Boolean {
        return bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
               bottomSheetBehavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED
    }
}
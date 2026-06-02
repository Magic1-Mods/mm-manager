package bin.mg.main.app.dex.plus

import android.content.Context
import android.view.*
import android.widget.*
import androidx.annotation.NonNull
import androidx.recyclerview.widget.*
import androidx.viewpager.widget.PagerAdapter
import bin.mg.main.R
import bin.mg.main.app.dex.plus.clickeffect.T_a
import com.fastrecyclerview.FastScrollerRecyclerView
import java.util.*

class TabPage(
    private val ctx: Context,
    private val tabs: Array<String>,
    classes: List<String>?
) : PagerAdapter() {

    private val roots = mutableListOf<Array<Any?>>()
    private val visible = mutableListOf<Array<Any?>>()
    private var adapter: RecyclerView.Adapter<*>? = null

    init {
        updateClasses(classes)
    }

    fun updateClasses(classes: List<String>?) {
        if (classes == null) return
        roots.clear()

        for (cls in classes) {
            val s = if (cls.startsWith("L") && cls.endsWith(";")) cls.substring(1, cls.length - 1) else cls
            val pts = s.split("/").toTypedArray()
            var cur: MutableList<Array<Any?>> = roots
            var p: Array<Any?>? = null

            for (i in pts.indices) {
                val isC = i == pts.size - 1
                var found: Array<Any?>? = null

                for (n in cur) {
                    if (n[0] == pts[i] && n[1] == isC) {
                        found = n
                        break
                    }
                }

                if (found == null) {
                    found = arrayOf(
                        pts[i],
                        isC,
                        false,
                        p,
                        mutableListOf<Array<Any?>>()
                    )
                    cur.add(found)
                }
                p = found
                cur = found[4] as MutableList<Array<Any?>>
            }
        }

        process(roots)
        visible.clear()
        add(roots, visible, true)
        adapter?.notifyDataSetChanged()
    }

    override fun getCount(): Int = tabs.size

    override fun isViewFromObject(@NonNull view: View, @NonNull obj: Any): Boolean = view == obj

    override fun getItemPosition(@NonNull obj: Any): Int = POSITION_NONE

    override fun getPageTitle(position: Int): CharSequence = tabs[position]

    @NonNull
    override fun instantiateItem(@NonNull container: ViewGroup, position: Int): Any {
        if (position != 0) {
            val tv = TextView(ctx).apply {
                if (position == 1) setText(R.string.no_history_yet) else text = tabs[position]
                gravity = Gravity.CENTER
                textSize = 18f
                setPadding(0, 60, 0, 0)
            }
            container.addView(tv)
            return tv
        }

        val rv = FastScrollerRecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @NonNull
            override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(ctx).inflate(R.layout.item_class_tree, parent, false)
                val holder = object : RecyclerView.ViewHolder(v) {}

                T_a.apply(v, object : T_a.ClickAction {
                    override fun onClick(view: View) {
                        val pos = holder.adapterPosition
                        if (pos != RecyclerView.NO_POSITION && visible[pos][1] != true) {
                            toggle(pos, visible[pos], view.findViewById<ImageView>(R.id.expand_icon))
                        }
                    }
                })
                return holder
            }

            override fun onBindViewHolder(@NonNull holder: RecyclerView.ViewHolder, position: Int) {
                val v = holder.itemView
                val node = visible[position]
                val density = ctx.resources.displayMetrics.density

                val nameText = v.findViewById<TextView>(R.id.name_text)
                nameText.text = node[0] as String

                var depth = 0
                var pr = node[3] as Array<Any?>?
                while (pr != null) {
                    depth++
                    pr = pr[3] as Array<Any?>?
                }

                val lp = v.layoutParams as RecyclerView.LayoutParams
                lp.leftMargin = (depth * (25 * density)).toInt() + if (node[1] == true) (17 * density).toInt() else 0
                v.layoutParams = lp

                val expandIcon = v.findViewById<ImageView>(R.id.expand_icon)
                val pkgIcon = v.findViewById<View>(R.id.type_icon)
                val classIcon = v.findViewById<View>(R.id.class_icon)

                if (node[1] == true) {
                    expandIcon.animate().cancel()
                    expandIcon.visibility = View.GONE
                    pkgIcon.visibility = View.GONE
                    classIcon.visibility = View.VISIBLE
                } else {
                    expandIcon.visibility = View.VISIBLE
                    pkgIcon.visibility = View.VISIBLE
                    classIcon.visibility = View.GONE
                    expandIcon.animate().cancel()
                    expandIcon.rotation = if (node[2] == true) 40f else 0f
                }
            }

            override fun getItemCount(): Int = visible.size
        }

        rv.adapter = adapter
        container.addView(rv)
        return rv
    }

    override fun destroyItem(@NonNull container: ViewGroup, position: Int, @NonNull obj: Any) {
        if (obj is RecyclerView) adapter = null
        container.removeView(obj as View)
    }

    private fun process(list: MutableList<Array<Any?>>) {
        list.sortWith(Comparator { a, b ->
            val aIsClass = a[1] as Boolean
            val bIsClass = b[1] as Boolean
            if (aIsClass != bIsClass) {
                if (aIsClass) 1 else -1
            } else {
                (a[0] as String).compareTo(b[0] as String, ignoreCase = true)
            }
        })

        for (n in list) {
            if (n[1] != true) {
                var children = n[4] as MutableList<Array<Any?>>
                while (children.size == 1 && children[0][1] != true) {
                    val child = children[0]
                    n[0] = (n[0] as String) + "." + child[0]
                    n[4] = child[4]
                    children = n[4] as MutableList<Array<Any?>>
                    for (x in children) {
                        x[3] = n
                    }
                }
                process(children)
            }
        }
    }

    private fun add(input: List<Array<Any?>>, output: MutableList<Array<Any?>>, checkExpanded: Boolean) {
        for (n in input) {
            output.add(n)
            if (!checkExpanded || n[2] == true) {
                add(n[4] as List<Array<Any?>>, output, checkExpanded)
            }
        }
    }

    private fun toggle(pos: Int, node: Array<Any?>, expandIcon: ImageView) {
        val expanded = !(node[2] as Boolean)
        node[2] = expanded

        expandIcon.animate().cancel()
        expandIcon.animate().rotation(if (expanded) 40f else 0f).duration = 180

        if (expanded) {
            val inserts = mutableListOf<Array<Any?>>()
            add(node[4] as List<Array<Any?>>, inserts, true)
            visible.addAll(pos + 1, inserts)
            adapter?.notifyItemRangeInserted(pos + 1, inserts.size)
        } else {
            val depth = generateSequence(node[3] as Array<Any?>?) { it[3] as Array<Any?>? }.count()
            var count = 0
            for (i in pos + 1 until visible.size) {
                val cd = generateSequence(visible[i][3] as Array<Any?>?) { it[3] as Array<Any?>? }.count()
                if (cd <= depth) break
                count++
            }
            visible.subList(pos + 1, pos + 1 + count).clear()
            adapter?.notifyItemRangeRemoved(pos + 1, count)
        }
    }
}
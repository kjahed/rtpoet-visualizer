package ca.jahed.rtpoet.visualizer

import ca.jahed.rtpoet.rtmodel.RTElement
import com.mxgraph.layout.mxCompactTreeLayout
import com.mxgraph.model.mxICell
import com.mxgraph.swing.handler.mxKeyboardHandler
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxConstants
import org.jgrapht.Graph
import org.jgrapht.ext.JGraphXAdapter
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.awt.Color
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.lang.reflect.Field
import java.util.*
import javax.swing.JFrame
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object RTVisualizer {
    fun draw(root: RTElement, ignore: List<Any> = listOf()) {
        val frame = JFrame()
        frame.defaultCloseOperation = JFrame.HIDE_ON_CLOSE

        val graph: Graph<Any, LabeledEdge> = DefaultDirectedGraph(LabeledEdge::class.java)
        val stack = Stack<Any>()
        val visited = HashSet<Any>()

        stack.add(root)
        visited.add(root)
        graph.addVertex(root)

        while (!stack.isEmpty()) {
            val current = stack.pop()
            val fields = mutableListOf<Field>()
            fields.addAll(current.javaClass.declaredFields)

            var c: Class<*>? = current.javaClass.superclass
            while (c != null) {
                fields.addAll(listOf(*c.declaredFields))
                c = c.superclass
            }

            for (field in fields.filter { it !in ignore }) {
                field.isAccessible = true
                val obj = field[current]

                if (obj == null
                    || obj == current
                    || obj.javaClass in ignore
                )
                    continue

                if (obj is RTElement || obj is List<*>) {
                    if (obj is List<*> && obj.isEmpty())
                        continue

                    graph.addVertex(obj)
                    graph.addEdge(current, obj,
                        LabeledEdge(field.name))

                    if (obj is List<*>) {
                        for ((index, item) in obj.withIndex()) {
                            if (item == null || item.javaClass in ignore)
                                continue

                            graph.addVertex(item)
                            graph.addEdge(obj, item,
                                LabeledEdge("${field.name}[$index]"))

                            if (!visited.contains(item)) {
                                visited.add(item)
                                stack.add(item)
                            }
                        }
                    } else {
                        if (!visited.contains(obj)) {
                            visited.add(obj)
                            stack.add(obj)
                        }
                    }
                }
            }
        }

        val jgxAdapter = JGraphXAdapter(graph)
        val component = mxGraphComponent(jgxAdapter)
        mxKeyboardHandler(component)

        component.viewport.background = Color.white
        jgxAdapter.cellToVertexMap.forEach { (mxICell: mxICell, _: Any?) ->
            component.graph.updateCellSize(mxICell)
            mxICell.geometry.width = mxICell.geometry.width + 20
        }

        val labeledEdge: MutableMap<String, Any> = HashMap(component.graph.stylesheet.defaultEdgeStyle)
        labeledEdge[mxConstants.STYLE_VERTICAL_LABEL_POSITION] = "bottom"
        labeledEdge[mxConstants.STYLE_VERTICAL_ALIGN] = "bottom"
        component.graph.stylesheet.putCellStyle("defaultEdge", labeledEdge)
        component.graph.refresh()

        val layout = mxCompactTreeLayout(jgxAdapter)
        layout.levelDistance = 75
        layout.setGroupPadding(75)
        layout.nodeDistance = 25
        layout.isHorizontal = false
        layout.execute(jgxAdapter.defaultParent)

        frame.contentPane.add(component)
        frame.pack()
        frame.isVisible = true

        val lock = Object()
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(arg0: WindowEvent) {
                synchronized(lock) {
                    frame.isVisible = false
                    lock.notify()
                }
            }
        })

        synchronized(lock) {
            while (frame.isVisible) {
                try {
                    lock.wait()
                } catch (e: InterruptedException) {
                    // ignored
                }
            }
        }
    }

    internal class LabeledEdge(private val label: String) : DefaultEdge() {
        override fun toString(): String {
            return label
        }
    }
}
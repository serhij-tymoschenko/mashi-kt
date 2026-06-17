package com.mashiverse.images.converters

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfPoint
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object SvgProcessor {

    init {
        // Initialize OpenCV binaries automatically for JVM
        nu.pattern.OpenCV.loadLocally()
    }

    fun processSvg(inputBytes: ByteArray): ByteArray {
        // Parse SVG String into XML DOM Document
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(ByteArrayInputStream(inputBytes))
        val root = doc.documentElement

        // --- 1. Map every Mask ID to its calculated path data ---
        val maskDefinitions = mutableMapOf<String, String>()
        val masks = doc.getElementsByTagNameNS("http://www.w3.org/2000/svg", "mask")

        for (i in 0 until masks.length) {
            val maskElem = masks.item(i) as Element
            val maskId = maskElem.getAttribute("id") ?: continue
            var combinedPathD = ""

            val images = maskElem.getElementsByTagNameNS("http://www.w3.org/2000/svg", "image")
            for (j in 0 until images.length) {
                val imgElem = images.item(j) as Element
                var href = imgElem.getAttributeNS("http://www.w3.org/1999/xlink", "href")
                if (href.isEmpty()) href = imgElem.getAttribute("href")

                if (!href.contains("base64,")) continue

                val img = try {
                    val b64Str = href.substringAfter("base64,").replace("\\s".toRegex(), "")
                    val decodedBytes = Base64.getDecoder().decode(b64Str)
                    val matOfByte = MatOfByte(*decodedBytes)
                    Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED)
                } catch (e: Exception) {
                    continue
                }

                if (img == null || img.empty()) continue

                val maskBits = Mat()
                if (img.channels() == 4) {
                    // Extract Alpha Channel
                    Core.extractChannel(img, maskBits, 3)
                } else {
                    Imgproc.cvtColor(img, maskBits, Imgproc.COLOR_BGR2GRAY)
                }

                val thresh = Mat()
                Imgproc.threshold(maskBits, thresh, 127.0, 255.0, Imgproc.THRESH_BINARY)

                val contours = mutableListOf<MatOfPoint>()
                val hierarchy = Mat()
                Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

                val imgX = imgElem.getAttribute("x").toDoubleOrNull() ?: 0.0
                val imgY = imgElem.getAttribute("y").toDoubleOrNull() ?: 0.0

                for (contour in contours) {
                    if (Imgproc.contourArea(contour) < 10) continue
                    val points = contour.toArray()
                    if (points.isEmpty()) continue

                    combinedPathD += "M ${points[0].x + imgX} ${points[0].y + imgY} "
                    for (k in 1 until points.size) {
                        combinedPathD += "L ${points[k].x + imgX} ${points[k].y + imgY} "
                    }
                    combinedPathD += "Z "
                }
            }

            if (combinedPathD.isNotBlank()) {
                maskDefinitions["url(#$maskId)"] = combinedPathD.trim()
            }
        }

        // --- 2. Replace masked elements with vector paths ---
        val allNodes = root.getElementsByTagName("*")
        val nodesToProcessMasks = mutableListOf<Element>()
        for (i in 0 until allNodes.length) {
            val node = allNodes.item(i) as Element
            if (node.hasAttribute("mask") && maskDefinitions.containsKey(node.getAttribute("mask"))) {
                nodesToProcessMasks.add(node)
            }
        }

        for (child in nodesToProcessMasks) {
            val maskAttr = child.getAttribute("mask")
            val pathD = maskDefinitions[maskAttr] ?: continue

            var fillColor = child.getAttribute("fill")
            var opacity = child.getAttribute("opacity").ifEmpty { child.getAttribute("fill-opacity") }

            if (fillColor.isEmpty() || fillColor == "none") {
                val innerElements = child.getElementsByTagName("*")
                for (k in 0 until innerElements.length) {
                    val inner = innerElements.item(k) as Element
                    if (inner.hasAttribute("fill")) {
                        fillColor = inner.getAttribute("fill")
                        if (opacity.isEmpty()) {
                            opacity = inner.getAttribute("opacity").ifEmpty { inner.getAttribute("fill-opacity") }
                        }
                        break
                    }
                }
            }

            val newPath = doc.createElementNS("http://www.w3.org/2000/svg", "path").apply {
                setAttribute("d", pathD)
                setAttribute("fill", fillColor.ifEmpty { "#000000" })
                setAttribute("fill-rule", "evenodd")
                if (opacity.isNotEmpty()) setAttribute("opacity", opacity)
                if (child.hasAttribute("transform")) setAttribute("transform", child.getAttribute("transform"))
            }

            child.parentNode.replaceChild(newPath, child)
        }

        // --- 3. Replace <use> tags with actual elements ---
        val defsMap = mutableMapOf<String, Element>()
        val elementsWithId = root.getElementsByTagName("*")
        for (i in 0 until elementsWithId.length) {
            val el = elementsWithId.item(i) as Element
            val id = el.getAttribute("id")
            if (id.isNotEmpty()) defsMap["#$id"] = el
        }

        val useElements = root.getElementsByTagNameNS("http://www.w3.org/2000/svg", "use")
        val useList = mutableListOf<Element>()
        for (i in 0 until useElements.length) useList.add(useElements.item(i) as Element)

        for (useElem in useList) {
            var href = useElem.getAttributeNS("http://www.w3.org/1999/xlink", "href")
            if (href.isEmpty()) href = useElem.getAttribute("href")

            val targetNode = defsMap[href]
            if (targetNode != null && useElem.parentNode != null) {
                val referencedNode = targetNode.cloneNode(true) as Element

                val ux = useElem.getAttribute("x").ifEmpty { "0" }
                val uy = useElem.getAttribute("y").ifEmpty { "0" }

                if (ux != "0" || uy != "0") {
                    val existTr = referencedNode.getAttribute("transform")
                    referencedNode.setAttribute("transform", "translate($ux, $uy) $existTr".trim())
                }

                // Inherit attributes from <use> node
                val useAttrs = useElem.attributes
                for (k in 0 until useAttrs.length) {
                    val attr = useAttrs.item(k)
                    val name = attr.nodeName
                    if (name !in listOf("href", "xlink:href", "x", "y", "id")) {
                        referencedNode.setAttribute(name, attr.nodeValue)
                    }
                }

                useElem.parentNode.replaceChild(referencedNode, useElem)
            }
        }

        // --- 4. Cleanup defs ---
        val defsList = root.getElementsByTagNameNS("http://www.w3.org/2000/svg", "defs")
        for (i in 0 until defsList.length) {
            val defs = defsList.item(i) as Element

            val childImages = defs.getElementsByTagNameNS("http://www.w3.org/2000/svg", "image")
            for (k in childImages.length - 1 downTo 0) {
                val img = childImages.item(k)
                if (img.parentNode == defs) defs.removeChild(img)
            }

            val childMasks = defs.getElementsByTagNameNS("http://www.w3.org/2000/svg", "mask")
            for (k in childMasks.length - 1 downTo 0) {
                val mask = childMasks.item(k)
                if (mask.parentNode == defs) defs.removeChild(mask)
            }
        }

        // Remove clipPathUnits attributes
        val clipPaths = root.getElementsByTagNameNS("http://www.w3.org/2000/svg", "clipPath")
        for (i in 0 until clipPaths.length) {
            (clipPaths.item(i) as Element).removeAttribute("clipPathUnits")
        }

        // --- 5. Finalize & Transform to pretty Byte Array ---
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        }

        val outStream = ByteArrayOutputStream()
        transformer.transform(DOMSource(doc), StreamResult(outStream))
        return outStream.toByteArray()
    }
}
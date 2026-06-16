package com.mashiverse.images.helpers

fun removeSvgMetadata(data: ByteArray): ByteArray {
    var svg = String(data, Charsets.UTF_8)
    svg = svg.replace(Regex("""^\s*<\?xml[^>]*\?>""", RegexOption.IGNORE_CASE), "")
    svg = svg.replace(Regex("""<!DOCTYPE[^>]*>""", RegexOption.IGNORE_CASE), "")
    svg = svg.replace(Regex("""serif:[^"]*"[^"]*""""), "")
    svg = svg.replace(Regex("""<sodipodi:namedview\b[^>]*?>[\s\S]*?<\/sodipodi:namedview>"""), "")
    svg = svg.replace(Regex("""<sodipodi:namedview\b[^>]*/>"""), "")
    svg = svg.replace(Regex("""\s*sodipodi:[^=]+="[^"]*""""), "")
    svg = svg.replace(Regex("""\s*inkscape:[^=]+="[^"]*""""), "")
    svg = svg.replace(Regex("""<SODI[^>]*>"""), "")
    svg = svg.replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), "")

    return svg
        .trim()
        .replace("\n", "")
        .toByteArray(Charsets.UTF_8)
}

fun replaceColors(
    data: ByteArray,
    body: String,
    eyes: String,
    hair: String
): ByteArray {
    var svg = String(data, Charsets.UTF_8)

    svg = svg.replace(
        Regex("""#00ff00|#0f0\b|\blime\b|rgb\s*\(\s*0\s*,\s*255\s*,\s*0\s*\)""", RegexOption.IGNORE_CASE),
        body
    )

    svg = svg.replace(
        Regex("""#ffff00|#ff0\b|\byellow\b|rgb\s*\(\s*255\s*,\s*255\s*,\s*0\s*\)""", RegexOption.IGNORE_CASE),
        eyes
    )

    svg = svg.replace(
        Regex("""#0000ff|#00f\b|\bblue\b|rgb\s*\(\s*0\s*,\s*0\s*,\s*255\s*\)""", RegexOption.IGNORE_CASE),
        hair
    )

    return svg
        .trim()
        .replace("\n", "")
        .toByteArray(Charsets.UTF_8)
}
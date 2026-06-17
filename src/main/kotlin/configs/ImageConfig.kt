package com.mashiverse.configs

// GIF
const val GIF_WIDTH = 552
const val GIF_HEIGHT = 736
const val GIF_TRAIT_WIDTH = 380
const val GIF_TRAIT_HEIGHT = 600

const val FRAME_DELAY_MS = 30
const val CAPTURE_FPS = 33.33
const val PLAYBACK_FPS = 33.33
const val DURATIONS_LIMIT_SEC = 5


// PNG
const val PNG_WIDTH = 552 * 2
const val PNG_HEIGHT = 736 * 2
const val PNG_TRAIT_WIDTH = 380 * 2
const val PNG_TRAIT_HEIGHT = 600 * 2

// Other
const val ANIM_STEP = 0.03
const val SEMAPHORE_LIMIT = 10

val LAYER_ORDER = listOf(
    "background",
    "hair_back",
    "cape",
    "bottom",
    "upper",
    "head",
    "eyes",
    "hair_front",
    "hat",
    "left_accessory",
    "right_accessory"
)
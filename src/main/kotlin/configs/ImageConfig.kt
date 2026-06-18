package com.mashiverse.configs


import kotlinx.coroutines.sync.Semaphore

// GIF
const val GIF_WIDTH = 552
const val GIF_HEIGHT = 736
const val GIF_TRAIT_WIDTH = 380
const val GIF_TRAIT_HEIGHT = 600

const val FRAME_DELAY_MS = 33
const val CAPTURE_FPS = 30
const val PLAYBACK_FPS = 15
const val DURATION_LIMIT_SEC = 5
const val ANIM_STEP = 0.033

// PNG
const val PNG_WIDTH = 552 * 2
const val PNG_HEIGHT = 736 * 2
const val PNG_TRAIT_WIDTH = 380 * 2
const val PNG_TRAIT_HEIGHT = 600 * 2

// Other
const val MAX_GENERATIONS = 10
val imageSemaphore = Semaphore(MAX_GENERATIONS)
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
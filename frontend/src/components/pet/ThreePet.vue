<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

const props = defineProps({
  species: { type: String, default: '猫' },
  color: { type: String, default: '#E8A359' },
  modelConfig: { type: Object, default: null },
  photoUrl: { type: String, default: '' },
  petName: { type: String, default: '' },
  albumPhotos: { type: Array, default: () => [] }
})
const emit = defineEmits(['interaction', 'laserCatch', 'laserComplete'])
const container = ref(null)
const statusMsg = ref('')
const mood = ref(60)
const hunger = ref(50)
const energy = ref(70)
const feedingCount = ref(0)
const playingCount = ref(0)

let scene, camera, renderer, controls, clock
let petGroup, headGroup, tail, bodyMesh
let foodBowl, toyRoot
let particleGroup
let animFrame, cur = 'idle', t0 = 0
let action = null
let blinkTimer = 3
let kibbles = [], toyBalls = []
let frontPaws = []
let disposed = false
let resizeObserver = null
const BASE_Y = 0.08

// Laser pointer state
let laserActive = false
let laserDot = null
let laserTarget = new THREE.Vector3(0, 0.01, 0)
let laserCatches = 0
const LASER_CATCHES_NEEDED = 5

const C = {
  paper: 0xF5EDE0, paperDark: 0xE8D8C0, wood: 0xC4A87C, woodLight: 0xD4B896,
  accent: 0x5C7A5E, spot: 0xC17B60, gold: 0xDDBB77,
  rug: 0xE8D5C4, rugRing: 0xD4BFAA, bowl: 0xEEEEEE, bowlInner: 0xAA8866,
  kibble: 0x9B7B4A, yarn: 0xFF88AA, leaf: 0x55AA44, pot: 0xCC7755,
}

function pc() { return new THREE.Color(props.modelConfig?.baseColor || props.color) }
function dc() { return new THREE.Color(props.modelConfig?.accentColor || props.color).multiplyScalar(0.78) }
function clamp01(v) { return Math.max(0, Math.min(1, v)) }
function smooth(v) {
  const x = clamp01(v)
  return x * x * (3 - 2 * x)
}
function mix(a, b, t) { return a + (b - a) * t }

// ════════════════════════════════════════
//  STAGE
// ════════════════════════════════════════
function createStage() {
  const g = new THREE.Group()
  const platform = new THREE.Mesh(
    new THREE.CylinderGeometry(1.3, 1.35, 0.12, 48),
    new THREE.MeshStandardMaterial({ color: C.wood, roughness: 0.5, metalness: 0.05 })
  )
  platform.position.y = -0.06
  platform.receiveShadow = true
  platform.castShadow = true
  g.add(platform)

  const platformTop = new THREE.Mesh(
    new THREE.CylinderGeometry(1.29, 1.3, 0.01, 48),
    new THREE.MeshStandardMaterial({ color: C.woodLight, roughness: 0.4 })
  )
  platformTop.position.y = 0.005
  platformTop.receiveShadow = true
  g.add(platformTop)
  const edge = new THREE.Mesh(
    new THREE.TorusGeometry(1.3, 0.012, 8, 64),
    new THREE.MeshStandardMaterial({ color: 0xA08060, roughness: 0.3, metalness: 0.4 })
  )
  edge.rotation.x = Math.PI / 2; edge.position.y = 0.065; g.add(edge)
  const rug = new THREE.Mesh(
    new THREE.CylinderGeometry(0.55, 0.58, 0.012, 40),
    new THREE.MeshStandardMaterial({ color: C.rug, roughness: 0.9 })
  )
  rug.scale.set(1, 1, 0.7); rug.position.y = 0.065; rug.receiveShadow = true; g.add(rug)
  for (let i = 1; i <= 3; i++) {
    const ring = new THREE.Mesh(
      new THREE.TorusGeometry(0.12 + i * 0.1, 0.006, 6, 48),
      new THREE.MeshStandardMaterial({ color: C.rugRing, roughness: 0.6 })
    )
    ring.rotation.x = -Math.PI / 2; ring.position.y = 0.072; g.add(ring)
  }
  return g
}

// ════════════════════════════════════════
//  FOOD BOWL
// ════════════════════════════════════════
function createFoodBowl() {
  const g = new THREE.Group()
  g.add(new THREE.Mesh(
    new THREE.CylinderGeometry(0.13, 0.08, 0.07, 24),
    new THREE.MeshStandardMaterial({ color: C.bowl, roughness: 0.15, metalness: 0.6 })
  ))
  const inner = new THREE.Mesh(
    new THREE.CylinderGeometry(0.11, 0.1, 0.015, 24),
    new THREE.MeshStandardMaterial({ color: C.bowlInner, roughness: 0.7 })
  )
  inner.position.y = 0.035; g.add(inner)
  for (let i = 0; i < 10; i++) {
    const kb = new THREE.Mesh(
      new THREE.SphereGeometry(0.014, 5, 3),
      new THREE.MeshStandardMaterial({ color: C.kibble, roughness: 0.6 })
    )
    kb.position.set((Math.random() - 0.5) * 0.08, 0.04, (Math.random() - 0.5) * 0.08)
    kb.visible = false; g.add(kb); kibbles.push(kb)
  }
  g.position.set(0.5, 0.07, 0.35)
  return g
}

// ════════════════════════════════════════
//  TOY
// ════════════════════════════════════════
function createToy() {
  const g = new THREE.Group()
  const colors = [0xFF5544, 0x44AAFF, 0xFFCC33]
  for (let i = 0; i < 3; i++) {
    const ball = new THREE.Group()
    ball.add(new THREE.Mesh(
      new THREE.SphereGeometry(0.04, 16, 12),
      new THREE.MeshStandardMaterial({ color: colors[i], roughness: 0.1, metalness: 0.2 })
    ))
    const ring = new THREE.Mesh(
      new THREE.TorusGeometry(0.03, 0.008, 8, 12),
      new THREE.MeshStandardMaterial({ color: 0xFFFFFF, roughness: 0.3 })
    )
    ring.rotation.x = Math.random() * Math.PI; ball.add(ring)
    ball.position.set(-0.35 + Math.random() * 0.25, 0.06, -0.25 + Math.random() * 0.15)
    ball.userData = { origPos: ball.position.clone() }
    g.add(ball); toyBalls.push(ball)
  }
  return g
}

// ════════════════════════════════════════
//  DECORATIONS
// ════════════════════════════════════════
function createDecorations() {
  const g = new THREE.Group()
  const pot = new THREE.Group()
  pot.add(new THREE.Mesh(
    new THREE.CylinderGeometry(0.06, 0.04, 0.1, 16),
    new THREE.MeshStandardMaterial({ color: C.pot, roughness: 0.5 })
  ))
  for (let i = 0; i < 4; i++) {
    const leaf = new THREE.Mesh(
      new THREE.SphereGeometry(0.035, 8, 6),
      new THREE.MeshStandardMaterial({ color: C.leaf + Math.floor(Math.random() * 0x221100), roughness: 0.6 })
    )
    leaf.scale.set(1, 0.3, 0.5)
    leaf.position.y = 0.07 + Math.random() * 0.03
    leaf.rotation.set(Math.random() * 0.5, i * Math.PI * 0.5, Math.random() * 0.3)
    pot.add(leaf)
  }
  pot.position.set(-0.6, 0.1, -0.1); g.add(pot)
  const yarn = new THREE.Mesh(
    new THREE.SphereGeometry(0.035, 12, 8),
    new THREE.MeshStandardMaterial({ color: C.yarn, roughness: 0.8 })
  )
  yarn.position.set(0.25, 0.06, -0.35); g.add(yarn)
  if (props.petName) {
    const cv = document.createElement('canvas'); cv.width = 256; cv.height = 64
    const ctx = cv.getContext('2d')
    ctx.fillStyle = '#C4A87C'; ctx.fillRect(0, 0, 256, 64)
    ctx.fillStyle = '#FFFFFF'; ctx.font = 'bold 22px sans-serif'
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
    ctx.fillText(props.petName, 128, 32)
    const tex = new THREE.CanvasTexture(cv); tex.colorSpace = THREE.SRGBColorSpace
    const plate = new THREE.Mesh(
      new THREE.PlaneGeometry(0.3, 0.08),
      new THREE.MeshStandardMaterial({ map: tex, roughness: 0.25, side: THREE.DoubleSide })
    )
    plate.position.set(0, -0.35, 0.9); plate.rotation.x = -0.3; g.add(plate)
  }
  return g
}

// ════════════════════════════════════════
//  CAT BED
// ════════════════════════════════════════
function createCatBed() {
  const g = new THREE.Group()
  const bedMat = new THREE.MeshStandardMaterial({ color: 0xD4BFAA, roughness: 0.85 })
  const rimMat = new THREE.MeshStandardMaterial({ color: 0xBFA88A, roughness: 0.7 })

  // Bed base (oval cushion)
  const base = new THREE.Mesh(
    new THREE.CylinderGeometry(0.18, 0.2, 0.05, 24),
    bedMat
  )
  base.scale.set(1.3, 1, 1)
  base.position.y = 0.025
  base.receiveShadow = true
  g.add(base)

  // Rim
  const rim = new THREE.Mesh(
    new THREE.TorusGeometry(0.19, 0.03, 8, 24),
    rimMat
  )
  rim.scale.set(1.3, 1, 1)
  rim.rotation.x = Math.PI / 2
  rim.position.y = 0.05
  g.add(rim)

  // Soft inner
  const inner = new THREE.Mesh(
    new THREE.CylinderGeometry(0.15, 0.16, 0.02, 20),
    new THREE.MeshStandardMaterial({ color: 0xEDE4D3, roughness: 0.9 })
  )
  inner.scale.set(1.2, 1, 1)
  inner.position.y = 0.055
  g.add(inner)

  g.position.set(-0.65, 0.0, 0.45)
  g.rotation.y = 0.3
  return g
}

// ════════════════════════════════════════
//  PHOTO FRAMES
// ════════════════════════════════════════
function createPhotoFrames() {
  const g = new THREE.Group()
  const frameMat = new THREE.MeshStandardMaterial({ color: 0xC4A87C, roughness: 0.4 })
  const photos = props.albumPhotos || []
  const maxFrames = Math.min(photos.length, 3)

  for (let i = 0; i < maxFrames; i++) {
    const frame = new THREE.Group()
    // Frame border
    const border = new THREE.Mesh(
      new THREE.BoxGeometry(0.18, 0.14, 0.008),
      frameMat
    )
    border.castShadow = true
    frame.add(border)

    // Photo surface
    const photoGeo = new THREE.PlaneGeometry(0.15, 0.11)
    const photoMat = new THREE.MeshStandardMaterial({ color: 0xFAF5EE, roughness: 0.5 })

    // Try to load photo texture
    if (photos[i]) {
      const loader = new THREE.TextureLoader()
      loader.load(photos[i], (tex) => {
        tex.colorSpace = THREE.SRGBColorSpace
        photoMat.map = tex
        photoMat.needsUpdate = true
      }, undefined, () => {})
    }

    const photoMesh = new THREE.Mesh(photoGeo, photoMat)
    photoMesh.position.z = 0.005
    frame.add(photoMesh)

    // Position along back arc
    const angle = -0.4 + i * 0.35
    const radius = 1.15
    frame.position.set(
      Math.sin(angle) * radius,
      0.35 + i * 0.08,
      -Math.cos(angle) * radius
    )
    frame.rotation.y = angle
    g.add(frame)
  }

  // If no photos, show placeholder frames
  if (maxFrames === 0) {
    for (let i = 0; i < 2; i++) {
      const frame = new THREE.Mesh(
        new THREE.BoxGeometry(0.16, 0.12, 0.006),
        frameMat
      )
      const angle = -0.2 + i * 0.4
      const radius = 1.15
      frame.position.set(
        Math.sin(angle) * radius,
        0.38 + i * 0.06,
        -Math.cos(angle) * radius
      )
      frame.rotation.y = angle
      frame.castShadow = true
      g.add(frame)
    }
  }

  return g
}

// ════════════════════════════════════════
//  LASER POINTER
// ════════════════════════════════════════
function createLaserDot() {
  const dot = new THREE.Mesh(
    new THREE.SphereGeometry(0.018, 12, 8),
    new THREE.MeshBasicMaterial({ color: 0xFF2222, transparent: true, opacity: 0.9 })
  )
  dot.position.set(0, 0.01, 0)
  dot.visible = false
  return dot
}

function startLaser() {
  if (laserActive) return
  laserActive = true
  laserCatches = 0
  if (laserDot) laserDot.visible = true
  moveLaserToRandom()
}

function stopLaser() {
  laserActive = false
  if (laserDot) laserDot.visible = false
  cur = 'idle'
  action = null
}

function moveLaserToRandom() {
  const angle = Math.random() * Math.PI * 2
  const radius = 0.3 + Math.random() * 0.5
  laserTarget.set(Math.sin(angle) * radius, 0.01, Math.cos(angle) * radius)
  if (laserDot) laserDot.position.copy(laserTarget)
}

function checkLaserCatch() {
  if (!laserActive || !petGroup) return
  const dx = petGroup.position.x - laserTarget.x
  const dz = petGroup.position.z - laserTarget.z
  const dist = Math.sqrt(dx * dx + dz * dz)
  if (dist < 0.12) {
    laserCatches++
    emit('laserCatch', laserCatches)
    spawnPaws(petGroup.position)
    if (laserCatches >= LASER_CATCHES_NEEDED) {
      emit('laserComplete', laserCatches)
      stopLaser()
    } else {
      moveLaserToRandom()
    }
  }
}

// ════════════════════════════════════════
//  CAT MODEL
// ════════════════════════════════════════
function createCatModel() {
  const g = new THREE.Group()
  frontPaws = []
  const mat = new THREE.MeshStandardMaterial({ color: pc(), roughness: 0.6 })
  const dark = new THREE.MeshStandardMaterial({ color: dc(), roughness: 0.5 })
  const markMat = new THREE.MeshStandardMaterial({ color: dc(), roughness: 0.62 })
  const cream = new THREE.MeshStandardMaterial({ color: 0xFFF3DA, roughness: 0.65 })
  const noseMat = new THREE.MeshStandardMaterial({ color: 0xFF9999, roughness: 0.4 })
  const eyeMat = new THREE.MeshStandardMaterial({ color: 0x23201C, roughness: 0.18, metalness: 0.05 })
  const blushMat = new THREE.MeshStandardMaterial({ color: 0xEAA98E, roughness: 0.7, transparent: true, opacity: 0.7 })

  bodyMesh = new THREE.Mesh(new THREE.SphereGeometry(0.23, 32, 18), mat)
  bodyMesh.scale.set(0.92, 0.68, 1.42)
  bodyMesh.userData.baseScale = bodyMesh.scale.clone()
  bodyMesh.position.set(0, 0.2, -0.02)
  bodyMesh.castShadow = true
  g.add(bodyMesh)

  if (props.modelConfig?.whiteMarkings) {
    const chest = new THREE.Mesh(new THREE.SphereGeometry(0.105, 18, 10), cream)
    chest.scale.set(0.9, 0.72, 0.35)
    chest.position.set(0, 0.205, 0.205)
    chest.rotation.x = -0.18
    g.add(chest)
  }

  const pattern = props.modelConfig?.pattern || 'soft'
  if (pattern !== 'solid') {
    for (let i = 0; i < 3; i++) {
      const stripe = new THREE.Mesh(new THREE.BoxGeometry(0.13 - i * 0.018, 0.006, 0.012), markMat)
      stripe.position.set(0, 0.36 - i * 0.028, 0.23)
      stripe.rotation.x = -0.28
      g.add(stripe)
    }
    for (const side of [-1, 1]) {
      for (let i = 0; i < 2; i++) {
        const sideMark = new THREE.Mesh(new THREE.BoxGeometry(0.09, 0.008, 0.014), markMat)
        sideMark.position.set(side * 0.18, 0.24 - i * 0.035, 0.06 - i * 0.06)
        sideMark.rotation.set(0.2, side * 0.25, side * 0.48)
        g.add(sideMark)
      }
    }
  }

  headGroup = new THREE.Group()
  const head = new THREE.Mesh(new THREE.SphereGeometry(0.145, 32, 18), mat)
  head.scale.set(1.02, 0.88, 1.08)
  head.castShadow = true; headGroup.add(head)

  for (const s of [-1, 1]) {
    const ear = new THREE.Mesh(new THREE.ConeGeometry(0.043, 0.11, 4), mat)
    ear.position.set(s * 0.085, 0.13, -0.008)
    ear.rotation.set(0.14, 0, s * -0.3)
    ear.castShadow = true
    headGroup.add(ear)
    const ei = new THREE.Mesh(new THREE.ConeGeometry(0.024, 0.055, 3),
      new THREE.MeshStandardMaterial({ color: 0xFFCCBB, roughness: 0.5 }))
    ei.position.set(s * 0.095, 0.128, 0.025)
    ei.rotation.set(0.18, 0, s * -0.25)
    headGroup.add(ei)
  }

  const muzzle = new THREE.Mesh(new THREE.SphereGeometry(0.05, 18, 10), cream)
  muzzle.scale.set(1.38, 0.58, 0.5)
  muzzle.position.set(0, -0.025, 0.13)
  headGroup.add(muzzle)

  for (const s of [-1, 1]) {
    const ep = new THREE.Mesh(new THREE.SphereGeometry(0.021, 14, 12), eyeMat)
    ep.scale.set(0.78, 1, 0.55)
    ep.position.set(s * 0.052, 0.035, 0.126)
    headGroup.add(ep)
    const es = new THREE.Mesh(new THREE.SphereGeometry(0.0045, 6, 6),
      new THREE.MeshStandardMaterial({ color: 0xFFFFFF, roughness: 0.1 }))
    es.position.set(s * 0.047, 0.043, 0.14)
    headGroup.add(es)
    const cheek = new THREE.Mesh(new THREE.SphereGeometry(0.018, 8, 6), blushMat)
    cheek.scale.set(1, 0.45, 0.35)
    cheek.position.set(s * 0.078, -0.02, 0.126)
    headGroup.add(cheek)
  }
  const eyelids = []
  for (const s of [-1, 1]) {
    const lid = new THREE.Mesh(new THREE.SphereGeometry(0.023, 10, 8), mat)
    lid.scale.set(0.85, 0.14, 0.45)
    lid.position.set(s * 0.056, 0.035, 0.136)
    lid.visible = false
    headGroup.add(lid); eyelids.push(lid)
  }
  headGroup.userData.eyelids = eyelids

  const nose = new THREE.Mesh(new THREE.SphereGeometry(0.015, 8, 8), noseMat)
  nose.scale.set(1, 0.75, 0.8)
  nose.position.set(0, -0.012, 0.158); headGroup.add(nose)

  const mc = new THREE.CatmullRomCurve3([
    new THREE.Vector3(-0.018, -0.042, 0.158),
    new THREE.Vector3(0, -0.032, 0.166),
    new THREE.Vector3(0.018, -0.042, 0.158)
  ])
  headGroup.add(new THREE.Mesh(
    new THREE.TubeGeometry(mc, 8, 0.003, 4),
    new THREE.MeshStandardMaterial({ color: 0x5C4D3C, roughness: 0.5 })
  ))

  for (const s of [-1, 1]) {
    for (let i = -1; i <= 1; i++) {
      const w = new THREE.Mesh(
        new THREE.CylinderGeometry(0.001, 0.001, 0.12, 4),
        new THREE.MeshStandardMaterial({ color: 0x9B8C7C, roughness: 0.3 })
      )
      w.rotation.z = Math.PI / 2
      w.position.set(s * 0.082, -0.018 + i * 0.014, 0.128)
      w.rotation.y = s * 0.28
      headGroup.add(w)
    }
  }
  headGroup.position.set(0, 0.385, 0.12); g.add(headGroup)

  for (const [x, z] of [[-0.1, 0.1], [0.1, 0.1], [-0.1, -0.1], [0.1, -0.1]]) {
    const leg = new THREE.Mesh(new THREE.CylinderGeometry(0.022, 0.03, 0.15, 12), mat)
    leg.position.set(x * 0.9, 0.068, z * 1.15); leg.castShadow = true
    g.add(leg)
    const paw = new THREE.Mesh(new THREE.SphereGeometry(0.03, 10, 6), dark)
    paw.scale.set(1.18, 0.42, 1.35)
    paw.position.set(x * 0.9, 0.004, z * 1.15 + 0.024)
    paw.userData.basePos = paw.position.clone()
    g.add(paw)
    if (z > 0) frontPaws.push(paw)
  }

  const tc = new THREE.CatmullRomCurve3([
    new THREE.Vector3(0, 0.18, -0.28),
    new THREE.Vector3(0.02, 0.34, -0.45),
    new THREE.Vector3(0.08, 0.48, -0.38),
    new THREE.Vector3(0.11, 0.55, -0.26)
  ])
  tail = new THREE.Mesh(new THREE.TubeGeometry(tc, 18, 0.018, 8), mat)
  tail.castShadow = true; g.add(tail)

  const collar = new THREE.Mesh(
    new THREE.TorusGeometry(0.14, 0.012, 8, 24),
    new THREE.MeshStandardMaterial({ color: C.spot, roughness: 0.3, metalness: 0.3 })
  )
  collar.position.set(0, 0.38, 0.05); collar.rotation.x = Math.PI * 0.42; g.add(collar)
  const bell = new THREE.Mesh(
    new THREE.SphereGeometry(0.018, 8, 8),
    new THREE.MeshStandardMaterial({ color: C.gold, roughness: 0.2, metalness: 0.6 })
  )
  bell.position.set(0, 0.34, 0.14); g.add(bell)

  g.scale.setScalar(0.95); g.position.set(0, 0.08, 0); return g
}

// ════════════════════════════════════════
//  DOG MODEL
// ════════════════════════════════════════
function createDogModel() {
  const g = new THREE.Group()
  const mat = new THREE.MeshStandardMaterial({ color: pc(), roughness: 0.6 })
  const dark = new THREE.MeshStandardMaterial({ color: dc(), roughness: 0.5 })
  const white = new THREE.MeshStandardMaterial({ color: 0xFAF5EE, roughness: 0.5 })
  const eyeMat = new THREE.MeshStandardMaterial({ color: 0x222222, roughness: 0.3 })

  bodyMesh = new THREE.Mesh(new THREE.SphereGeometry(0.24, 20, 14), mat)
  bodyMesh.scale.set(1, 0.8, 1.3); bodyMesh.position.y = 0.22; bodyMesh.castShadow = true
  g.add(bodyMesh)

  headGroup = new THREE.Group()
  const head = new THREE.Mesh(new THREE.SphereGeometry(0.18, 20, 14), mat)
  head.scale.set(1, 0.95, 1.05); head.castShadow = true; headGroup.add(head)

  for (const s of [-1, 1]) {
    const ear = new THREE.Mesh(new THREE.SphereGeometry(0.06, 10, 8), mat)
    ear.scale.set(0.6, 1.2, 0.3)
    ear.position.set(s * 0.14, 0.04, -0.02); ear.rotation.z = s * 0.4; ear.castShadow = true
    headGroup.add(ear)
  }
  for (const s of [-1, 1]) {
    const ew = new THREE.Mesh(new THREE.SphereGeometry(0.035, 10, 10), white)
    ew.position.set(s * 0.07, 0.04, 0.14); headGroup.add(ew)
    const ep = new THREE.Mesh(new THREE.SphereGeometry(0.02, 10, 10), eyeMat)
    ep.position.set(s * 0.07, 0.04, 0.17); headGroup.add(ep)
    const es = new THREE.Mesh(new THREE.SphereGeometry(0.007, 6, 6),
      new THREE.MeshStandardMaterial({ color: 0xFFFFFF, roughness: 0.1 }))
    es.position.set(s * 0.065, 0.05, 0.18); headGroup.add(es)
  }
  const eyelids = []
  for (const s of [-1, 1]) {
    const lid = new THREE.Mesh(new THREE.SphereGeometry(0.037, 10, 10), mat)
    lid.position.set(s * 0.07, 0.04, 0.14); lid.scale.y = 0.01; lid.visible = false
    headGroup.add(lid); eyelids.push(lid)
  }
  headGroup.userData.eyelids = eyelids

  const nose = new THREE.Mesh(new THREE.SphereGeometry(0.022, 10, 8),
    new THREE.MeshStandardMaterial({ color: 0x222222, roughness: 0.4 }))
  nose.scale.set(1, 0.8, 1); nose.position.set(0, 0, 0.18); headGroup.add(nose)

  const mc = new THREE.CatmullRomCurve3([
    new THREE.Vector3(-0.025, -0.04, 0.165),
    new THREE.Vector3(0, -0.03, 0.175),
    new THREE.Vector3(0.025, -0.04, 0.165)
  ])
  headGroup.add(new THREE.Mesh(
    new THREE.TubeGeometry(mc, 8, 0.003, 4),
    new THREE.MeshStandardMaterial({ color: 0x5C4D3C, roughness: 0.5 })
  ))

  const tongue = new THREE.Mesh(new THREE.SphereGeometry(0.015, 8, 6),
    new THREE.MeshStandardMaterial({ color: 0xFF8888, roughness: 0.4 }))
  tongue.scale.set(1, 1.5, 0.5); tongue.position.set(0, -0.045, 0.17)
  tongue.visible = false; headGroup.add(tongue)
  headGroup.userData.tongue = tongue

  headGroup.position.set(0, 0.46, 0.12); g.add(headGroup)

  for (const [x, z] of [[-0.12, 0.12], [0.12, 0.12], [-0.12, -0.12], [0.12, -0.12]]) {
    const leg = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.04, 0.14, 10), mat)
    leg.position.set(x, 0.07, z); leg.castShadow = true; g.add(leg)
    const paw = new THREE.Mesh(new THREE.SphereGeometry(0.04, 8, 6), dark)
    paw.scale.set(1, 0.5, 1.2); paw.position.set(x, 0.01, z + 0.01); g.add(paw)
  }

  const tc = new THREE.CatmullRomCurve3([
    new THREE.Vector3(0, 0.2, -0.22),
    new THREE.Vector3(0, 0.35, -0.3),
    new THREE.Vector3(0.02, 0.48, -0.22)
  ])
  tail = new THREE.Mesh(new THREE.TubeGeometry(tc, 12, 0.025, 8), mat)
  tail.castShadow = true; g.add(tail)

  const collar = new THREE.Mesh(
    new THREE.TorusGeometry(0.15, 0.014, 8, 24),
    new THREE.MeshStandardMaterial({ color: C.accent, roughness: 0.3, metalness: 0.2 })
  )
  collar.position.set(0, 0.38, 0.06); collar.rotation.x = Math.PI * 0.42; g.add(collar)
  const tag = new THREE.Mesh(
    new THREE.CircleGeometry(0.025, 16),
    new THREE.MeshStandardMaterial({ color: C.gold, roughness: 0.2, metalness: 0.5 })
  )
  tag.position.set(0, 0.33, 0.16); g.add(tag)

  g.scale.setScalar(0.9); g.position.set(0, 0.08, 0); return g
}

// ════════════════════════════════════════
//  GENERIC MODEL
// ════════════════════════════════════════
function createGenericModel() {
  const g = new THREE.Group()
  const mat = new THREE.MeshStandardMaterial({ color: pc(), roughness: 0.6 })
  const white = new THREE.MeshStandardMaterial({ color: 0xFAF5EE, roughness: 0.5 })
  const eyeMat = new THREE.MeshStandardMaterial({ color: 0x222222, roughness: 0.3 })

  bodyMesh = new THREE.Mesh(new THREE.SphereGeometry(0.22, 18, 12), mat)
  bodyMesh.scale.set(1, 0.85, 1.1); bodyMesh.position.y = 0.22; bodyMesh.castShadow = true
  g.add(bodyMesh)

  headGroup = new THREE.Group()
  const head = new THREE.Mesh(new THREE.SphereGeometry(0.16, 18, 12), mat)
  head.castShadow = true; headGroup.add(head)
  for (const s of [-1, 1]) {
    const ear = new THREE.Mesh(new THREE.SphereGeometry(0.04, 8, 6), mat)
    ear.position.set(s * 0.1, 0.14, 0.02); headGroup.add(ear)
    const ew = new THREE.Mesh(new THREE.SphereGeometry(0.03, 8, 8), white)
    ew.position.set(s * 0.06, 0.03, 0.13); headGroup.add(ew)
    const ep = new THREE.Mesh(new THREE.SphereGeometry(0.016, 8, 8), eyeMat)
    ep.position.set(s * 0.06, 0.03, 0.155); headGroup.add(ep)
  }
  const eyelids = []
  for (const s of [-1, 1]) {
    const lid = new THREE.Mesh(new THREE.SphereGeometry(0.032, 8, 8), mat)
    lid.position.set(s * 0.06, 0.03, 0.13); lid.scale.y = 0.01; lid.visible = false
    headGroup.add(lid); eyelids.push(lid)
  }
  headGroup.userData.eyelids = eyelids
  const nose = new THREE.Mesh(new THREE.SphereGeometry(0.013, 8, 8),
    new THREE.MeshStandardMaterial({ color: 0xFF9999, roughness: 0.4 }))
  nose.position.set(0, -0.01, 0.155); headGroup.add(nose)
  headGroup.position.set(0, 0.44, 0.08); g.add(headGroup)

  for (const [x, z] of [[-0.1, 0.1], [0.1, 0.1], [-0.1, -0.1], [0.1, -0.1]]) {
    const leg = new THREE.Mesh(new THREE.CylinderGeometry(0.028, 0.032, 0.12, 10), mat)
    leg.position.set(x, 0.06, z); g.add(leg)
  }
  const tc = new THREE.CatmullRomCurve3([
    new THREE.Vector3(0, 0.18, -0.2),
    new THREE.Vector3(0, 0.32, -0.32),
    new THREE.Vector3(0.04, 0.42, -0.25)
  ])
  tail = new THREE.Mesh(new THREE.TubeGeometry(tc, 12, 0.02, 8), mat); g.add(tail)

  g.scale.setScalar(0.9); g.position.set(0, 0.08, 0); return g
}

function createPetModel() {
  const sp = props.species || '猫'
  if (sp.includes('狗') || sp.includes('dog')) return createDogModel()
  if (sp.includes('猫') || sp.includes('cat')) return createCatModel()
  return createGenericModel()
}

// ════════════════════════════════════════
//  PARTICLES
// ════════════════════════════════════════
function spawnHearts(origin) {
  if (!particleGroup) return
  for (let i = 0; i < 8; i++) {
    const h = new THREE.Mesh(
      new THREE.SphereGeometry(0.012, 6, 4),
      new THREE.MeshBasicMaterial({ color: new THREE.Color().setHSL(0.95, 0.8, 0.55), transparent: true })
    )
    h.position.copy(origin).add(new THREE.Vector3(
      (Math.random() - 0.5) * 0.18, 0.25 + Math.random() * 0.12, (Math.random() - 0.5) * 0.18
    ))
    h.userData = {
      v: new THREE.Vector3((Math.random() - 0.5) * 0.002, 0.004 + Math.random() * 0.004, (Math.random() - 0.5) * 0.002),
      life: 1, isParticle: true
    }
    particleGroup.add(h)
  }
}

function spawnPaws(origin) {
  if (!particleGroup) return
  for (let i = 0; i < 6; i++) {
    const d = new THREE.Mesh(
      new THREE.SphereGeometry(0.01, 5, 3),
      new THREE.MeshBasicMaterial({ color: new THREE.Color(C.accent), transparent: true })
    )
    d.position.copy(origin).add(new THREE.Vector3(
      (Math.random() - 0.5) * 0.2, 0.2 + Math.random() * 0.15, (Math.random() - 0.5) * 0.2
    ))
    d.userData = {
      v: new THREE.Vector3((Math.random() - 0.5) * 0.015, 0.015 + Math.random() * 0.02, (Math.random() - 0.5) * 0.015),
      life: 1, isParticle: true
    }
    particleGroup.add(d)
  }
}

// ════════════════════════════════════════
//  STATE
// ════════════════════════════════════════
function setPetState(state) {
  if (!clock || action) return false
  cur = state
  t0 = clock.getElapsedTime()
  action = {
    type: state,
    start: t0,
    duration: state === 'eat' ? 4.2 : state === 'pet' ? 3.5 : 4.0,
    spawned: false
  }
  if (state === 'eat') {
    kibbles.forEach((k, i) => { k.visible = i < Math.min(3 + feedingCount.value, 10) })
    hunger.value = Math.min(100, hunger.value + 15)
    mood.value = Math.min(100, mood.value + 8)
    energy.value = Math.min(100, energy.value + 3)
  } else if (state === 'play') {
    mood.value = Math.min(100, mood.value + 10)
    energy.value = Math.max(0, energy.value - 5)
    hunger.value = Math.max(0, hunger.value - 3)
  } else if (state === 'pet') {
    mood.value = Math.min(100, mood.value + 5)
  }
  return true
}

// ════════════════════════════════════════
//  INIT SCENE
// ════════════════════════════════════════
function initScene() {
  const el = container.value
  if (!el) { statusMsg.value = '3D容器未找到'; return false }

  const w = el.clientWidth || 800
  const h = el.clientHeight || 540

  // Renderer
  try {
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  } catch (e) {
    statusMsg.value = 'WebGL不可用: ' + e.message; return false
  }
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.1
  el.innerHTML = ''
  renderer.domElement.style.display = 'block'
  renderer.domElement.style.width = '100%'
  renderer.domElement.style.height = '100%'
  el.appendChild(renderer.domElement)

  // Scene
  scene = new THREE.Scene()
  scene.background = new THREE.Color(C.paper)
  scene.fog = new THREE.Fog(C.paper, 5, 14)

  // Camera
  camera = new THREE.PerspectiveCamera(36, w / h, 0.1, 20)
  camera.position.set(0.8, 1.0, 3.0)
  camera.lookAt(0, 0.2, 0)

  // Lights
  scene.add(new THREE.AmbientLight(0xFFF8EE, 1.6))
  const keyLight = new THREE.DirectionalLight(0xFFF4E8, 3.5)
  keyLight.position.set(5, 7, 4)
  keyLight.castShadow = true
  keyLight.shadow.mapSize.set(1024, 1024)
  keyLight.shadow.camera.near = 0.1
  keyLight.shadow.camera.far = 20
  keyLight.shadow.bias = -0.001
  keyLight.shadow.normalBias = 0.02
  scene.add(keyLight)
  const fillLight = new THREE.DirectionalLight(0xD8E0FF, 0.8)
  fillLight.position.set(-3, 2, -2)
  scene.add(fillLight)

  // Scene objects
  scene.add(createStage())
  scene.add(createDecorations())
  foodBowl = createFoodBowl()
  scene.add(foodBowl)
  toyRoot = createToy()
  scene.add(toyRoot)

  petGroup = createPetModel()
  scene.add(petGroup)

  laserDot = createLaserDot()
  scene.add(laserDot)

  particleGroup = new THREE.Group()
  scene.add(particleGroup)

  // Floor
  const floor = new THREE.Mesh(
    new THREE.CircleGeometry(3, 64),
    new THREE.MeshStandardMaterial({ color: C.paperDark, roughness: 0.8 })
  )
  floor.rotation.x = -Math.PI / 2; floor.position.y = -0.9; floor.receiveShadow = true
  scene.add(floor)

  // Controls
  controls = new OrbitControls(camera, renderer.domElement)
  controls.target.set(0, 0.2, 0)
  controls.enableDamping = true
  controls.dampingFactor = 0.1
  controls.minDistance = 1.2
  controls.maxDistance = 4.5
  controls.maxPolarAngle = Math.PI * 0.6
  controls.update()

  // Click interaction
  const rc = new THREE.Raycaster()
  const floorPlane = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0)
  renderer.domElement.addEventListener('pointerdown', e => {
    if (!petGroup) return
    const r = renderer.domElement.getBoundingClientRect()
    const mouse = new THREE.Vector2(
      ((e.clientX - r.left) / r.width) * 2 - 1,
      -((e.clientY - r.top) / r.height) * 2 + 1
    )
    rc.setFromCamera(mouse, camera)

    // Laser mode: move laser dot to click point on floor
    if (laserActive) {
      const intersection = new THREE.Vector3()
      if (rc.ray.intersectPlane(floorPlane, intersection)) {
        const clampedX = Math.max(-0.8, Math.min(0.8, intersection.x))
        const clampedZ = Math.max(-0.8, Math.min(0.8, intersection.z))
        laserTarget.set(clampedX, 0.01, clampedZ)
        if (laserDot) laserDot.position.copy(laserTarget)
      }
      return
    }

    // Normal mode: pet interaction
    if (rc.intersectObject(petGroup, true).length > 0) {
      if (setPetState('pet')) {
        spawnHearts(petGroup.position)
      }
      emit('interaction', 'pet')
    }
  })

  return true
}

// ════════════════════════════════════════
//  ANIMATE
// ════════════════════════════════════════
function animate() {
  if (disposed) return
  animFrame = requestAnimationFrame(animate)

  if (!renderer || !scene || !camera) return

  const dt = clock ? Math.min(clock.getDelta(), 0.1) : 0.016
  const t = clock ? clock.getElapsedTime() : 0

  // Idle breathing
  if (petGroup && headGroup && bodyMesh && cur === 'idle') {
    const br = Math.sin(t * 1.8) * 0.012
    const base = bodyMesh.userData.baseScale || { x: 1, y: 0.82, z: 1.15 }
    bodyMesh.scale.y = base.y + br
    bodyMesh.scale.x = base.x - br * 0.3
    bodyMesh.scale.z = base.z + br * 0.12
    headGroup.rotation.y = Math.sin(t * 0.7) * 0.06
    headGroup.rotation.z = Math.sin(t * 0.5) * 0.03
    if (tail) {
      tail.rotation.y = Math.sin(t * 1.2) * 0.15
      tail.rotation.x = Math.sin(t * 0.8) * 0.05
    }
    // Blink
    blinkTimer -= dt
    const isBlinking = blinkTimer <= 0
    if (blinkTimer <= -0.14) blinkTimer = 2.2 + Math.random() * 3.5
    ;(headGroup.userData.eyelids || []).forEach(l => { l.visible = isBlinking })
    // Dog tongue
    if (headGroup.userData.tongue) headGroup.userData.tongue.visible = mood.value > 70
    // Return to base
    petGroup.position.x += (0 - petGroup.position.x) * 0.06
    petGroup.position.y += (0.08 - petGroup.position.y) * 0.06
    petGroup.rotation.x *= 0.95; petGroup.rotation.y *= 0.92; petGroup.rotation.z *= 0.95
    petGroup.position.z *= 0.95
    bodyMesh.rotation.x *= 0.9
    headGroup.rotation.x *= 0.95
    frontPaws.forEach(p => {
      if (p.userData.basePos) p.position.lerp(p.userData.basePos, 0.08)
      p.rotation.x *= 0.9
    })
    toyBalls.forEach(b => { b.position.lerp(b.userData.origPos, 0.04) })
    kibbles.forEach(k => { if (k.visible) k.position.y += (0.04 - k.position.y) * 0.05 })

    // Laser chase behavior
    if (laserActive && !action) {
      const toLaser = new THREE.Vector3().subVectors(laserTarget, petGroup.position)
      toLaser.y = 0
      const dist = toLaser.length()
      if (dist > 0.08) {
        toLaser.normalize()
        const speed = 0.012
        petGroup.position.x += toLaser.x * speed
        petGroup.position.z += toLaser.z * speed
        // Face laser direction
        const targetAngle = Math.atan2(toLaser.x, toLaser.z)
        let diff = targetAngle - petGroup.rotation.y
        while (diff > Math.PI) diff -= Math.PI * 2
        while (diff < -Math.PI) diff += Math.PI * 2
        petGroup.rotation.y += diff * 0.08
        // Head tracks laser
        headGroup.rotation.y = diff * 0.3
        headGroup.rotation.x = -0.08
        // Tail up when chasing
        if (tail) tail.rotation.x = -0.2
      } else {
        checkLaserCatch()
      }
    }
  }

  if (action && petGroup && headGroup && bodyMesh) {
    const e = t - action.start
    const p = clamp01(e / action.duration)
    const base = bodyMesh.userData.baseScale || { x: 1, y: 0.74, z: 1.08 }

    if (action.type === 'eat') {
      const turnIn = smooth(p / 0.2)
      const eating = smooth((p - 0.2) / 0.45)
      const pleased = smooth((p - 0.62) / 0.18)
      const back = smooth((p - 0.82) / 0.18)
      const chew = Math.sin(e * 11)

      const px = mix(mix(0, 0.18, turnIn), 0, back)
      const pz = mix(mix(0, 0.1, turnIn), 0, back)
      const ry = mix(mix(0, 0.48, turnIn), 0, back)
      const bow = eating * (1 - pleased) * (1 - back)

      petGroup.position.set(px, BASE_Y + Math.abs(Math.sin(e * 2.6)) * 0.008 * (1 - back), pz)
      petGroup.rotation.set(-0.05 * bow, ry, 0.025 * chew * bow)
      bodyMesh.scale.set(base.x + 0.02 * bow, base.y - 0.04 * bow, base.z + 0.03 * bow)
      bodyMesh.rotation.x = -0.08 * bow

      headGroup.rotation.y = mix(0.08, 0.5, turnIn) * (1 - back)
      headGroup.rotation.x = (-0.5 * bow + 0.05 * chew * bow + 0.12 * pleased) * (1 - back)
      headGroup.rotation.z = 0.025 * chew * bow
      if (tail) tail.rotation.y = Math.sin(e * 5.2) * (0.18 + 0.28 * pleased)

      frontPaws.forEach((paw, i) => {
        const bp = paw.userData.basePos
        if (!bp) return
        paw.position.x = bp.x + (i === 0 ? -0.01 : 0.01) * bow
        paw.position.y = bp.y + 0.006 * bow
        paw.position.z = bp.z + 0.055 * bow + Math.sin(e * 6 + i) * 0.006 * bow
        paw.rotation.x = -0.35 * bow
      })

      if (foodBowl) {
        const pulse = 1 + Math.sin(e * 9) * 0.025 * bow
        foodBowl.scale.set(pulse, 1, pulse)
      }
      kibbles.forEach((k, i) => {
        if (k.visible) {
          k.position.y = 0.04 + Math.abs(Math.sin(e * 7 + i)) * 0.018 * bow
          k.scale.setScalar(1 - pleased * 0.35)
        }
      })
      if (p > 0.64 && !action.spawned) {
        spawnHearts(new THREE.Vector3(0.25, 0.5, 0.2))
        action.spawned = true
      }
    }

    if (action.type === 'play') {
      const focus = smooth(p / 0.18)
      const crouch = smooth((p - 0.16) / 0.18)
      const pounce = smooth((p - 0.34) / 0.22)
      const celebrate = smooth((p - 0.58) / 0.2)
      const back = smooth((p - 0.82) / 0.18)
      const wiggle = Math.sin(e * 10)
      const leap = Math.sin(clamp01((p - 0.34) / 0.26) * Math.PI)

      const px = mix(mix(0, -0.18, pounce), 0, back)
      const py = BASE_Y + leap * 0.13 * (1 - back)
      const pz = mix(mix(0, -0.05, pounce), 0, back)
      const ry = mix(mix(0, -0.55, focus), 0, back)

      petGroup.position.set(px, py, pz)
      petGroup.rotation.set(-0.08 * crouch + 0.08 * leap, ry, wiggle * 0.055 * (pounce + celebrate) * (1 - back))
      bodyMesh.scale.set(base.x + 0.02 * crouch, base.y - 0.07 * crouch + 0.025 * leap, base.z + 0.04 * crouch)
      headGroup.rotation.y = mix(0, -0.45, focus) * (1 - back)
      headGroup.rotation.x = (-0.12 * crouch + 0.08 * leap + wiggle * 0.02 * celebrate) * (1 - back)
      headGroup.rotation.z = wiggle * 0.035 * celebrate
      if (tail) tail.rotation.y = Math.sin(e * 7) * (0.22 + 0.36 * (pounce + celebrate))

      frontPaws.forEach((paw, i) => {
        const bp = paw.userData.basePos
        if (!bp) return
        const side = i === 0 ? -1 : 1
        paw.position.x = bp.x + side * 0.012 * pounce
        paw.position.y = bp.y + Math.abs(Math.sin(e * 8 + i)) * 0.045 * (pounce + celebrate)
        paw.position.z = bp.z + 0.055 * pounce + Math.sin(e * 6 + i) * 0.018 * celebrate
        paw.rotation.x = Math.sin(e * 7 + i) * 0.45 * (pounce + celebrate)
      })

      toyBalls.forEach((b, i) => {
        const bp = b.userData.origPos
        const roll = smooth((p - 0.18) / 0.62)
        const ph = e * 6 + i * 1.6
        b.position.x = bp.x + roll * (0.28 + i * 0.05) + Math.sin(ph) * 0.035 * (1 - back)
        b.position.y = bp.y + Math.abs(Math.sin(ph * 1.2)) * 0.1 * (pounce + celebrate) * (1 - back)
        b.position.z = bp.z + Math.cos(ph) * 0.035 * roll * (1 - back)
        b.rotation.y += 0.18 + roll * 0.14
        b.rotation.z += 0.12 + roll * 0.1
      })

      if (p > 0.5 && !action.spawned) {
        spawnPaws(new THREE.Vector3(-0.18, 0.35, -0.12))
        action.spawned = true
      }
    }

    if (action.type === 'pet') {
      const leanIn = smooth(p / 0.2)
      const enjoy = smooth((p - 0.15) / 0.3)
      const purr = smooth((p - 0.2) / 0.25)
      const back = smooth((p - 0.75) / 0.25)
      const t2 = e * 2.5

      // Head: tilt and lean toward touch, eyes close in pleasure
      headGroup.rotation.z = Math.sin(t2 * 0.8) * 0.12 * enjoy * (1 - back)
      headGroup.rotation.x = (-0.15 * leanIn + 0.08 * enjoy) * (1 - back)
      headGroup.rotation.y = Math.sin(t2 * 0.5) * 0.1 * enjoy * (1 - back)
      // Eyes closed during petting
      ;(headGroup.userData.eyelids || []).forEach(l => { l.visible = enjoy > 0.3 && p < 0.85 })

      // Body: gentle purr vibration
      const purrAmt = 0.015 * purr * (1 - back)
      bodyMesh.scale.y = (base.y) + Math.sin(t2 * 6) * purrAmt
      bodyMesh.scale.x = (base.x) - Math.sin(t2 * 6) * purrAmt * 0.4
      bodyMesh.scale.z = (base.z) + Math.sin(t2 * 6) * purrAmt * 0.2

      // Slight lean toward the touch
      petGroup.position.x += (0.04 - petGroup.position.x) * 0.04 * (1 - back)
      petGroup.rotation.z = Math.sin(t2 * 1.2) * 0.02 * enjoy * (1 - back)

      // Tail: slow happy sway
      if (tail) {
        tail.rotation.y = Math.sin(t2 * 1.5) * (0.25 + 0.15 * enjoy) * (1 - back)
        tail.rotation.x = Math.sin(t2 * 0.8) * 0.08 * enjoy * (1 - back)
      }

      // Paws: slight kneading
      frontPaws.forEach((paw, i) => {
        const bp = paw.userData.basePos
        if (!bp) return
        const side = i === 0 ? -1 : 1
        paw.position.y = bp.y + Math.abs(Math.sin(t2 * 2 + i * 1.5)) * 0.008 * purr * (1 - back)
        paw.rotation.x = Math.sin(t2 * 2 + i) * 0.08 * purr * (1 - back)
      })

      // Spawn hearts midway
      if (p > 0.4 && !action.spawned) {
        spawnHearts(petGroup.position)
        action.spawned = true
      }
    }

    if (p >= 1) {
      action = null
      cur = 'idle'
      if (foodBowl) foodBowl.scale.set(1, 1, 1)
      kibbles.forEach(k => {
        k.visible = false
        k.scale.setScalar(1)
      })
    }
  }

  // Particles
  if (particleGroup) {
    for (let i = particleGroup.children.length - 1; i >= 0; i--) {
      const p = particleGroup.children[i]
      if (!p.userData.isParticle) continue
      p.userData.life -= 0.0025
      if (p.userData.life <= 0) { particleGroup.remove(p); continue }
      p.position.add(p.userData.v); p.userData.v.y += 0.00003
      p.material.opacity = p.userData.life
      p.scale.setScalar(p.userData.life * 0.5 + 0.5)
    }
  }

  if (controls) controls.update()
  renderer.render(scene, camera)
}

// ════════════════════════════════════════
//  PUBLIC API
// ════════════════════════════════════════
defineExpose({
  feed() {
    feedingCount.value++
    if (!setPetState('eat')) feedingCount.value--
  },
  play() {
    playingCount.value++
    if (!setPetState('play')) playingCount.value--
  },
  startLaser() {
    startLaser()
  },
  stopLaser() {
    stopLaser()
  },
  isLaserActive() {
    return laserActive
  }
})

// ════════════════════════════════════════
//  LIFECYCLE
// ════════════════════════════════════════
onMounted(() => {
  clock = new THREE.Clock()
  // Wait for DOM layout, then init
  const tryInit = () => {
    if (disposed) return
    const el = container.value
    if (!el) { statusMsg.value = '3D容器未找到'; return }
    if (el.clientWidth < 10 || el.clientHeight < 10) {
      setTimeout(tryInit, 100)
      return
    }
    if (initScene()) {
      animate()
    }
  }
  nextTick(() => setTimeout(tryInit, 50))

  // ResizeObserver for robust container size tracking
  resizeObserver = new ResizeObserver(() => {
    if (!container.value || !camera || !renderer) return
    const w = container.value.clientWidth
    const h = container.value.clientHeight
    if (w < 10 || h < 10) return
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h)
  })
  if (container.value) {
    resizeObserver.observe(container.value)
  }
})

onUnmounted(() => {
  disposed = true
  cancelAnimationFrame(animFrame)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (controls) { controls.dispose(); controls = null }
  if (renderer) {
    renderer.dispose()
    if (renderer.domElement && renderer.domElement.parentNode) {
      renderer.domElement.parentNode.removeChild(renderer.domElement)
    }
    renderer = null
  }
  scene = null; camera = null; petGroup = null; headGroup = null
  bodyMesh = null; tail = null; particleGroup = null
})
</script>

<template>
  <div class="pet3d-wrap">
    <div ref="container" class="pet3d" />
    <div class="status-overlay" v-if="statusMsg">
      <p>{{ statusMsg }}</p>
    </div>
    <div class="status-bar">
      <div class="status-item">
        <span class="status-label">心情</span>
        <div class="status-bar-track"><div class="status-bar-fill mood" :style="{ width: mood + '%' }"></div></div>
      </div>
      <div class="status-item">
        <span class="status-label">饱腹</span>
        <div class="status-bar-track"><div class="status-bar-fill hunger" :style="{ width: hunger + '%' }"></div></div>
      </div>
      <div class="status-item">
        <span class="status-label">活力</span>
        <div class="status-bar-track"><div class="status-bar-fill energy" :style="{ width: energy + '%' }"></div></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pet3d-wrap { position: relative; width: 100%; height: 100%; }
.pet3d {
  width: 100%; height: 100%; border-radius: 18px; overflow: hidden;
  cursor: grab; border: 1px solid #E0D5C5;
  background: linear-gradient(180deg, #F5EDE0 0%, #E8D8C0 100%);
  display: block;
}
.pet3d:active { cursor: grabbing; }
.pet3d canvas { display: block; width: 100% !important; height: 100% !important; }
.status-overlay {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(245, 237, 224, 0.9); border-radius: 18px;
  color: #8B7355; font-size: 0.9rem; z-index: 5;
}
.status-bar {
  position: absolute; bottom: 12px; left: 12px; right: 12px;
  display: flex; gap: 12px; padding: 8px 12px;
  background: rgba(250, 247, 240, 0.85); backdrop-filter: blur(6px);
  border: 1px solid #E0D5C5; border-radius: 10px; z-index: 10;
}
.status-item { flex: 1; display: flex; align-items: center; gap: 6px; }
.status-label {
  font-family: var(--font-display); font-size: 0.7rem;
  color: #8B7355; letter-spacing: 0.04em; min-width: 28px;
}
.status-bar-track {
  flex: 1; height: 6px; background: #E8E0D0; border-radius: 3px; overflow: hidden;
}
.status-bar-fill { height: 100%; border-radius: 3px; transition: width 0.5s ease; }
.status-bar-fill.mood { background: #C17B60; }
.status-bar-fill.hunger { background: #C4A87C; }
.status-bar-fill.energy { background: #5C7A5E; }
</style>
